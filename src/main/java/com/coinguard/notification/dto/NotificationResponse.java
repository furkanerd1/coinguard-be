package com.coinguard.notification.dto;

import com.coinguard.notification.entity.NotificationType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record NotificationResponse(
        Long id,
        String title,
        String message,
        NotificationType type,
        boolean read,
        Instant createdAt
) {
}


