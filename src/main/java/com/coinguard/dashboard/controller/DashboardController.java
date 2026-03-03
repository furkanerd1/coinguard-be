package com.coinguard.dashboard.controller;

import com.coinguard.common.constant.RestApiPaths;
import com.coinguard.common.response.ApiResponse;
import com.coinguard.dashboard.dto.response.DashboardSummaryResponse;
import com.coinguard.dashboard.service.DashboardService;
import com.coinguard.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestApiPaths.API_VERSION_1 + "/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardSummary(currentUser.getId())));
    }
}