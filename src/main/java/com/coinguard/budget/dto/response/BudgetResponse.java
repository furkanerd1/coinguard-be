package com.coinguard.budget.dto.response;

import com.coinguard.common.enums.TransactionCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetResponse(
        Long id,
        Long userId,
        TransactionCategory category,
        BigDecimal limitAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        Double usagePercentage,
        LocalDate periodStart,
        LocalDate periodEnd,
        boolean isActive,
        boolean isExceeded
) {
}
