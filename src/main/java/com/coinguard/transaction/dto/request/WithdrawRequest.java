package com.coinguard.transaction.dto.request;

import com.coinguard.common.enums.TransactionCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record WithdrawRequest(
        @NotNull
        @Positive
        BigDecimal amount,

        @NotNull
        @Pattern(regexp = "^TR[0-9]{24}$", message = "Invalid TR IBAN format")
        String iban,

        TransactionCategory category
){}
