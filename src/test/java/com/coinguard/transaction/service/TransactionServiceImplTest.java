package com.coinguard.transaction.service;

import com.coinguard.budget.service.BudgetService;
import com.coinguard.common.enums.Currency;
import com.coinguard.common.enums.TransactionCategory;
import com.coinguard.common.exception.InsufficientBalanceException;
import com.coinguard.common.exception.SelfTransferException;
import com.coinguard.messaging.producer.NotificationMessageProducer;
import com.coinguard.transaction.dto.request.TransactionFilterRequest;
import com.coinguard.transaction.dto.request.TransferRequest;
import com.coinguard.transaction.dto.response.ReceiptResponse;
import com.coinguard.transaction.dto.response.TransactionResponse;
import com.coinguard.transaction.dto.response.TransactionStatsResponse;
import com.coinguard.transaction.entity.Transaction;
import com.coinguard.transaction.enums.TransactionStatus;
import com.coinguard.transaction.enums.TransactionType;
import com.coinguard.transaction.mapper.TransactionMapper;
import com.coinguard.transaction.repository.TransactionRepository;
import com.coinguard.user.entity.User;
import com.coinguard.wallet.entity.Wallet;
import com.coinguard.wallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private BudgetService budgetService;
    @Mock
    private NotificationMessageProducer notificationProducer;

    private Wallet createWallet(Long userId, BigDecimal balance) {
        User user = User.builder()
                .id(userId)
                .username("user" + userId)
                .fullName("Test User " + userId)
                .email("user" + userId + "@example.com")
                .build();

        return Wallet.builder()
                .id(userId)
                .user(user)
                .balance(balance)
                .currency(Currency.TRY)
                .isFrozen(false)
                .build();
    }

    @Test
    @DisplayName("Should transfer money successfully when balance is sufficient")
    void shouldTransferMoneySuccessfully() {
        // GIVEN
        Long senderId = 1L;
        Long receiverId = 2L;
        BigDecimal transferAmount = new BigDecimal("100.00");

        TransferRequest request = new TransferRequest(
                receiverId,
                transferAmount,
                "Shopping Payment",
                TransactionCategory.SHOPPING
        );

        Wallet senderWallet = createWallet(senderId, new BigDecimal("500.00"));
        Wallet receiverWallet = createWallet(receiverId, new BigDecimal("200.00"));

        when(walletRepository.findByUserIdForUpdate(senderId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserIdForUpdate(receiverId)).thenReturn(Optional.of(receiverWallet));

        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(notificationProducer).sendNotificationMessage(any());

        when(transactionMapper.toTransactionResponse(any(Transaction.class)))
                .thenReturn(new TransactionResponse(
                        100L,
                        "Test User 1",
                        "Test User 2",
                        transferAmount,
                        Currency.TRY,
                        TransactionType.TRANSFER,
                        TransactionStatus.COMPLETED,
                        TransactionCategory.SHOPPING,
                        "REF-123",
                        "Shopping Payment",
                        LocalDateTime.now()
                ));

        // WHEN
        TransactionResponse response = transactionService.transfer(senderId, request);

        // THEN
        assertNotNull(response, "Response should not be null");

        assertEquals(new BigDecimal("400.00"), senderWallet.getBalance());
        assertEquals(new BigDecimal("300.00"), receiverWallet.getBalance());

        // Verify
        verify(walletRepository).findByUserIdForUpdate(senderId);
        verify(walletRepository).findByUserIdForUpdate(receiverId);
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(transactionRepository).save(any(Transaction.class));
        verify(notificationProducer, times(2)).sendNotificationMessage(any()); // sender + receiver
        verify(budgetService).trackExpense(eq(senderId), eq(transferAmount), eq(TransactionCategory.SHOPPING), any());
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when balance is low")
    void shouldThrowException_WhenBalanceInsufficient() {
        // GIVEN
        Long senderId = 1L;
        Long receiverId = 2L;
        TransferRequest request = new TransferRequest(receiverId, new BigDecimal("1000.00"), "Shop", TransactionCategory.SHOPPING);

        Wallet senderWallet = createWallet(senderId, new BigDecimal("50.00"));
        Wallet receiverWallet = createWallet(receiverId, BigDecimal.ZERO);

        when(walletRepository.findByUserIdForUpdate(senderId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserIdForUpdate(receiverId)).thenReturn(Optional.of(receiverWallet));

        // WHEN & THEN
        assertThrows(InsufficientBalanceException.class, () -> transactionService.transfer(senderId, request));

        // Verify
        assertEquals(new BigDecimal("50.00"), senderWallet.getBalance());
        verify(transactionRepository, never()).save(any());
        verify(budgetService, never()).trackExpense(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw SelfTransferException when sending to self")
    void shouldThrowException_WhenSelfTransfer() {
        // GIVEN
        Long userId = 1L;
        TransferRequest request = new TransferRequest(userId, new BigDecimal("100.00"), "Self", TransactionCategory.OTHER);

        // WHEN & THEN
        assertThrows(SelfTransferException.class, () -> transactionService.transfer(userId, request));

        verify(walletRepository, never()).findByUserIdForUpdate(any());
    }

    @Test
    @DisplayName("Should COMPLETE transfer even if BudgetService fails (Fail-Safe)")
    void shouldCompleteTransfer_EvenIfBudgetServiceFails() {
        // GIVEN
        Long senderId = 1L;
        Long receiverId = 2L;
        BigDecimal amount = new BigDecimal("100.00");
        TransferRequest request = new TransferRequest(receiverId, amount, "Fail-Safe Test", TransactionCategory.SHOPPING);

        Wallet senderWallet = createWallet(senderId, new BigDecimal("500.00"));
        Wallet receiverWallet = createWallet(receiverId, new BigDecimal("200.00"));

        when(walletRepository.findByUserIdForUpdate(senderId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserIdForUpdate(receiverId)).thenReturn(Optional.of(receiverWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(notificationProducer).sendNotificationMessage(any());

        when(transactionMapper.toTransactionResponse(any(Transaction.class)))
                .thenReturn(new TransactionResponse(
                        100L,
                        "Sender",
                        "Receiver",
                        amount,
                        Currency.TRY,
                        TransactionType.TRANSFER,
                        TransactionStatus.COMPLETED,
                        TransactionCategory.SHOPPING,
                        "REF-SAFE",
                        "Desc",
                        LocalDateTime.now()
                ));

        doThrow(new RuntimeException("Budget Service Unavailable"))
                .when(budgetService).trackExpense(any(), any(), any(), any());

        // WHEN
        TransactionResponse response = transactionService.transfer(senderId, request);

        // THEN
        assertNotNull(response);
        assertEquals(new BigDecimal("400.00"), senderWallet.getBalance());
        verify(transactionRepository).save(any(Transaction.class));
        verify(notificationProducer, times(2)).sendNotificationMessage(any());
    }

    @Test
    @DisplayName("Should calculate stats correctly for mixed transactions")
    void getTransactionStats_Success() {
        // GIVEN
        Long userId = 1L;
        User user = User.builder().id(userId).build();
        Wallet wallet = Wallet.builder().user(user).build();

        Transaction t1 = Transaction.builder()
                .amount(BigDecimal.valueOf(100))
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.TRANSFER)
                .category(TransactionCategory.GROCERY)
                .fromWallet(wallet)
                .toWallet(wallet)
                .build();

        Transaction t2 = Transaction.builder()
                .amount(BigDecimal.valueOf(200))
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.DEPOSIT)
                .category(TransactionCategory.SALARY)
                .fromWallet(wallet)
                .toWallet(wallet)
                .build();

        Transaction t3 = Transaction.builder()
                .amount(BigDecimal.valueOf(5000))
                .status(TransactionStatus.FAILED)
                .type(TransactionType.WITHDRAWAL)
                .fromWallet(wallet)
                .toWallet(wallet)
                .build();

        List<Transaction> transactions = List.of(t1, t2, t3);

        when(transactionRepository.findTransactionsForStats(eq(userId), any(), any()))
                .thenReturn(transactions);

        // WHEN
        TransactionStatsResponse response = transactionService.getTransactionStats(userId, "month", null, null);

        // THEN
        assertNotNull(response);

        assertEquals(3, response.totalTransactions());
        assertEquals(2, response.successfulTransactions());
        assertEquals(1, response.failedTransactions());


        assertEquals(BigDecimal.valueOf(300), response.totalAmount());

        assertEquals(0, BigDecimal.valueOf(150).compareTo(response.averageAmount()));

        assertTrue(response.successRate() > 66.0 && response.successRate() < 67.0);


        assertTrue(response.byType().containsKey(TransactionType.TRANSFER));
        assertEquals(1, response.byType().get(TransactionType.TRANSFER).count());
        assertEquals(BigDecimal.valueOf(100), response.byType().get(TransactionType.TRANSFER).total());

        assertTrue(response.byCategory().containsKey(TransactionCategory.GROCERY));
        assertEquals(1, response.byCategory().get(TransactionCategory.GROCERY).count());
    }

    @Test
    @DisplayName("Should return zero stats when no transactions found")
    void getTransactionStats_Empty() {
        // GIVEN
        Long userId = 1L;
        when(transactionRepository.findTransactionsForStats(eq(userId), any(), any()))
                .thenReturn(Collections.emptyList());

        // WHEN
        TransactionStatsResponse response = transactionService.getTransactionStats(userId, "month", null, null);

        // THEN
        assertEquals(0, response.totalTransactions());
        assertEquals(BigDecimal.ZERO, response.totalAmount());
        assertEquals(0.0, response.successRate());
        assertTrue(response.byType().isEmpty());
    }

    @Test
    @DisplayName("Should handle custom date range correctly")
    void getTransactionStats_CustomDate() {
        // GIVEN
        Long userId = 1L;
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 1, 31);

        // WHEN
        transactionService.getTransactionStats(userId, "custom", start, end);

        // THEN
        verify(transactionRepository).findTransactionsForStats(
                userId,
                LocalDateTime.of(2023, 1, 1, 0, 0),
                LocalDateTime.of(2023, 1, 31, 23, 59, 59)
        );
    }

    @Test
    @DisplayName("Should return receipt when user is the SENDER")
    void getTransactionReceipt_Success_Sender() {
        // GIVEN
        String refNo = "ref-123";
        Long userId = 1L;

        User user = User.builder().id(userId).build();
        Wallet wallet = Wallet.builder().user(user).build();

        Transaction transaction = Transaction.builder()
                .referenceNo(refNo)
                .fromWallet(wallet)
                .toWallet(Wallet.builder().user(User.builder().id(99L).build()).build())
                .build();

        ReceiptResponse expectedReceipt = new ReceiptResponse(
                refNo, 1L, "Me", "me", "Other", "other",
                BigDecimal.TEN, "TRY", BigDecimal.ZERO, BigDecimal.TEN,
                TransactionType.TRANSFER, TransactionCategory.OTHER, TransactionStatus.COMPLETED,
                "desc", LocalDateTime.now(), LocalDateTime.now()
        );

        when(transactionRepository.findByReferenceNo(refNo)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toReceiptResponse(transaction)).thenReturn(expectedReceipt);

        // WHEN
        ReceiptResponse result = transactionService.getTransactionReceipt(refNo, userId);

        // THEN
        assertNotNull(result);
        assertEquals(refNo, result.referenceNo());
        verify(transactionMapper).toReceiptResponse(transaction);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user is NOT owner")
    void getTransactionReceipt_AccessDenied() {
        // GIVEN
        String refNo = "ref-123";
        Long maliciousUserId = 666L;

        User sender = User.builder().id(1L).build();
        User receiver = User.builder().id(2L).build();

        Transaction transaction = Transaction.builder()
                .referenceNo(refNo)
                .fromWallet(Wallet.builder().user(sender).build())
                .toWallet(Wallet.builder().user(receiver).build())
                .build();

        when(transactionRepository.findByReferenceNo(refNo)).thenReturn(Optional.of(transaction));

        assertThrows(AccessDeniedException.class, () ->
                transactionService.getTransactionReceipt(refNo, maliciousUserId)
        );

        verify(transactionMapper, never()).toReceiptResponse(any());
    }

    @Test
    @DisplayName("Should NOT throw NPE for DEPOSIT transaction (fromWallet is null)")
    void getByReference_Deposit_NoNPE() {
        // GIVEN
        String refNo = "deposit-ref";
        Long userId = 1L;

        User user = User.builder().id(userId).build();
        Wallet myWallet = Wallet.builder().user(user).build();

        Transaction depositTx = Transaction.builder()
                .referenceNo(refNo)
                .fromWallet(null)
                .toWallet(myWallet)
                .type(TransactionType.DEPOSIT)
                .build();

        when(transactionRepository.findByReferenceNo(refNo)).thenReturn(Optional.of(depositTx));
        when(transactionMapper.toTransactionResponse(any())).thenReturn(mock(TransactionResponse.class));

        // WHEN
        assertDoesNotThrow(() -> transactionService.getByReference(refNo, userId));
    }

    @Test
    @DisplayName("Should NOT throw NPE for WITHDRAWAL transaction (toWallet is null)")
    void getByReference_Withdraw_NoNPE() {
        // GIVEN
        String refNo = "withdraw-ref";
        Long userId = 1L;

        User user = User.builder().id(userId).build();
        Wallet myWallet = Wallet.builder().user(user).build();

        Transaction withdrawTx = Transaction.builder()
                .referenceNo(refNo)
                .fromWallet(myWallet)
                .toWallet(null)
                .type(TransactionType.WITHDRAWAL)
                .build();

        when(transactionRepository.findByReferenceNo(refNo)).thenReturn(Optional.of(withdrawTx));
        when(transactionMapper.toTransactionResponse(any())).thenReturn(mock(TransactionResponse.class));

        // WHEN
        assertDoesNotThrow(() -> transactionService.getByReference(refNo, userId));
    }

    @Test
    @DisplayName("Should handle NULL category in stats by grouping as OTHER")
    void getTransactionStats_NullCategorySafe() {
        // GIVEN
        Long userId = 1L;
        User user = User.builder().id(userId).build();
        Wallet wallet = Wallet.builder().user(user).build();

        Transaction nullCategoryTx = Transaction.builder()
                .amount(BigDecimal.valueOf(100))
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.TRANSFER)
                .category(null)
                .fromWallet(wallet)
                .toWallet(wallet)
                .build();

        when(transactionRepository.findTransactionsForStats(any(), any(), any()))
                .thenReturn(List.of(nullCategoryTx));

        // WHEN
        TransactionStatsResponse response = transactionService.getTransactionStats(userId, "month", null, null);

        // THEN
        assertNotNull(response);
        assertTrue(response.byCategory().containsKey(TransactionCategory.OTHER));
        assertEquals(1, response.byCategory().get(TransactionCategory.OTHER).count());
    }

    @Test
    @DisplayName("Should return paginated transaction history successfully")
    void getTransactionHistory_Paginated_Success() {
        // GIVEN
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        User user = User.builder().id(userId).build();
        Wallet wallet = Wallet.builder().user(user).build();

        Transaction transaction = Transaction.builder()
                .id(1L)
                .amount(BigDecimal.valueOf(100))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .fromWallet(wallet)
                .toWallet(wallet)
                .build();

        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction), pageable, 1);

        TransactionResponse response = new TransactionResponse(
                1L, "User1", "User2", BigDecimal.valueOf(100),
                Currency.TRY, TransactionType.TRANSFER, TransactionStatus.COMPLETED,
                TransactionCategory.OTHER, "REF-123", "Test", LocalDateTime.now()
        );

        when(transactionRepository.findTransactionsByUserId(userId, pageable)).thenReturn(transactionPage);
        when(transactionMapper.toTransactionResponse(any())).thenReturn(response);

        // WHEN
        Page<TransactionResponse> result = transactionService.getTransactionHistory(userId, pageable);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        verify(transactionRepository).findTransactionsByUserId(userId, pageable);
    }

    @Test
    @DisplayName("Should filter transactions successfully with all criteria")
    void getFilteredTransactionHistory_Success() {
        // GIVEN
        Long userId = 1L;
        TransactionFilterRequest filter = TransactionFilterRequest.builder()
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .category(TransactionCategory.SHOPPING)
                .minAmount(BigDecimal.valueOf(100))
                .maxAmount(BigDecimal.valueOf(500))
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        User user = User.builder().id(userId).build();
        Wallet wallet = Wallet.builder().user(user).build();

        Transaction transaction = Transaction.builder()
                .id(1L)
                .amount(BigDecimal.valueOf(200))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .category(TransactionCategory.SHOPPING)
                .fromWallet(wallet)
                .toWallet(wallet)
                .build();

        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction), pageable, 1);

        TransactionResponse response = new TransactionResponse(
                1L, "User1", "User2", BigDecimal.valueOf(200),
                Currency.TRY, TransactionType.TRANSFER, TransactionStatus.COMPLETED,
                TransactionCategory.SHOPPING, "REF-123", "Shopping", LocalDateTime.now()
        );

        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(transactionPage);
        when(transactionMapper.toTransactionResponse(any())).thenReturn(response);

        // WHEN
        Page<TransactionResponse> result = transactionService.getFilteredTransactionHistory(userId, filter, pageable);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(TransactionType.TRANSFER, result.getContent().get(0).type());
        verify(transactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Should return empty page when no transactions match filter")
    void getFilteredTransactionHistory_EmptyResult() {
        // GIVEN
        Long userId = 1L;
        TransactionFilterRequest filter = TransactionFilterRequest.builder()
                .type(TransactionType.WITHDRAWAL)
                .minAmount(BigDecimal.valueOf(10000))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        // WHEN
        Page<TransactionResponse> result = transactionService.getFilteredTransactionHistory(userId, filter, pageable);

        // THEN
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
}


