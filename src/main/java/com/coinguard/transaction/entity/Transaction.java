package com.coinguard.transaction.entity;

import com.coinguard.common.entity.BaseEntity;
import com.coinguard.common.enums.Currency;
import com.coinguard.transaction.enums.TransactionStatus;
import com.coinguard.transaction.enums.TransactionType;
import com.coinguard.wallet.entity.Wallet;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_from_wallet", columnList = "from_wallet_id"),
        @Index(name = "idx_to_wallet", columnList = "to_wallet_id"),
        @Index(name = "idx_reference_no", columnList = "reference_no"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 19, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(length = 500)
    private String description;

    @Column(name = "reference_no", nullable = false, unique = true, length = 36)
    @EqualsAndHashCode.Include
    private String referenceNo;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    // CRITICAL: Currency must match wallet's currency
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency = Currency.TRY;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Generates a unique reference number before saving to DB.
     * Ensures idempotency and tracking.
     */
    @PrePersist
    public void prePersist() {
        if (this.referenceNo == null) {
            this.referenceNo = UUID.randomUUID().toString();
        }
    }
}