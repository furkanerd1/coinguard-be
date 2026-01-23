package com.coinguard.budget.dto.request;

import com.coinguard.receipt.enums.ReceiptCategory;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBudgetRequest(
        @NotNull(message = "Category cannot be null")
        ReceiptCategory category,

        @NotNull(message = "Limit amount cannot be null")
        @Positive(message = "Limit amount must be positive")
        BigDecimal limitAmount,

        @NotNull(message = "Period start date is required")
        @FutureOrPresent(message = "Start date cannot be in the past")
        LocalDate periodStart,

        @NotNull(message = "Period end date is required")
        @FutureOrPresent(message = "End date cannot be in the past")
        LocalDate periodEnd,

        Integer alertThreshold
){}
