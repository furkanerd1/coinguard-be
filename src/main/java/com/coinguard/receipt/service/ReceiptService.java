package com.coinguard.receipt.service;

import com.coinguard.receipt.dto.ReceiptDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReceiptService {
    ReceiptDto uploadReceipt(Long userId, MultipartFile file);

    List<ReceiptDto> getUserReceipts(Long userId);

    Page<ReceiptDto> getUserReceipts(Long userId, Pageable pageable);

    ReceiptDto approveReceipt(Long userId, Long receiptId);
}
