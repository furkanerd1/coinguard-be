package com.coinguard.wallet.controller;

import com.coinguard.common.constant.RestApiPaths;
import com.coinguard.common.response.ApiResponse;
import com.coinguard.user.entity.User;
import com.coinguard.wallet.dto.response.WalletResponse;
import com.coinguard.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(RestApiPaths.Wallet.CTRL)
@RequiredArgsConstructor
@Validated
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWalletByUserId(currentUser.getId())));
    }

    @PostMapping("/reset-limits")
    public ResponseEntity<ApiResponse<Void>> resetDailyLimits() {
        walletService.resetDailyLimits();
        return ResponseEntity.ok(ApiResponse.success("Daily limits reset successfully"));
    }
}
