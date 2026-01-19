package com.coinguard.wallet.repository;

import com.coinguard.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Query("select w from Wallet w join fetch w.user where w.user.id = :userId")
    Optional<Wallet> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Modifying
    @Query("UPDATE Wallet w SET w.dailySpent = 0, w.lastResetDate = :date")
    void resetAllDailyLimits(LocalDate date);
}
