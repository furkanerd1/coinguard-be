package com.coinguard.transaction.dto.response;

import com.coinguard.common.enums.TransactionCategory;
import com.coinguard.transaction.enums.TransactionStatus;
import com.coinguard.transaction.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceiptResponse(
        String referenceNo,
        Long transactionId,
        String senderName,
        String senderAccount,
        String receiverName,
        String receiverAccount,
        BigDecimal amount,
        String currency,
        BigDecimal transactionFee,
        BigDecimal totalDeducted,
        TransactionType type,
        TransactionCategory category,
        TransactionStatus status,
        String description,
        LocalDateTime transactionDate,
        LocalDateTime createdAt
){}