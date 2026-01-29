package com.coinguard.transaction.dto.request;

import com.coinguard.common.enums.TransactionCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DepositRequest(
        @NotNull
        @Positive
        BigDecimal amount,

        @NotNull
        @Pattern(regexp = "^[0-9]{16}$", message = "Card number must be 16 digits")
        String cardNumber,

        @Pattern(regexp = "^(0[1-9]|1[0-2])\\/?([0-9]{2})$", message = "Invalid expiry date format (MM/YY)")
        String expiryDate,

        @Pattern(regexp = "^[0-9]{3}$", message = "CVC must be 3 digits")
        String cvc,

        TransactionCategory category
){}
