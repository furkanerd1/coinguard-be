package com.coinguard.budget.controller;

import com.coinguard.budget.dto.request.CreateBudgetRequest;
import com.coinguard.budget.dto.response.BudgetResponse;
import com.coinguard.budget.service.BudgetService;
import com.coinguard.common.constant.RestApiPaths;
import com.coinguard.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// todo : UserId will come from security context after implementing auth
@RestController
@RequestMapping(RestApiPaths.Budget.CTRL)
@RequiredArgsConstructor
@Validated
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateBudgetRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(budgetService.createBudget(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getUserBudgets(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getUserBudgets(userId)));
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable @Positive(message = "Budget ID must be positive") Long budgetId) {

        budgetService.deleteBudget(userId, budgetId);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted successfully"));
    }
}
