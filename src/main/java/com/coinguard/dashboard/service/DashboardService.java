package com.coinguard.dashboard.service;

import com.coinguard.dashboard.dto.response.DashboardSummaryResponse;

public interface DashboardService {
    DashboardSummaryResponse getDashboardSummary(Long userId);
}
