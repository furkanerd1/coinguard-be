package com.coinguard.receipt.service;

import com.coinguard.common.enums.Currency;
import com.coinguard.common.exception.FileValidationException;
import com.coinguard.common.service.FileStorageService;
import com.coinguard.messaging.producer.NotificationMessageProducer;
import com.coinguard.receipt.dto.ReceiptDto;
import com.coinguard.receipt.dto.ai.ExtractedReceiptData;
import com.coinguard.receipt.entity.Receipt;
import com.coinguard.receipt.enums.ProcessingStatus;
import com.coinguard.receipt.mapper.ReceiptMapper;
import com.coinguard.receipt.repository.ReceiptRepository;
import com.coinguard.transaction.entity.Transaction;
import com.coinguard.transaction.repository.TransactionRepository;
import com.coinguard.wallet.entity.Wallet;
import com.coinguard.wallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceImplTest {

    @InjectMocks
    private ReceiptServiceImpl receiptService;

    @Mock private ReceiptRepository receiptRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private ReceiptMapper receiptMapper;
    @Mock private GeminiOcrService geminiOcrService;
    @Mock private NotificationMessageProducer notificationProducer;

    @Test
    @DisplayName("Upload: Should upload and process receipt successfully via AI")
    void uploadReceipt_Success() {
        // GIVEN
        Long userId = 1L;
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "image data".getBytes());

        Wallet wallet = new Wallet();
        wallet.setId(1L);

        Receipt savedReceipt = new Receipt();
        savedReceipt.setId(100L);
        savedReceipt.setWallet(wallet);

        ExtractedReceiptData aiData = new ExtractedReceiptData("MIGROS", "2026-02-21", BigDecimal.valueOf(150), "GROCERY", 0.95);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(fileStorageService.storeFile(any(), eq("receipts"))).thenReturn("uploads/receipts/uuid.jpg");
        when(receiptRepository.save(any(Receipt.class))).thenReturn(savedReceipt);
        when(geminiOcrService.extractDataFromImage(anyString())).thenReturn(aiData);
        when(receiptMapper.toDto(any())).thenReturn(mock(ReceiptDto.class));

        // WHEN
        ReceiptDto result = receiptService.uploadReceipt(userId, file);

        // THEN
        assertNotNull(result);
        verify(geminiOcrService).extractDataFromImage("uploads/receipts/uuid.jpg");
        verify(receiptRepository, atLeast(2)).save(any(Receipt.class));
        assertEquals(ProcessingStatus.COMPLETED, savedReceipt.getStatus());
        assertEquals(BigDecimal.valueOf(150), savedReceipt.getAmount());
    }

    @Test
    @DisplayName("Upload: Should set status to FAILED when AI processing throws exception")
    void uploadReceipt_AiProcessingFails() {
        // GIVEN
        MockMultipartFile file = new MockMultipartFile("file", "receipt.png", "image/png", "data".getBytes());
        Wallet wallet = new Wallet();
        Receipt savedReceipt = new Receipt();

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(fileStorageService.storeFile(any(), eq("receipts"))).thenReturn("path.jpg");
        when(receiptRepository.save(any(Receipt.class))).thenReturn(savedReceipt);
        when(geminiOcrService.extractDataFromImage(anyString())).thenThrow(new RuntimeException("API Quota Exceeded"));
        doNothing().when(notificationProducer).sendNotificationMessage(any());

        // WHEN
        receiptService.uploadReceipt(1L, file);

        // THEN
        assertEquals(ProcessingStatus.FAILED, savedReceipt.getStatus());
        assertTrue(savedReceipt.getErrorMessage().contains("API Quota Exceeded"));
        verify(notificationProducer, times(2)).sendNotificationMessage(any()); // Processing start + failure
    }

    @Test
    @DisplayName("Upload: Should throw exception when file type is invalid (PDF)")
    void uploadReceipt_InvalidFileType() {
        MockMultipartFile pdfFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", "pdf".getBytes());
        assertThrows(FileValidationException.class, () -> receiptService.uploadReceipt(1L, pdfFile));
    }

    @Test
    @DisplayName("Approve: Should approve receipt, create transaction and deduct balance")
    void approveReceipt_Success() {
        // GIVEN
        Long receiptId = 1L;
        Wallet wallet = new Wallet();
        wallet.setId(10L);
        wallet.setBalance(BigDecimal.valueOf(1000));
        wallet.setCurrency(Currency.TRY);

        Receipt receipt = new Receipt();
        receipt.setId(receiptId);
        receipt.setWallet(wallet);
        receipt.setStatus(ProcessingStatus.COMPLETED);
        receipt.setAmount(BigDecimal.valueOf(300));

        when(receiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());
        when(receiptRepository.save(any(Receipt.class))).thenReturn(receipt);
        when(receiptMapper.toDto(any())).thenReturn(mock(ReceiptDto.class));
        doNothing().when(notificationProducer).sendNotificationMessage(any());

        // WHEN
        receiptService.approveReceipt(1L, receiptId);

        // THEN
        assertEquals(BigDecimal.valueOf(700), wallet.getBalance());
        verify(walletRepository).save(wallet);
        verify(transactionRepository).save(any(Transaction.class));
        verify(notificationProducer).sendNotificationMessage(any());
    }

    @Test
    @DisplayName("Approve: Should throw exception when wallet balance is insufficient")
    void approveReceipt_InsufficientBalance() {
        // GIVEN
        Long receiptId = 1L;
        Wallet wallet = new Wallet();
        wallet.setId(10L);
        wallet.setBalance(BigDecimal.valueOf(100));

        Receipt receipt = new Receipt();
        receipt.setId(receiptId);
        receipt.setWallet(wallet);
        receipt.setStatus(ProcessingStatus.COMPLETED);
        receipt.setAmount(BigDecimal.valueOf(500));

        when(receiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());

        // WHEN & THEN
        Exception exception = assertThrows(RuntimeException.class, () -> {
            receiptService.approveReceipt(1L, receiptId);
        });

        assertTrue(exception.getMessage().contains("Insufficient balance"));
        verify(walletRepository, never()).save(wallet);
    }

    @Test
    @DisplayName("Should return paginated receipts successfully")
    void getUserReceipts_Paginated_Success() {
        // GIVEN
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Receipt receipt = new Receipt();
        receipt.setId(1L);
        receipt.setStatus(ProcessingStatus.COMPLETED);

        Page<Receipt> receiptPage = new PageImpl<>(List.of(receipt), pageable, 1);
        ReceiptDto receiptDto = mock(ReceiptDto.class);

        when(receiptRepository.findByWallet_User_Id(userId, pageable)).thenReturn(receiptPage);
        when(receiptMapper.toDto(receipt)).thenReturn(receiptDto);

        // WHEN
        Page<ReceiptDto> result = receiptService.getUserReceipts(userId, pageable);

        // THEN
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        verify(receiptRepository).findByWallet_User_Id(userId, pageable);
    }

    @Test
    @DisplayName("Should return empty page when user has no receipts")
    void getUserReceipts_Paginated_Empty() {
        // GIVEN
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Receipt> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(receiptRepository.findByWallet_User_Id(userId, pageable)).thenReturn(emptyPage);

        // WHEN
        Page<ReceiptDto> result = receiptService.getUserReceipts(userId, pageable);

        // THEN
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
}

