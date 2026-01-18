package com.coinguard.receipt.entity;

import com.coinguard.common.entity.BaseEntity;
import com.coinguard.common.enums.Currency;
import com.coinguard.receipt.enums.ProcessingStatus;
import com.coinguard.receipt.enums.ReceiptCategory;
import com.coinguard.transaction.entity.Transaction;
import com.coinguard.wallet.entity.Wallet;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "receipts", indexes = {
        @Index(name = "idx_receipt_wallet", columnList = "wallet_id"),
        @Index(name = "idx_receipt_status", columnList = "status"),
        @Index(name = "idx_receipt_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;  //  Fiş işlendiğinde oluşan transaction

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;  // MinIO'daki dosya yolu

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;  // bytes

    // OCR sonuçları (Gemini API'den gelecek)
    @Column(name = "merchant_name", length = 255)
    private String merchantName;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Currency currency=Currency.TRY;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ReceiptCategory category;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "gemini_confidence")
    private Double geminiConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessingStatus status = ProcessingStatus.UPLOADED;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Receipt)) return false;
        Receipt other = (Receipt) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
