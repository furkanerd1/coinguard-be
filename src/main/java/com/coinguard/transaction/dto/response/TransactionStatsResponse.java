package com.coinguard.transaction.dto.response;

import com.coinguard.common.enums.TransactionCategory;
import com.coinguard.transaction.enums.TransactionType;

import java.math.BigDecimal;
import java.util.Map;

public record TransactionStatsResponse(
        long totalTransactions,
        long successfulTransactions,
        long failedTransactions,
        BigDecimal totalAmount,
        BigDecimal averageAmount,
        double successRate,
        Map<TransactionType,TypeStat> byType,
        Map<TransactionCategory,CategoryStat> byCategory
){
    public record TypeStat(long count, BigDecimal total) {}
    public record CategoryStat(long count, BigDecimal total) {}
}
