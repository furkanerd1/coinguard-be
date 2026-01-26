package com.coinguard.wallet.controller;

import com.coinguard.common.constant.RestApiPaths;
import com.coinguard.common.response.ApiResponse;
import com.coinguard.wallet.dto.response.WalletResponse;
import com.coinguard.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


// todo : userID should be fetched from JWT token after implementing authentication
@RestController
@RequestMapping(RestApiPaths.Wallet.CTRL)
@RequiredArgsConstructor
@Validated
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWalletByUserId(userId)));
    }

    @PostMapping("/reset-limits")
    public ResponseEntity<ApiResponse<Void>> resetDailyLimits() {
        walletService.resetDailyLimits();
        return ResponseEntity.ok(ApiResponse.success("Daily limits reset successfully"));
    }
}
