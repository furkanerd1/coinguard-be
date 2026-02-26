package com.coinguard.receipt.repository;

import com.coinguard.receipt.entity.Receipt;
import com.coinguard.receipt.enums.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    List<Receipt> findByWalletIdOrderByCreatedAtDesc(Long toWalletId);

    List<Receipt> findByStatus(ProcessingStatus status);

    List<Receipt> findByWallet_User_Id(Long userId);

    Page<Receipt> findByWallet_User_Id(Long userId, Pageable pageable);
}

