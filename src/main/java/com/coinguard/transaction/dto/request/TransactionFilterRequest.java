package com.coinguard.transaction.dto.request;

import com.coinguard.common.enums.Currency;
import com.coinguard.common.enums.TransactionCategory;
import com.coinguard.transaction.enums.TransactionStatus;
import com.coinguard.transaction.enums.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionFilterRequest(
        TransactionType type,
        TransactionStatus status,
        TransactionCategory category,
        Currency currency,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String searchQuery  // For searching in description, reference, sender/receiver name
) {
}
