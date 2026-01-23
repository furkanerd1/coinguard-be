package com.coinguard.budget.service;

import com.coinguard.budget.dto.request.CreateBudgetRequest;
import com.coinguard.budget.dto.response.BudgetResponse;

import java.util.List;

public interface BudgetService {

    BudgetResponse createBudget(Long userId, CreateBudgetRequest request);


    List<BudgetResponse> getUserBudgets(Long userId);

    void deleteBudget(Long userId, Long budgetId);
}
