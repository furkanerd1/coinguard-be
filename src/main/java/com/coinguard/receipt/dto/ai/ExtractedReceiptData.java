package com.coinguard.receipt.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractedReceiptData(
        String merchantName,
        String date,
        BigDecimal amount,
        String category,
        Double confidence
) {}
