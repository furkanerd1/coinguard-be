package com.coinguard.receipt.service;

import com.coinguard.common.exception.FileValidationException;
import com.coinguard.common.exception.WalletNotFoundException;
import com.coinguard.common.service.FileStorageService;
import com.coinguard.receipt.dto.ReceiptDto;
import com.coinguard.receipt.entity.Receipt;
import com.coinguard.receipt.enums.ProcessingStatus;
import com.coinguard.receipt.mapper.ReceiptMapper;
import com.coinguard.receipt.repository.ReceiptRepository;
import com.coinguard.wallet.entity.Wallet;
import com.coinguard.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final WalletRepository walletRepository;
    private final FileStorageService fileStorageService;
    private final ReceiptMapper receiptMapper;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/jpg");

    @Override
    @Transactional
    public ReceiptDto uploadReceipt(Long userId, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new FileValidationException("Invalid file type. Only JPG and PNG images are allowed.");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        String filePath = fileStorageService.storeFile(file, "receipts");

        Receipt receipt = Receipt.builder()
                .wallet(wallet)
                .fileUrl(filePath)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .status(ProcessingStatus.UPLOADED)
                .build();

        Receipt savedReceipt = receiptRepository.save(receipt);
        log.info("Receipt uploaded successfully. ID: {}, User: {}", savedReceipt.getId(), userId);

        // TODO: sende a mesege to rabbitmq to process the receipt asynchronously
        // messageQueue.send("process-receipt", savedReceipt.getId());

        return receiptMapper.toDto(savedReceipt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptDto> getUserReceipts(Long userId) {
        //TODO: add pagination and sorting
        return receiptRepository.findByWallet_User_Id(userId).stream()
                .map(receiptMapper::toDto)
                .toList();
    }
}