package com.coinguard.transaction.repository;

import com.coinguard.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceNo(String referenceNo);

    List<Transaction> findByFromWalletIdOrderByCreatedAtDesc(Long walletId);

    List<Transaction> findByToWalletIdOrderByCreatedAtDesc(Long toWalletId);
}
