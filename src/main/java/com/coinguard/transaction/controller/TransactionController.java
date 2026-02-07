package com.coinguard.transaction.controller;

import com.coinguard.common.constant.RestApiPaths;
import com.coinguard.common.response.ApiResponse;
import com.coinguard.transaction.dto.request.DepositRequest;
import com.coinguard.transaction.dto.request.TransferRequest;
import com.coinguard.transaction.dto.request.WithdrawRequest;
import com.coinguard.transaction.dto.response.ReceiptResponse;
import com.coinguard.transaction.dto.response.TransactionResponse;
import com.coinguard.transaction.dto.response.TransactionStatsResponse;
import com.coinguard.transaction.service.TransactionService;
import com.coinguard.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


// todo : swagger docs
@RestController
@RequestMapping(RestApiPaths.Transaction.CTRL)
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping(RestApiPaths.Transaction.TRANSFER)
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(@AuthenticationPrincipal User currentUser, @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.transfer(currentUser.getId(), request)));
    }

    @PostMapping(RestApiPaths.Transaction.DEPOSIT)
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.deposit(currentUser.getId(), request)));
    }


    @PostMapping(RestApiPaths.Transaction.WITHDRAW)
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.withdraw(currentUser.getId(), request)));
    }

    @GetMapping(RestApiPaths.Transaction.HISTORY)
    public ResponseEntity<ApiResponse<?>> getTransactionHistory(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactionHistory(currentUser.getId(), page, size)));
    }

    @GetMapping("/{referenceNo}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getByReference(
            @PathVariable String referenceNo, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getByReference(referenceNo, currentUser.getId())));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TransactionStatsResponse>> getStats(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactionStats(user.getId(), period, startDate, endDate)));
    }

    @GetMapping("/{referenceNo}/receipt")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceipt(@PathVariable String referenceNo, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactionReceipt(referenceNo, user.getId())));
    }
}

