package com.coinguard.receipt.controller;

import com.coinguard.common.constant.RestApiPaths;
import com.coinguard.common.response.ApiResponse;
import com.coinguard.common.response.PageResponse;
import com.coinguard.receipt.dto.ReceiptDto;
import com.coinguard.receipt.service.ReceiptService;
import com.coinguard.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<ApiResponse<PageResponse<ReceiptDto>>> getMyReceipts(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ReceiptDto> receiptPage = receiptService.getUserReceipts(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(receiptPage)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ReceiptDto>> approveReceipt(@AuthenticationPrincipal User user, @PathVariable("id") Long receiptId) {
        return ResponseEntity.ok(ApiResponse.success(receiptService.approveReceipt(user.getId(), receiptId)));
    }
}