package com.coinguard.auth.service;

import com.coinguard.auth.dto.request.LoginRequest;
import com.coinguard.auth.dto.request.RefreshTokenRequest;
import com.coinguard.auth.dto.request.RegisterRequest;
import com.coinguard.auth.dto.response.AuthResponse;
import com.coinguard.common.exception.AuthorizationException;
import com.coinguard.security.entity.RefreshToken;
import com.coinguard.security.service.JwtService;
import com.coinguard.security.service.RefreshTokenService;
import com.coinguard.user.entity.User;
import com.coinguard.user.enums.UserRole;
import com.coinguard.user.repository.UserRepository;
import com.coinguard.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final WalletService walletService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthorizationException("Email already in use");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .username(request.username())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .isActive(true)
                .isEmailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        walletService.createWalletForUser(savedUser);

        String token = jwtService.generateToken(savedUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return new AuthResponse(token, refreshToken.getToken(), "User registered successfully");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthorizationException("User not found"));

        String token = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(token, refreshToken.getToken(), "Login successful");
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.refreshToken());
        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(newAccessToken, newRefreshToken.getToken(), "Token refreshed successfully");
    }
}