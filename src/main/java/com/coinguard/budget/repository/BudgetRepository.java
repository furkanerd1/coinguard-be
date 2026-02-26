package com.coinguard.budget.repository;

import com.coinguard.budget.entity.Budget;
import com.coinguard.common.enums.TransactionCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findAllByUserId(Long userId);

    Page<Budget> findAllByUserId(Long userId, Pageable pageable);

    Optional<Budget> findByUserIdAndCategoryAndIsActiveTrue(Long userId, TransactionCategory category);

    List<Budget> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.category = :category AND b.isActive = true AND :transactionDate BETWEEN b.periodStart AND b.periodEnd")
    Optional<Budget> findActiveBudgetByCategory(@Param("userId") Long userId, @Param("category") TransactionCategory category, @Param("transactionDate") LocalDate transactionDate);

    @Query("SELECT COUNT(b) > 0 FROM Budget b WHERE b.user.id = :userId AND b.category = :category AND b.isActive = true " +
            "AND ((:start BETWEEN b.periodStart AND b.periodEnd) OR (:end BETWEEN b.periodStart AND b.periodEnd) OR (b.periodStart BETWEEN :start AND :end))")
    boolean existsActiveBudgetOverlap(@Param("userId") Long userId, @Param("category") TransactionCategory category, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
