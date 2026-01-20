package com.coinguard.transaction.service;

import com.coinguard.common.exception.InsufficientBalanceException;
import com.coinguard.common.exception.SelfTransferException;
import com.coinguard.transaction.dto.request.TransferRequest;
import com.coinguard.transaction.dto.response.TransactionResponse;
import com.coinguard.transaction.entity.Transaction;
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

    // Helper
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
                .build();
    }

    @Test
    @DisplayName("Should transfer money successfully when balance is sufficient")
    void shouldTransferMoneySuccessfully() {
        // GIVEN
        Long senderId = 1L;
        Long receiverId = 2L;
        BigDecimal amount = new BigDecimal("100.00");
        TransferRequest request = new TransferRequest(receiverId, amount, "Rent Payment");

        Wallet senderWallet = createWallet(senderId, new BigDecimal("500.00"));
        Wallet receiverWallet = createWallet(receiverId, new BigDecimal("200.00"));

        // Mocking behavior
        when(walletRepository.findByUserId(senderId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(receiverId)).thenReturn(Optional.of(receiverWallet));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);

        when(transactionMapper.toTransactionResponse(any(Transaction.class)))
                .thenReturn(new TransactionResponse(senderId, "Sender", "Receiver", amount, null, null, null, "REF-123", "Desc", null));

        // WHEN
        TransactionResponse response = transactionService.transfer(senderId, request);

        // THEN
        assertNotNull(response);
        assertEquals(new BigDecimal("400.00"), senderWallet.getBalance()); // 500 - 100
        assertEquals(new BigDecimal("300.00"), receiverWallet.getBalance()); // 200 + 100

        //Verify
        verify(walletRepository).save(senderWallet);
        verify(walletRepository).save(receiverWallet);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when balance is low")
    void shouldThrowException_WhenBalanceInsufficient() {
        // GIVEN
        Long senderId = 1L;
        TransferRequest request = new TransferRequest(2L, new BigDecimal("1000.00"), "Rent");

        Wallet senderWallet = createWallet(senderId, new BigDecimal("50.00"));
        Wallet receiverWallet = createWallet(2L, BigDecimal.ZERO);

        when(walletRepository.findByUserId(senderId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(receiverWallet));

        // WHEN & THEN
        assertThrows(InsufficientBalanceException.class, () -> {
            transactionService.transfer(senderId, request);
        });

        assertEquals(new BigDecimal("50.00"), senderWallet.getBalance());

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw SelfTransferException when sending to self")
    void shouldThrowException_WhenSelfTransfer() {
        // GIVEN
        Long senderId = 1L;

        TransferRequest request = new TransferRequest(1L, new BigDecimal("100.00"), "Self");

        // WHEN & THEN
        assertThrows(SelfTransferException.class, () -> {
            transactionService.transfer(senderId, request);
        });

        verify(walletRepository, never()).findByUserId(any());
        verify(transactionRepository, never()).save(any());
    }
}