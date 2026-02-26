package com.coinguard.transaction.repository;

import com.coinguard.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReferenceNo(String referenceNo);

    @Query("SELECT t FROM Transaction t " + "LEFT JOIN t.fromWallet fw " + "LEFT JOIN t.toWallet tw " + "WHERE (fw.user.id = :userId OR tw.user.id = :userId) " + "ORDER BY t.createdAt DESC")
    Page<Transaction> findTransactionsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.fromWallet.user.id = :userId OR t.toWallet.user.id = :userId) AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findTransactionsForStats(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
