package com.coinguard.receipt.dto;

import com.coinguard.common.enums.TransactionCategory;
import com.coinguard.receipt.enums.ProcessingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceiptDto(
        Long id,
        String fileUrl,
        ProcessingStatus status,
        String merchantName,
        BigDecimal amount,
        TransactionCategory category,
        Double confidence,
        LocalDateTime uploadDate
) {
}
