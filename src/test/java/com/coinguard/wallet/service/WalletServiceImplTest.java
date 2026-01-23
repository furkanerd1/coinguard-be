package com.coinguard.wallet.service;

import com.coinguard.common.exception.WalletNotFoundException;
import com.coinguard.user.entity.User;
import com.coinguard.wallet.dto.response.WalletResponse;
import com.coinguard.wallet.entity.Wallet;
import com.coinguard.wallet.mapper.WalletMapper;
import com.coinguard.wallet.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @InjectMocks
    private WalletServiceImpl walletService;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMapper walletMapper;

    //Helper Method
    private User createDummyUser() {
        return User.builder()
                .id(1L)
                .username("testuser")
                .fullName("Test User")
                .build();
    }

    private Wallet createDummyWallet(User user) {
        return Wallet.builder()
                .id(100L)
                .user(user)
                .balance(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("Should return wallet response when wallet exists")
    void shouldGetWalletByUserId_Success() {
        // GIVEN
        Long userId = 1L;
        User user = createDummyUser();
        Wallet wallet = createDummyWallet(user);
        WalletResponse expectedResponse = new WalletResponse(100L, 1L, "Test User", BigDecimal.ZERO, null, null, null, null, false);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletMapper.toWalletResponse(wallet)).thenReturn(expectedResponse);

        // WHEN
        WalletResponse result = walletService.getWalletByUserId(userId);

        // THEN
        assertNotNull(result);
        assertEquals(expectedResponse.id(), result.id());
        verify(walletRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when wallet does not exist")
    void shouldThrowException_WhenWalletNotFound() {
        // GIVEN
        Long userId = 99L;
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(WalletNotFoundException.class, () -> walletService.getWalletByUserId(userId));

        verify(walletMapper, never()).toWalletResponse(any());
    }


    @Test
    @DisplayName("Should create wallet successfully when it does not exist")
    void shouldCreateWalletForUser_Success() {
        // GIVEN
        User user = createDummyUser();
        Wallet wallet = createDummyWallet(user);
        WalletResponse expectedResponse = new WalletResponse(100L, 1L, "Test User", BigDecimal.ZERO, null, null, null, null, false);

        when(walletRepository.existsByUserId(user.getId())).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet); // save ne dönerse onu al
        when(walletMapper.toWalletResponse(wallet)).thenReturn(expectedResponse);

        // WHEN
        WalletResponse result = walletService.createWalletForUser(user);

        // THEN
        assertNotNull(result);
        verify(walletRepository).existsByUserId(user.getId());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when wallet already exists")
    void shouldThrowException_WhenWalletAlreadyExists() {
        // GIVEN
        User user = createDummyUser();
        when(walletRepository.existsByUserId(user.getId())).thenReturn(true);

        // WHEN & THEN
        assertThrows(IllegalStateException.class, () -> walletService.createWalletForUser(user));

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should return true when balance is sufficient")
    void shouldReturnTrue_WhenBalanceIsSufficient() {
        // GIVEN
        Long userId = 1L;
        Wallet wallet = createDummyWallet(createDummyUser());
        wallet.setBalance(new BigDecimal("100.00")); // 100 TL var

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        // WHEN
        boolean result = walletService.hasSufficientBalance(userId, new BigDecimal("50.00"));

        // THEN
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when balance is insufficient")
    void shouldReturnFalse_WhenBalanceIsInsufficient() {
        // GIVEN
        Long userId = 1L;
        Wallet wallet = createDummyWallet(createDummyUser());
        wallet.setBalance(new BigDecimal("20.00")); // 20 TL var

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        // WHEN
        boolean result = walletService.hasSufficientBalance(userId, new BigDecimal("50.00"));

        // THEN
        assertFalse(result);
    }

    @Test
    @DisplayName("Should call repository to reset daily limits")
    void shouldResetDailyLimits() {
        // WHEN
        walletService.resetDailyLimits();
        verify(walletRepository).resetAllDailyLimits(any(LocalDate.class));
    }
}