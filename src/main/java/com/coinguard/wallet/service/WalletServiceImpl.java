package com.coinguard.wallet.service;

import com.coinguard.common.enums.Currency;
import com.coinguard.common.exception.WalletNotFoundException;
import com.coinguard.messaging.dto.NotificationMessage;
import com.coinguard.messaging.producer.NotificationMessageProducer;
import com.coinguard.user.entity.User;
import com.coinguard.wallet.dto.response.WalletResponse;
import com.coinguard.wallet.entity.Wallet;
import com.coinguard.wallet.mapper.WalletMapper;
import com.coinguard.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService{

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;
    private final NotificationMessageProducer notificationProducer;

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(Long userId){
        log.debug("Fetching wallet for user ID: {}", userId);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
        return walletMapper.toWalletResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse createWalletForUser(User user) {
        log.info("Creating wallet for user: {}", user.getUsername());

        if (walletRepository.existsByUserId(user.getId())) {
            log.warn("Wallet already exists for user ID: {}", user.getId());
            throw new IllegalStateException("Wallet already exists for user: " + user.getId());
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .currency(Currency.TRY)
                .isFrozen(false)
                .dailyLimit(new BigDecimal("10000.00"))
                .dailySpent(BigDecimal.ZERO)
                .lastResetDate(LocalDate.now())
                .build();
        return walletMapper.toWalletResponse(walletRepository.save(wallet));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSufficientBalance(Long userId, BigDecimal amount) {
        log.debug("Checking balance for user ID: {}, amount: {}", userId, amount);

        Wallet existingWallet =walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
        return existingWallet.hasSufficientBalance(amount);
    }

    @Override
    @Transactional
    public void resetDailyLimits() {
        log.info("Starting daily limit reset for all wallets");
        walletRepository.resetAllDailyLimits(LocalDate.now());

        // Send notification to all users about daily limits reset
        List<Wallet> allWallets = walletRepository.findAll();
        for (Wallet wallet : allWallets) {
            notificationProducer.sendNotificationMessage(new NotificationMessage(
                    wallet.getUser().getId(),
                    "Daily Limits Reset",
                    "Your daily transaction limits have been reset",
                    "INFO"
            ));
        }
    }
}
