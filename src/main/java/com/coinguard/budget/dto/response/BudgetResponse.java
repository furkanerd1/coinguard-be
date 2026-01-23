package com.coinguard.budget.dto.response;

import com.coinguard.receipt.enums.ReceiptCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetResponse(
        Long id,
        Long userId,
        ReceiptCategory category,
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
