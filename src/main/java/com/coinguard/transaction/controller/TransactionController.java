package com.coinguard.transaction.controller;

import com.coinguard.common.constant.RestApiPaths;
import com.coinguard.common.response.ApiResponse;
import com.coinguard.transaction.dto.request.DepositRequest;
import com.coinguard.transaction.dto.request.TransferRequest;
import com.coinguard.transaction.dto.request.WithdrawRequest;
import com.coinguard.transaction.dto.response.TransactionResponse;
import com.coinguard.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


// todo : swagger docs
// todo : @RequestHeader("X-User-Id") Long senderId -> get from auth context
@RestController
@RequestMapping(RestApiPaths.Transaction.CTRL)
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;


    @PostMapping(RestApiPaths.Transaction.TRANSFER)
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @RequestHeader("X-User-Id") Long senderId,
            @Valid @RequestBody TransferRequest request) {

        return ResponseEntity.ok(ApiResponse.success(transactionService.transfer(senderId, request)));
    }

    @PostMapping(RestApiPaths.Transaction.DEPOSIT)
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.deposit(userId, request)));
    }


    @PostMapping(RestApiPaths.Transaction.WITHDRAW)
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.withdraw(userId, request)));
    }

    @GetMapping(RestApiPaths.Transaction.HISTORY)
    public ResponseEntity<ApiResponse<?>> getTransactionHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactionHistory(userId, page, size)));
    }

    @GetMapping("/{referenceNo}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getByReference(
            @PathVariable String referenceNo) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getByReference(referenceNo)));
    }
}

