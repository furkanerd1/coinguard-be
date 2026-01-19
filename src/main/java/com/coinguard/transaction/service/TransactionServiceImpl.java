package com.coinguard.transaction.service;

import com.coinguard.common.exception.WalletNotFoundException;
import com.coinguard.transaction.dto.request.DepositRequest;
import com.coinguard.transaction.dto.request.TransferRequest;
import com.coinguard.transaction.dto.request.WithdrawRequest;
import com.coinguard.transaction.dto.response.TransactionResponse;
import com.coinguard.transaction.entity.Transaction;
import com.coinguard.transaction.enums.TransactionStatus;
import com.coinguard.transaction.enums.TransactionType;
import com.coinguard.transaction.mapper.TransactionMapper;
import com.coinguard.transaction.repository.TransactionRepository;
import com.coinguard.wallet.entity.Wallet;
import com.coinguard.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public TransactionResponse transfer(Long senderId, TransferRequest request) {
        if (senderId.equals(request.receiverId())) {
            throw new IllegalArgumentException("Cannot transfer money to yourself");
        }

        Wallet senderWallet = findWallet(senderId);
        Wallet receiverWallet = findWallet(request.receiverId());

        if(!senderWallet.hasSufficientBalance(request.amount())){
            throw new IllegalArgumentException("Insufficient balance");
        }

        //State change
        senderWallet.setBalance(senderWallet.getBalance().subtract(request.amount()));
        receiverWallet.setBalance(receiverWallet.getBalance().add(request.amount()));

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        Transaction transaction = Transaction.builder()
                .fromWallet(senderWallet)
                .toWallet(receiverWallet)
                .amount(request.amount())
                .currency(senderWallet.getCurrency())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .referenceNo(UUID.randomUUID().toString())
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .build();

        return transactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    private Wallet findWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
    }

    @Override
    @Transactional
    public TransactionResponse deposit(Long userId, DepositRequest request) {
        Wallet wallet = findWallet(userId);

        simulateBankLatency();

        //State change
        wallet.setBalance(wallet.getBalance().add(request.amount()));
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .toWallet(wallet)
                .fromWallet(null) // Dışarıdan geldiği için gönderen yok
                .amount(request.amount())
                .currency(wallet.getCurrency())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .referenceNo(UUID.randomUUID().toString())
                .description("Deposit via " + request.cardNumber())
                .createdAt(LocalDateTime.now())
                .build();

        return transactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    private void simulateBankLatency() {
        try {
            Thread.sleep(1500); // wait for 1.5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public TransactionResponse withdraw(Long userId, WithdrawRequest request) {
        Wallet wallet = findWallet(userId);

        // balance check
        if (!wallet.hasSufficientBalance(request.amount())) {
            throw new IllegalArgumentException("Insufficient balance for withdrawal");
        }

        simulateBankLatency();

        // state change
        wallet.setBalance(wallet.getBalance().subtract(request.amount()));
        walletRepository.save(wallet);

        // create transaction record
        Transaction transaction = Transaction.builder()
                .fromWallet(wallet)
                .toWallet(null)
                .amount(request.amount())
                .currency(wallet.getCurrency())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .referenceNo(UUID.randomUUID().toString())
                .description("Withdrawal to " + request.iban())
                .createdAt(LocalDateTime.now())
                .build();

        return transactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }

    @Override
    public Page<TransactionResponse> getTransactionHistory(Long userId, int page, int size) {
        return null;
    }

    @Override
    public TransactionResponse getByReference(String referenceNo) {
        Transaction transaction = transactionRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with reference: " + referenceNo));

        return transactionMapper.toTransactionResponse(transaction);
    }
}
