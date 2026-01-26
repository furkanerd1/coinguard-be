package com.coinguard.budget.repository;

import com.coinguard.budget.entity.Budget;
import com.coinguard.receipt.enums.ReceiptCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findAllByUserId(Long userId);

    Optional<Budget> findByUserIdAndCategoryAndIsActiveTrue(Long userId, ReceiptCategory category);

    List<Budget> findByUserIdAndIsActiveTrue(Long userId);
}
