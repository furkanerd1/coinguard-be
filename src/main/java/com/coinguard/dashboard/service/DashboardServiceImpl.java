package com.coinguard.dashboard.service;

import com.coinguard.budget.dto.response.BudgetResponse;
import com.coinguard.budget.service.BudgetService;
import com.coinguard.dashboard.dto.response.DashboardSummaryResponse;
import com.coinguard.transaction.dto.response.TransactionStatsResponse;
import com.coinguard.transaction.service.TransactionService;
import com.coinguard.wallet.dto.response.WalletResponse;
import com.coinguard.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final WalletService walletService;
    private final TransactionService transactionService;
    private final BudgetService budgetService;

    @Override
    public DashboardSummaryResponse getDashboardSummary(Long userId) {
        log.info("Fetching dashboard summary for user ID: {}", userId);

        WalletResponse wallet = walletService.getWalletByUserId(userId);
        TransactionStatsResponse stats = transactionService.getTransactionStats(userId, "month", null, null);
        List<BudgetResponse> activeBudgets = budgetService.getUserBudgets(userId).stream().filter(BudgetResponse::isActive).toList();

        return new DashboardSummaryResponse(wallet, stats, activeBudgets);
    }
}
