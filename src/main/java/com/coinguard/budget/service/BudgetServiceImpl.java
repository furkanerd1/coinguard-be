package com.coinguard.budget.service;

import com.coinguard.budget.dto.request.CreateBudgetRequest;
import com.coinguard.budget.dto.response.BudgetResponse;
import com.coinguard.budget.entity.Budget;
import com.coinguard.budget.mapper.BudgetMapper;
import com.coinguard.budget.repository.BudgetRepository;
import com.coinguard.common.exception.ActiveBudgetAlreadyExistsException;
import com.coinguard.common.exception.BudgetNotFoundException;
import com.coinguard.common.exception.InvalidBudgetPeriodException;
import com.coinguard.common.exception.UserNotFoundException;
import com.coinguard.user.entity.User;
import com.coinguard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final BudgetMapper budgetMapper;

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

        return budgetMapper.toBudgetResponse(budgetRepository.save(toCreateBudget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> getUserBudgets(Long userId) {
        log.info("Retrieving budgets for user {}", userId);
        List<Budget> budgets = budgetRepository.findAllByUserId(userId);
        return budgetMapper.toBudgetResponseList(budgets);
    }

    @Override
    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        log.info("Deleting budget {} for user {}", budgetId, userId);
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));

        if (!budget.getUser().getId().equals(userId)) {
            throw new SecurityException("You are not authorized to delete this budget");
        }

        budgetRepository.delete(budget);
        log.info("Budget {} deleted successfully", budgetId);
    }
}
