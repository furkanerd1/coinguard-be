package com.coinguard.budget.entity;

import com.coinguard.common.entity.BaseEntity;
import com.coinguard.receipt.enums.ReceiptCategory;
import com.coinguard.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets", indexes = {
        @Index(name = "idx_budget_user", columnList = "user_id"),
        @Index(name = "idx_budget_period", columnList = "period_start, period_end")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Budget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReceiptCategory category;

    @Version
    private Long version;

    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "spent_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "alert_threshold")
    @Builder.Default
    private Integer alertThreshold = 80;

    @Column(name = "alert_sent")
    @Builder.Default
    private Boolean alertSent = false;

    /**
     * Calculates how much money is left in the budget.
     * Formula: Limit - Spent
     */
    public BigDecimal getRemainingAmount() {
        return limitAmount.subtract(spentAmount);
    }

    /**
     * Calculates the spending percentage.
     * Useful for UI progress bars.
     * @return percentage value (e.g., 75.5)
     */
    public Double getUsagePercentage() {
        if (limitAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return spentAmount.divide(limitAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    /**
     * Checks if the spending has exceeded the limit.
     */
    public boolean isOverBudget() {
        return spentAmount.compareTo(limitAmount) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Budget)) return false;
        Budget other = (Budget) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}