package com.coinguard.messaging.dto;

import lombok.Builder;

import java.io.Serializable;

@Builder
public record NotificationMessage(
        Long userId,
        String title,
        String message,
        String type // INFO, WARNING, ERROR, SUCCESS
) implements Serializable {
}

