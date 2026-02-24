package com.coinguard.receipt.controller;

import com.coinguard.common.constant.RestApiPaths;
import com.coinguard.common.response.ApiResponse;
import com.coinguard.receipt.dto.ReceiptDto;
import com.coinguard.receipt.service.ReceiptService;
import com.coinguard.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(RestApiPaths.Receipt.CTRL)
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping(value = RestApiPaths.Receipt.UPLOAD, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReceiptDto>> uploadReceipt
            (@RequestParam("file") MultipartFile file,
             @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(receiptService.uploadReceipt(user.getId(), file)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReceiptDto>>> getMyReceipts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(receiptService.getUserReceipts(user.getId())));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ReceiptDto>> approveReceipt(@AuthenticationPrincipal User user, @PathVariable("id") Long receiptId) {
        return ResponseEntity.ok(ApiResponse.success(receiptService.approveReceipt(user.getId(), receiptId)));
    }
}