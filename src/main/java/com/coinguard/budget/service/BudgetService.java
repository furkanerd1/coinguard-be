package com.coinguard.budget.service;

import com.coinguard.budget.dto.request.CreateBudgetRequest;
import com.coinguard.budget.dto.response.BudgetResponse;
import com.coinguard.common.enums.TransactionCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BudgetService {

    BudgetResponse createBudget(Long userId, CreateBudgetRequest request);

    List<BudgetResponse> getUserBudgets(Long userId);

    void deleteBudget(Long userId, Long budgetId);

    void trackExpense(Long userId, BigDecimal amount, TransactionCategory category, LocalDate transactionDate);
}
