package com.coinguard.auth.service;

import com.coinguard.auth.dto.request.*;
import com.coinguard.auth.dto.response.AuthResponse;
import com.coinguard.common.exception.AuthorizationException;
import com.coinguard.common.exception.InvalidTokenException;
import com.coinguard.common.exception.UserNotFoundException;
import com.coinguard.common.service.EmailService;
import com.coinguard.security.entity.PasswordResetToken;
import com.coinguard.security.entity.RefreshToken;
import com.coinguard.security.repository.PasswordResetTokenRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final WalletService walletService;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

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

        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName());

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

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Security: Don't reveal if email exists (prevent email enumeration attack)
        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null) {
            // Log for security monitoring but don't throw exception
            return;
        }

        // delete any existing tokens for this user to prevent multiple valid tokens
        passwordResetTokenRepository.deleteByUser(user);

        // generate a unique token and save it with an expiry time
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        passwordResetTokenRepository.save(resetToken);

        // send mail
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid or non-existent password reset token"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new InvalidTokenException("Password reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // delete the token after successful password reset to prevent reuse
        passwordResetTokenRepository.delete(resetToken);
    }

}