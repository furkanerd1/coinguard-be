package com.coinguard.wallet.service;

import com.coinguard.common.exception.WalletNotFoundException;
import com.coinguard.user.entity.User;
import com.coinguard.wallet.dto.response.WalletResponse;

import java.math.BigDecimal;

public interface WalletService {

    /**
     * Get wallet by user ID
     * @param userId User ID
     * @return Wallet response DTO
     * @throws WalletNotFoundException if wallet not found
     */
    WalletResponse getWalletByUserId(Long userId);

    /**
     * Create wallet for user
     * @param user User entity
     * @return Created wallet response DTO
     * @throws IllegalStateException if wallet already exists
     */
    WalletResponse createWalletForUser(User user);


    /**
     * Check if user has sufficient balance
     * @param userId User ID
     * @param amount Amount to check
     * @return true if sufficient balance, false otherwise
     * @throws WalletNotFoundException if wallet not found
     */
    boolean hasSufficientBalance(Long userId, BigDecimal amount);


    /**
     * Reset daily limits for all wallets (scheduled job)
     * Should be called daily at midnight
     */
    void resetDailyLimits();
}
