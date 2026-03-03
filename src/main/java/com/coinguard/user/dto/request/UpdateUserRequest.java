package com.coinguard.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName,

        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Pattern(regexp = "^(\\+\\d{1,3}[- ]?)?\\d{10,15}$", message = "Invalid phone number format")
        String phoneNumber
) {}
