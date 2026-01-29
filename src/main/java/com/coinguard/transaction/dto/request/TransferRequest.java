package com.coinguard.transaction.dto.request;

import com.coinguard.common.enums.TransactionCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;


public record TransferRequest(
        @NotNull(message = "Receiver ID is required")
        Long receiverId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        @DecimalMin(value = "1.00", message = "Minimum transfer amount is 1.00")
        BigDecimal amount,

        String description,

        TransactionCategory category
){}
