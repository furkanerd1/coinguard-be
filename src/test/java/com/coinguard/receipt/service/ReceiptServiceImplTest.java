package com.coinguard.receipt.service;

import com.coinguard.common.exception.FileValidationException;
import com.coinguard.common.exception.WalletNotFoundException;
import com.coinguard.common.service.FileStorageService;
import com.coinguard.receipt.dto.ReceiptDto;
import com.coinguard.receipt.entity.Receipt;
import com.coinguard.receipt.mapper.ReceiptMapper;
import com.coinguard.receipt.repository.ReceiptRepository;
import com.coinguard.wallet.entity.Wallet;
import com.coinguard.wallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceImplTest {

    @InjectMocks
    private ReceiptServiceImpl receiptService;

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private ReceiptMapper receiptMapper;

    @Test
    @DisplayName("Should upload receipt successfully when file is valid image")
    void uploadReceipt_Success() {
        // GIVEN
        Long userId = 1L;
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "image data".getBytes());

        Wallet wallet = new Wallet();
        wallet.setId(1L);

        Receipt savedReceipt = new Receipt();
        savedReceipt.setId(100L);
        savedReceipt.setFileUrl("uploads/receipts/uuid.jpg");

        ReceiptDto expectedDto = new ReceiptDto(100L, "uploads/receipts/uuid.jpg", null, null, null, null, null, null);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(fileStorageService.storeFile(any(), eq("receipts"))).thenReturn("uploads/receipts/uuid.jpg");
        when(receiptRepository.save(any(Receipt.class))).thenReturn(savedReceipt);
        when(receiptMapper.toDto(any(Receipt.class))).thenReturn(expectedDto);

        // WHEN
        ReceiptDto result = receiptService.uploadReceipt(userId, file);

        // THEN
        assertNotNull(result);
        assertEquals(100L, result.id());
        verify(fileStorageService).storeFile(any(), eq("receipts"));
        verify(receiptRepository).save(any(Receipt.class));
    }

    @Test
    @DisplayName("Should throw exception when file type is invalid (PDF)")
    void uploadReceipt_InvalidFileType() {
        // GIVEN
        Long userId = 1L;
        MockMultipartFile pdfFile = new MockMultipartFile("file", "document.pdf", "application/pdf", "pdf data".getBytes());

        // WHEN & THEN
        assertThrows(FileValidationException.class, () -> receiptService.uploadReceipt(userId, pdfFile));

        verifyNoInteractions(fileStorageService);
        verifyNoInteractions(receiptRepository);
    }

    @Test
    @DisplayName("Should throw exception when user wallet not found")
    void uploadReceipt_WalletNotFound() {
        // GIVEN
        Long userId = 999L;
        MockMultipartFile file = new MockMultipartFile("file", "receipt.png", "image/png", "data".getBytes());

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        // WHEN & THEN
        assertThrows(WalletNotFoundException.class, () -> receiptService.uploadReceipt(userId, file));
    }
}