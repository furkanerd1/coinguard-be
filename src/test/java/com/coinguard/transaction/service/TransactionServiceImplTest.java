package com.coinguard.transaction.service;

import com.coinguard.budget.service.BudgetService;
import com.coinguard.common.enums.Currency;
import com.coinguard.common.enums.TransactionCategory;
import com.coinguard.common.exception.InsufficientBalanceException;
import com.coinguard.common.exception.SelfTransferException;
import com.coinguard.transaction.dto.request.TransferRequest;
import com.coinguard.transaction.dto.response.TransactionResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    }
}