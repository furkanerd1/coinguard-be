package com.coinguard.notification.dto;

import java.util.List;

public record PagedNotificationResponse(
        List<NotificationResponse> notifications,
        int currentPage,
        int totalPages,
        long totalElements,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious
) {
}


