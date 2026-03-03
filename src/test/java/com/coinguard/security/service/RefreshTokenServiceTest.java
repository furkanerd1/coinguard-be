package com.coinguard.security.service;

import com.coinguard.common.exception.InvalidTokenException;
import com.coinguard.security.entity.RefreshToken;
import com.coinguard.security.repository.RefreshTokenRepository;
import com.coinguard.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("Should create refresh token successfully")
    void createRefreshToken_Success() {
        // GIVEN
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 604800000L); // 7 days

        User user = User.builder().id(1L).email("test@mail.com").build();
        RefreshToken savedToken = RefreshToken.builder()
                .id(1L)
                .user(user)
                .token("generated-uuid-token")
                .expiryDate(Instant.now().plusMillis(604800000L))
                .isRevoked(false)
                .build();

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

        // WHEN
        RefreshToken result = refreshTokenService.createRefreshToken(user);

        // THEN
        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertFalse(result.isRevoked());

        verify(refreshTokenRepository).revokeAllByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should revoke all existing tokens before creating new one")
    void createRefreshToken_ShouldRevokeExistingTokens() {
        // GIVEN
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 604800000L);

        User user = User.builder().id(1L).email("test@mail.com").build();
        RefreshToken savedToken = RefreshToken.builder()
                .user(user)
                .token("new-token")
                .expiryDate(Instant.now().plusMillis(604800000L))
                .build();

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

        // WHEN
        refreshTokenService.createRefreshToken(user);

        // THEN
        verify(refreshTokenRepository, times(1)).revokeAllByUser(user);
    }

    @Test
    @DisplayName("Should verify valid refresh token successfully")
    void verifyRefreshToken_Success() {
        // GIVEN
        String tokenString = "valid-token";
        User user = User.builder().id(1L).email("test@mail.com").build();
        RefreshToken validToken = RefreshToken.builder()
                .token(tokenString)
                .user(user)
                .expiryDate(Instant.now().plusSeconds(3600))
                .isRevoked(false)
                .build();

        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(validToken));

        // WHEN
        RefreshToken result = refreshTokenService.verifyRefreshToken(tokenString);

        // THEN
        assertNotNull(result);
        assertEquals(tokenString, result.getToken());
        assertFalse(result.isRevoked());
        assertFalse(result.isExpired());
    }

    @Test
    @DisplayName("Should throw exception when token not found")
    void verifyRefreshToken_TokenNotFound() {
        // GIVEN
        String tokenString = "non-existent-token";
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

        // WHEN & THEN
        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> refreshTokenService.verifyRefreshToken(tokenString)
        );

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when token is revoked")
    void verifyRefreshToken_TokenRevoked() {
        // GIVEN
        String tokenString = "revoked-token";
        RefreshToken revokedToken = RefreshToken.builder()
                .token(tokenString)
                .expiryDate(Instant.now().plusSeconds(3600))
                .isRevoked(true)
                .build();

        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(revokedToken));

        // WHEN & THEN
        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> refreshTokenService.verifyRefreshToken(tokenString)
        );

        assertEquals("Refresh token has been revoked", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when token is expired")
    void verifyRefreshToken_TokenExpired() {
        // GIVEN
        String tokenString = "expired-token";
        RefreshToken expiredToken = RefreshToken.builder()
                .token(tokenString)
                .expiryDate(Instant.now().minusSeconds(3600))
                .isRevoked(false)
                .build();

        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(expiredToken));

        // WHEN & THEN
        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> refreshTokenService.verifyRefreshToken(tokenString)
        );

        assertEquals("Refresh token has expired", exception.getMessage());
    }

    @Test
    @DisplayName("Should revoke refresh token successfully")
    void revokeRefreshToken_Success() {
        // GIVEN
        String tokenString = "token-to-revoke";
        RefreshToken token = RefreshToken.builder()
                .token(tokenString)
                .isRevoked(false)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(token);

        // WHEN
        refreshTokenService.revokeRefreshToken(tokenString);

        // THEN
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertTrue(savedToken.isRevoked());
    }

    @Test
    @DisplayName("Should throw exception when revoking non-existent token")
    void revokeRefreshToken_TokenNotFound() {
        // GIVEN
        String tokenString = "non-existent-token";
        when(refreshTokenRepository.findByToken(tokenString)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(InvalidTokenException.class, () -> refreshTokenService.revokeRefreshToken(tokenString));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should cleanup expired tokens")
    void cleanupExpiredTokens_Success() {
        // WHEN
        refreshTokenService.cleanupExpiredTokens();

        // THEN
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).deleteAllExpiredTokens(instantCaptor.capture());

        Instant capturedInstant = instantCaptor.getValue();
        assertNotNull(capturedInstant);
        assertTrue(capturedInstant.isBefore(Instant.now().plusSeconds(1)));
    }
}

