package com.coinguard.user.dto.response;

public record UserSummaryResponse(
        Long id,
        String username,
        String fullName,
        String email
){}
