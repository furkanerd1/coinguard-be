package com.coinguard.budget.service;

import com.coinguard.budget.dto.request.CreateBudgetRequest;
import com.coinguard.budget.dto.response.BudgetResponse;
import com.coinguard.budget.entity.Budget;
import com.coinguard.budget.mapper.BudgetMapper;
import com.coinguard.budget.repository.BudgetRepository;
import com.coinguard.common.enums.TransactionCategory;
import com.coinguard.common.exception.*;
import com.coinguard.messaging.dto.NotificationMessage;
import com.coinguard.messaging.producer.NotificationMessageProducer;
import com.coinguard.user.entity.User;
import com.coinguard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final BudgetMapper budgetMapper;
    private final NotificationMessageProducer notificationProducer;

    @Override
    @Transactional
    public BudgetResponse createBudget(Long userId, CreateBudgetRequest request) {
        log.info("Creating budget for user {} in category {}", userId, request.category());

        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new InvalidBudgetPeriodException("End date cannot be before start date");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (budgetRepository.findByUserIdAndCategoryAndIsActiveTrue(userId, request.category()).isPresent()) {
            throw new ActiveBudgetAlreadyExistsException(request.category());
        }
        Budget toCreateBudget = Budget.builder()
                .user(user)
                .category(request.category())
                .limitAmount(request.limitAmount())
                .spentAmount(BigDecimal.ZERO)
                .periodStart(request.periodStart())
                .periodEnd(request.periodEnd())
                .isActive(true)
                .alertThreshold(request.alertThreshold() != null ? request.alertThreshold() : 80)
                .alertSent(false)
                .build();

        Budget savedBudget = budgetRepository.save(toCreateBudget);

        // Send notification after successful budget creation
        notificationProducer.sendNotificationMessage(new NotificationMessage(
                userId,
                "Budget Created",
                String.format("Budget created for %s with limit %s", request.category(), request.limitAmount()),
                "SUCCESS"
        ));

        return budgetMapper.toBudgetResponse(savedBudget);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getUserBudgets(Long userId) {
        log.info("Retrieving budgets for user {}", userId);
        List<Budget> budgets = budgetRepository.findAllByUserId(userId);
        return budgetMapper.toBudgetResponseList(budgets);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BudgetResponse> getUserBudgets(Long userId, Pageable pageable) {
        log.info("Retrieving paginated budgets for user {} with page {}", userId, pageable);
        Page<Budget> budgets = budgetRepository.findAllByUserId(userId, pageable);
        return budgets.map(budgetMapper::toBudgetResponse);
    }

    @Override
    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        log.info("Deleting budget {} for user {}", budgetId, userId);
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));

        if (!budget.getUser().getId().equals(userId)) {
            throw new AuthorizationException("You are not authorized to delete this budget");
        }

        budgetRepository.delete(budget);
        log.info("Budget {} deleted successfully", budgetId);

        // Send notification after successful budget deletion
        notificationProducer.sendNotificationMessage(new NotificationMessage(
                userId,
                "Budget Deleted",
                String.format("Budget for %s has been deleted", budget.getCategory()),
                "INFO"
        ));
    }

    @Override
    public void trackExpense(Long userId, BigDecimal amount, TransactionCategory category, LocalDate transactionDate) {
        log.info("Tracking expense for User: {}, Category: {}, Amount: {}", userId, category, amount);

        var activeBudgetOpt = budgetRepository.findActiveBudgetByCategory(userId, category, transactionDate);

        if (activeBudgetOpt.isPresent()) {
            Budget budget = activeBudgetOpt.get();

            BigDecimal newSpent = budget.getSpentAmount().add(amount);
            budget.setSpentAmount(newSpent);

            checkThreshold(budget);

            budgetRepository.save(budget);
            log.info("Budget updated. New Spent: {}", newSpent);

            // Send alert if budget limit exceeded (100%)
            BigDecimal limitAmount = budget.getLimitAmount();
            if (newSpent.compareTo(limitAmount) >= 0) {
                notificationProducer.sendNotificationMessage(new NotificationMessage(
                        userId,
                        "Budget Limit Exceeded",
                        String.format("Your %s budget limit has been exceeded. Spent: %s, Limit: %s",
                                category, newSpent, limitAmount),
                        "WARNING"
                ));
            }
        } else {
            log.warn("No active budget found for category: {}", category);
        }
    }

    private void checkThreshold(Budget budget) {
        if (Boolean.TRUE.equals(budget.getAlertSent())) {
            return;
        }

        Double usagePercentage = budget.getUsagePercentage();

        if (usagePercentage.compareTo(Double.valueOf(budget.getAlertThreshold())) >= 0) {
            log.warn("ALERT: Budget threshold exceeded! Usage: {}%", usagePercentage);
            budget.setAlertSent(true);
        }
    }
}
