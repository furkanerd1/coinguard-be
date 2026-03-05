package com.coinguard.transaction.service;

import com.coinguard.budget.service.BudgetService;
import com.coinguard.common.enums.TransactionCategory;
import com.coinguard.common.exception.InsufficientBalanceException;
import com.coinguard.common.exception.SelfTransferException;
import com.coinguard.common.exception.TransactionNotFoundException;
import com.coinguard.common.exception.WalletNotFoundException;
import com.coinguard.messaging.dto.NotificationMessage;
import com.coinguard.messaging.producer.NotificationMessageProducer;
import com.coinguard.notification.dto.NotificationResponse;
import com.coinguard.notification.entity.NotificationType;
import com.coinguard.transaction.dto.request.DepositRequest;
import com.coinguard.transaction.dto.request.TransactionFilterRequest;
import com.coinguard.transaction.dto.request.TransferRequest;
import com.coinguard.transaction.dto.request.WithdrawRequest;
import com.coinguard.transaction.dto.response.ReceiptResponse;
import com.coinguard.transaction.dto.response.TransactionResponse;
import com.coinguard.transaction.dto.response.TransactionStatsResponse;
import com.coinguard.transaction.entity.Transaction;
import com.coinguard.transaction.enums.TransactionStatus;
import com.coinguard.transaction.enums.TransactionType;
import com.coinguard.transaction.mapper.TransactionMapper;
import com.coinguard.transaction.repository.TransactionRepository;
import com.coinguard.transaction.specification.TransactionSpecification;
import com.coinguard.wallet.entity.Wallet;
import com.coinguard.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final TransactionMapper transactionMapper;
    private final BudgetService budgetService;
    private final NotificationMessageProducer notificationProducer;

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

        notificationProducer.sendNotificationMessage(NotificationMessage.builder()
                .userId(senderWallet.getUser().getId())
                .title("Transfer Successful!")
                .message(String.format("You sent %s %s to user %s.",
                         request.amount(), senderWallet.getCurrency(),receiverWallet.getUser().getFullName()))
                .type("SUCCESS")
                .build());

        notificationProducer.sendNotificationMessage(NotificationMessage.builder()
                .userId(receiverWallet.getUser().getId())
                .title("New Transfer Received!")
                .message(String.format("You received %s %s from user %s in your account.",
                        request.amount(), receiverWallet.getCurrency(),senderWallet.getUser().getFullName()))
                .type("INFO")
                .build());
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
    public Page<TransactionResponse> getTransactionHistory(Long userId, Pageable pageable) {
        return transactionRepository.findTransactionsByUserId(userId, pageable)
                .map(transactionMapper::toTransactionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getFilteredTransactionHistory(Long userId, TransactionFilterRequest filterRequest, Pageable pageable) {
        Specification<Transaction> spec = TransactionSpecification.filterTransactions(userId, filterRequest);
        return transactionRepository.findAll(spec, pageable)
                .map(transactionMapper::toTransactionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getByReference(String referenceNo, Long currentUserId) {
        Transaction transaction = findTransactionSecured(referenceNo, currentUserId);
        return transactionMapper.toTransactionResponse(transaction);
    }

    @Override
    public ReceiptResponse getTransactionReceipt(String referenceNo, Long currentUserId) {
        Transaction transaction = findTransactionSecured(referenceNo, currentUserId);
        return transactionMapper.toReceiptResponse(transaction);
    }


    @Override
    @Transactional(readOnly = true)
    public TransactionStatsResponse getTransactionStats(Long userId, String period, LocalDate startDate, LocalDate endDate) {

        LocalDateTime startDateTime = calculateStartDate(period, startDate);
        LocalDateTime endDateTime = (endDate != null && "custom".equalsIgnoreCase(period)) ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        List<Transaction> transactions = transactionRepository.findTransactionsForStats(userId, startDateTime, endDateTime);

        long totalCount = transactions.size();

        List<Transaction> successfulTx = transactions.stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED).toList();

        long successCount = successfulTx.size();
        long failedCount = totalCount - successCount;

        BigDecimal totalAmount = successfulTx.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgAmount = (successCount > 0) ? totalAmount.divide(BigDecimal.valueOf(successCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        double successRate = (totalCount > 0) ? (double) successCount / totalCount * 100 : 0.0;

        Map<TransactionType, TransactionStatsResponse.TypeStat> byType = calculateTypeStats(successfulTx);
        Map<TransactionCategory, TransactionStatsResponse.CategoryStat> byCategory = calculateCategoryStats(successfulTx);

        return new TransactionStatsResponse(totalCount, successCount, failedCount, totalAmount, avgAmount, successRate, byType, byCategory);
    }


    private Transaction findTransactionSecured(String referenceNo, Long userId) {
        Transaction transaction = transactionRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new TransactionNotFoundException(referenceNo));

        Long senderId = (transaction.getFromWallet() != null) ? transaction.getFromWallet().getUser().getId() : null;
        Long receiverId = (transaction.getToWallet() != null) ? transaction.getToWallet().getUser().getId() : null;
        boolean isOwner = userId.equals(senderId) || userId.equals(receiverId);

        if (!isOwner) {
            throw new AccessDeniedException("Bu işlemi görüntüleme yetkiniz yok.");
        }

        return transaction;
    }

    private LocalDateTime calculateStartDate(String period, LocalDate customStart) {
        if ("custom".equalsIgnoreCase(period) && customStart != null) {
            return customStart.atStartOfDay();
        }
        return switch (period != null ? period.toLowerCase() : "month") {
            case "day" -> LocalDate.now().atStartOfDay();
            case "week" -> LocalDate.now().minus(1, ChronoUnit.WEEKS).atStartOfDay();
            case "year" -> LocalDate.now().minus(1, ChronoUnit.YEARS).atStartOfDay();
            default -> LocalDate.now().minus(1, ChronoUnit.MONTHS).atStartOfDay();
        };
    }

    private Map<TransactionType, TransactionStatsResponse.TypeStat> calculateTypeStats(List<Transaction> transactions) {
        return transactions.stream().collect(Collectors.groupingBy
                (Transaction::getType,
                        Collectors.collectingAndThen(
                                Collectors.toList(), list -> new TransactionStatsResponse.TypeStat
                                        (list.size(), list.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                        )
                ));
    }

    private Map<TransactionCategory, TransactionStatsResponse.CategoryStat> calculateCategoryStats(List<Transaction> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() == null ? TransactionCategory.OTHER : t.getCategory(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> new TransactionStatsResponse.CategoryStat(
                                        list.size(),
                                        list.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                                )
                        )
                ));
    }
}
