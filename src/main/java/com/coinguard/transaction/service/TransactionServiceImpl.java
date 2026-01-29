package com.coinguard.transaction.service;

import com.coinguard.budget.service.BudgetService;
import com.coinguard.common.enums.TransactionCategory;
import com.coinguard.common.exception.InsufficientBalanceException;
import com.coinguard.common.exception.SelfTransferException;
import com.coinguard.common.exception.TransactionNotFoundException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final TransactionMapper transactionMapper;
    private final BudgetService budgetService;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse transfer(Long senderId, TransferRequest request) {
        if (senderId.equals(request.receiverId())) {
            throw new SelfTransferException(senderId);
        }

        Long firstId = Math.min(senderId, request.receiverId());
        Long secondId = Math.max(senderId, request.receiverId());

        Wallet firstWallet = walletRepository.findByUserIdForUpdate(firstId)
                .orElseThrow(() -> new WalletNotFoundException(firstId));

        Wallet secondWallet = walletRepository.findByUserIdForUpdate(secondId)
                .orElseThrow(() -> new WalletNotFoundException(secondId));

        Wallet senderWallet = senderId.equals(firstId) ? firstWallet : secondWallet;
        Wallet receiverWallet = senderId.equals(firstId) ? secondWallet : firstWallet;

        if (!senderWallet.hasSufficientBalance(request.amount())) {
            throw new InsufficientBalanceException(senderWallet.getBalance());
        }

        senderWallet.debit(request.amount());
        receiverWallet.credit(request.amount());

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        TransactionCategory category = request.category() != null ? request.category() : TransactionCategory.OTHER;

        Transaction transaction = Transaction.builder()
                .fromWallet(senderWallet)
                .toWallet(receiverWallet)
                .amount(request.amount())
                .currency(senderWallet.getCurrency())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .category(category)
                .referenceNo(UUID.randomUUID().toString())
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            budgetService.trackExpense(senderId, request.amount(), category, LocalDate.now());
        } catch (Exception e) {
            log.error("Failed to update budget for transfer: {}", e.getMessage());
        }
        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    private Wallet findWalletForUpdate(Long userId) {
        return walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse deposit(Long userId, DepositRequest request) {
        Wallet wallet = findWalletForUpdate(userId);

        wallet.credit(request.amount());
        walletRepository.save(wallet);

        TransactionCategory category = request.category() != null ? request.category() : TransactionCategory.SALARY;

        Transaction transaction = Transaction.builder()
                .toWallet(wallet)
                .fromWallet(null)
                .amount(request.amount())
                .currency(wallet.getCurrency())
                .type(TransactionType.DEPOSIT)
                .category(category)
                .status(TransactionStatus.COMPLETED)
                .referenceNo(UUID.randomUUID().toString())
                .description("Deposit via " + request.cardNumber())
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        return transactionMapper.toTransactionResponse(transactionRepository.save(transaction));
    }


    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse withdraw(Long userId, WithdrawRequest request) {
        Wallet wallet = findWalletForUpdate(userId);

        if (!wallet.hasSufficientBalance(request.amount())) {
            throw new InsufficientBalanceException(wallet.getBalance());
        }

        wallet.debit(request.amount());
        walletRepository.save(wallet);

        TransactionCategory category = request.category() != null ? request.category() : TransactionCategory.OTHER;

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .fromWallet(wallet)
                .toWallet(null)
                .amount(request.amount())
                .currency(wallet.getCurrency())
                .type(TransactionType.WITHDRAWAL)
                .category(category)
                .status(TransactionStatus.COMPLETED)
                .referenceNo(UUID.randomUUID().toString())
                .description("Withdrawal to " + request.iban())
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        try {
            budgetService.trackExpense(userId, request.amount(), category, LocalDate.now());
        } catch (Exception e) {
            log.error("Failed to update budget for withdrawal: {}", e.getMessage());
        }

        return transactionMapper.toTransactionResponse(savedTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return transactionRepository.findTransactionsByUserId(userId, pageRequest)
                .map(transactionMapper::toTransactionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getByReference(String referenceNo) {
        Transaction transaction = transactionRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new TransactionNotFoundException(referenceNo));

        return transactionMapper.toTransactionResponse(transaction);
    }
}
