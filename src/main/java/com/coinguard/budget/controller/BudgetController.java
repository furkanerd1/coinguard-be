package com.coinguard.budget.controller;

import com.coinguard.budget.dto.request.CreateBudgetRequest;
import com.coinguard.budget.dto.response.BudgetResponse;
import com.coinguard.budget.service.BudgetService;
import com.coinguard.common.constant.RestApiPaths;
import com.coinguard.common.response.ApiResponse;
import com.coinguard.common.response.PageResponse;
import com.coinguard.user.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RestApiPaths.Budget.CTRL)
@RequiredArgsConstructor
@Validated
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateBudgetRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(budgetService.createBudget(currentUser.getId(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BudgetResponse>>> getUserBudgets(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "periodStart") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<BudgetResponse> budgetPage = budgetService.getUserBudgets(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(budgetPage)));
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @AuthenticationPrincipal User currentUser,
            @PathVariable @Positive(message = "Budget ID must be positive") Long budgetId) {

        budgetService.deleteBudget(currentUser.getId(), budgetId);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted successfully"));
    }
}
