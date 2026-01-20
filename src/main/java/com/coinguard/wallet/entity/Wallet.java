package com.coinguard.wallet.entity;

import com.coinguard.common.entity.BaseEntity;
import com.coinguard.common.enums.Currency;
import com.coinguard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallet_user", columnList = "user_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Wallet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    @Builder.Default
    private Currency currency = Currency.TRY;

    // CRITICAL: Optimistic Locking for Race Condition protection
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "is_frozen")
    @Builder.Default
    private boolean isFrozen = false;

    @Column(name = "daily_limit", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal dailyLimit = new BigDecimal("10000.00");

    @Column(name = "daily_spent", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal dailySpent = BigDecimal.ZERO;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate;

    /**
     * Deducts amount from balance.
     * Throws exception if balance is insufficient or amount is invalid.
     */
    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Adds amount to wallet balance.
     */
    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    /**
     * Checks if balance is enough for the transaction.
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wallet)) return false;
        Wallet other = (Wallet) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
