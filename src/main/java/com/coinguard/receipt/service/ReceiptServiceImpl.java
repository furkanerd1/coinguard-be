package com.coinguard.receipt.service;

import com.coinguard.common.enums.TransactionCategory;
import com.coinguard.common.exception.FileValidationException;
import com.coinguard.common.exception.WalletNotFoundException;
import com.coinguard.common.service.FileStorageService;
import com.coinguard.receipt.dto.ReceiptDto;
import com.coinguard.receipt.dto.ai.ExtractedReceiptData;
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

import java.time.LocalDate;
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
    private final GeminiOcrService geminiOcrService;

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
        try {
            //update status to processing
            savedReceipt.setStatus(ProcessingStatus.PROCESSING);
            receiptRepository.save(savedReceipt);

            // ask to gemini to extract data from the image
            ExtractedReceiptData aiData = geminiOcrService.extractDataFromImage(filePath);

            // update receipt with extracted data
            savedReceipt.setMerchantName(aiData.merchantName());
            savedReceipt.setAmount(aiData.amount());

            // date formatting
            if (aiData.date() != null) {
                savedReceipt.setReceiptDate(LocalDate.parse(aiData.date()));
            }
            // category mapping with fallback
            if (aiData.category() != null) {
                try {
                    savedReceipt.setCategory(TransactionCategory.valueOf(aiData.category()));
                } catch (IllegalArgumentException e) {
                    savedReceipt.setCategory(TransactionCategory.OTHER);
                }
            }
            savedReceipt.setGeminiConfidence(aiData.confidence());
            savedReceipt.setStatus(ProcessingStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Error during AI processing", e);

            savedReceipt.setStatus(ProcessingStatus.FAILED);

            String rawError = e.getMessage();
            if (rawError != null && rawError.length() > 400) {
                savedReceipt.setErrorMessage(rawError.substring(0, 400) + "... [Truncated]");
            } else {
                savedReceipt.setErrorMessage(rawError);
            }
        }

        // TODO: sende a mesege to rabbitmq to process the receipt asynchronously
        // messageQueue.send("process-receipt", savedReceipt.getId());

        Receipt finalReceipt = receiptRepository.save(savedReceipt);
        return receiptMapper.toDto(finalReceipt);
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