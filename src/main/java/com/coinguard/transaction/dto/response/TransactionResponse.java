package com.coinguard.transaction.dto.response;

import com.coinguard.common.enums.Currency;
import com.coinguard.common.enums.TransactionCategory;
import com.coinguard.transaction.enums.TransactionStatus;
import com.coinguard.transaction.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String senderName,
        String receiverName,
        BigDecimal amount,
        Currency currency,
        TransactionType type,
        TransactionStatus status,
        TransactionCategory category,
        String referenceNo,
        String description,
        LocalDateTime transactionDate
){}
