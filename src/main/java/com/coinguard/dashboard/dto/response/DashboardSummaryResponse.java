package com.coinguard.dashboard.dto.response;

import com.coinguard.budget.dto.response.BudgetResponse;
import com.coinguard.transaction.dto.response.TransactionStatsResponse;
import com.coinguard.wallet.dto.response.WalletResponse;

import java.util.List;

public record DashboardSummaryResponse(
        WalletResponse walletDetails,
        TransactionStatsResponse monthlyStats,
        List<BudgetResponse> activeBudgets
){}
