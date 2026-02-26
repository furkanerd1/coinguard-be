package com.coinguard.auth.service;

import com.coinguard.auth.dto.request.LoginRequest;
import com.coinguard.auth.dto.request.RegisterRequest;
import com.coinguard.auth.dto.response.AuthResponse;
import com.coinguard.common.exception.AuthorizationException;
import com.coinguard.security.service.JwtService;
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
    private AuthenticationManager authenticationManager;
    @Mock
    private WalletService walletService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_ShouldReturnToken_WhenUserIsValid() {
        // Given
        RegisterRequest request = new RegisterRequest("Ahmet Yılmaz", "ahmet123", "ahmet@mail.com", "123456", "+905551112233");
        User savedUser = User.builder().email(request.email()).build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(jwtService.generateToken(savedUser)).thenReturn("mock-token");

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertNotNull(response);
        assertEquals("mock-token", response.token());

        verify(userRepository, times(1)).save(any(User.class));
        verify(walletService, times(1)).createWalletForUser(savedUser);
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
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreCorrect() {
        // Given
        LoginRequest request = new LoginRequest("ahmet@mail.com", "123456");
        User mockUser = User.builder().email("ahmet@mail.com").build();

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser)).thenReturn("login-token");

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("login-token", response.token());
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFound() {
        // Given
        LoginRequest request = new LoginRequest("yok@mail.com", "123456");

        // Mock the authentication manager to pass, but the repository to return empty
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AuthorizationException.class, () -> authService.login(request));
    }
}