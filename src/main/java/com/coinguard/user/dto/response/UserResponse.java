package com.coinguard.user.dto.response;

import com.coinguard.user.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String username,
    String email,
    String fullName,
    String phoneNumber,
    boolean isActive,
    boolean isEmailVerified,
    UserRole role,
    LocalDateTime createdAt
){}
