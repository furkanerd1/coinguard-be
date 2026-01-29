package com.coinguard.transaction.repository;

import com.coinguard.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceNo(String referenceNo);

    List<Transaction> findByFromWalletIdOrderByCreatedAtDesc(Long walletId);

    List<Transaction> findByToWalletIdOrderByCreatedAtDesc(Long toWalletId);

    @Query("SELECT t FROM Transaction t " + "LEFT JOIN t.fromWallet fw " + "LEFT JOIN t.toWallet tw " + "WHERE (fw.user.id = :userId OR tw.user.id = :userId) " + "ORDER BY t.createdAt DESC")
    Page<Transaction> findTransactionsByUserId(@Param("userId") Long userId, Pageable pageable);
}
