package com.coinguard.dashboard.service;

import com.coinguard.budget.dto.response.BudgetResponse;
import com.coinguard.budget.service.BudgetService;
import com.coinguard.common.enums.Currency;
import com.coinguard.common.enums.TransactionCategory;
import com.coinguard.dashboard.dto.response.DashboardSummaryResponse;
import com.coinguard.transaction.dto.response.TransactionStatsResponse;
import com.coinguard.transaction.service.TransactionService;
import com.coinguard.wallet.dto.response.WalletResponse;
import com.coinguard.wallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Mock
    private WalletService walletService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private BudgetService budgetService;

    @Test
    @DisplayName("Should return complete dashboard summary successfully")
    void getDashboardSummary_Success() {
        // GIVEN
        Long userId = 1L;

        WalletResponse mockWallet = new WalletResponse(
                100L, userId, "Furkan", BigDecimal.valueOf(5000), Currency.TRY,
                BigDecimal.valueOf(10000), BigDecimal.ZERO, LocalDate.now(), false
        );

        TransactionStatsResponse mockStats = new TransactionStatsResponse(
                10, 8, 2, BigDecimal.valueOf(1500), BigDecimal.valueOf(150), 80.0,
                Map.of(), Map.of()
        );

        BudgetResponse activeBudget = new BudgetResponse(
                1L, userId, TransactionCategory.SHOPPING, BigDecimal.valueOf(2000),
                BigDecimal.valueOf(500), BigDecimal.valueOf(1500), 25.0,
                LocalDate.now(), LocalDate.now().plusDays(30), true, false
        );

        BudgetResponse inactiveBudget = new BudgetResponse(
                2L, userId, TransactionCategory.GROCERY, BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1000), BigDecimal.ZERO, 100.0,
                LocalDate.now().minusMonths(1), LocalDate.now().minusDays(1), false, true
        );

        when(walletService.getWalletByUserId(userId)).thenReturn(mockWallet);
        when(transactionService.getTransactionStats(eq(userId), eq("month"), any(), any())).thenReturn(mockStats);
        when(budgetService.getUserBudgets(userId)).thenReturn(List.of(activeBudget, inactiveBudget));

        // WHEN
        DashboardSummaryResponse result = dashboardService.getDashboardSummary(userId);

        // THEN
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(5000), result.walletDetails().balance());
        assertEquals(10, result.monthlyStats().totalTransactions());
        assertEquals(1, result.activeBudgets().size());
        assertEquals(TransactionCategory.SHOPPING, result.activeBudgets().get(0).category());

        verify(walletService).getWalletByUserId(userId);
        verify(transactionService).getTransactionStats(eq(userId), eq("month"), any(), any());
        verify(budgetService).getUserBudgets(userId);
    }
}