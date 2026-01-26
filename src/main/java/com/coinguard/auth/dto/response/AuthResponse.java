package com.coinguard.auth.dto.response;

public record AuthResponse(
        String token,
        String message
){}
