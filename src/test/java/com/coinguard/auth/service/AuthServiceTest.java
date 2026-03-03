package com.coinguard.auth.service;

import com.coinguard.auth.dto.request.LoginRequest;
import com.coinguard.auth.dto.request.RefreshTokenRequest;
import com.coinguard.auth.dto.request.RegisterRequest;
import com.coinguard.auth.dto.response.AuthResponse;
import com.coinguard.common.exception.AuthorizationException;
import com.coinguard.common.exception.InvalidTokenException;
import com.coinguard.security.entity.RefreshToken;
import com.coinguard.security.service.JwtService;
import com.coinguard.security.service.RefreshTokenService;
import com.coinguard.user.entity.User;
import com.coinguard.user.repository.UserRepository;
import com.coinguard.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private WalletService walletService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_ShouldReturnAuthResponse_WhenUserIsValid() {
        // Given
        RegisterRequest request = new RegisterRequest("Ahmet Yılmaz", "ahmet123", "ahmet@mail.com", "123456", "+905551112233");
        User savedUser = User.builder().id(1L).email(request.email()).build();
        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token-uuid")
                .user(savedUser)
                .expiryDate(Instant.now().plusSeconds(604800))
                .build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("mock-access-token");
        when(refreshTokenService.createRefreshToken(savedUser)).thenReturn(refreshToken);

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertNotNull(response);
        assertEquals("mock-access-token", response.token());
        assertEquals("refresh-token-uuid", response.refreshToken());
        assertEquals("User registered successfully", response.message());

        verify(userRepository, times(1)).save(any(User.class));
        verify(walletService, times(1)).createWalletForUser(savedUser);
        verify(refreshTokenService, times(1)).createRefreshToken(savedUser);
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        // Given
        RegisterRequest request = new RegisterRequest("Ahmet", "user", "varolan@mail.com", "123", "555");

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // When & Then
        assertThrows(AuthorizationException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
        verify(walletService, never()).createWalletForUser(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreCorrect() {
        // Given
        LoginRequest request = new LoginRequest("ahmet@mail.com", "123456");
        User mockUser = User.builder().id(1L).email("ahmet@mail.com").build();
        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token-uuid")
                .user(mockUser)
                .expiryDate(Instant.now().plusSeconds(604800))
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser)).thenReturn("login-token");
        when(refreshTokenService.createRefreshToken(mockUser)).thenReturn(refreshToken);

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("login-token", response.token());
        assertEquals("refresh-token-uuid", response.refreshToken());
        assertEquals("Login successful", response.message());

        verify(refreshTokenService, times(1)).createRefreshToken(mockUser);
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFound() {
        // Given
        LoginRequest request = new LoginRequest("notfound@mail.com", "123456");

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AuthorizationException.class, () -> authService.login(request));

        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void refresh_ShouldReturnNewTokens_WhenRefreshTokenIsValid() {
        // Given
        String oldRefreshToken = "old-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(oldRefreshToken);

        User mockUser = User.builder().id(1L).email("test@mail.com").build();
        RefreshToken validRefreshToken = RefreshToken.builder()
                .token(oldRefreshToken)
                .user(mockUser)
                .expiryDate(Instant.now().plusSeconds(604800))
                .isRevoked(false)
                .build();

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token("new-refresh-token")
                .user(mockUser)
                .expiryDate(Instant.now().plusSeconds(604800))
                .build();

        when(refreshTokenService.verifyRefreshToken(oldRefreshToken)).thenReturn(validRefreshToken);
        when(jwtService.generateToken(mockUser)).thenReturn("new-access-token");
        when(refreshTokenService.createRefreshToken(mockUser)).thenReturn(newRefreshToken);

        // When
        AuthResponse response = authService.refresh(request);

        // Then
        assertNotNull(response);
        assertEquals("new-access-token", response.token());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals("Token refreshed successfully", response.message());

        verify(refreshTokenService, times(1)).verifyRefreshToken(oldRefreshToken);
        verify(refreshTokenService, times(1)).createRefreshToken(mockUser);
    }

    @Test
    void refresh_ShouldThrowException_WhenRefreshTokenIsInvalid() {
        // Given
        String invalidToken = "invalid-token";
        RefreshTokenRequest request = new RefreshTokenRequest(invalidToken);

        when(refreshTokenService.verifyRefreshToken(invalidToken))
                .thenThrow(new InvalidTokenException("Invalid refresh token"));

        // When & Then
        assertThrows(InvalidTokenException.class, () -> authService.refresh(request));

        verify(jwtService, never()).generateToken(any());
    }
}

