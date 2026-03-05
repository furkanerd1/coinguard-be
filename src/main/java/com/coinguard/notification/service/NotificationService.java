package com.coinguard.notification.service;

import com.coinguard.messaging.dto.NotificationMessage;
import com.coinguard.notification.dto.PagedNotificationResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    void saveNotification(NotificationMessage message);
    PagedNotificationResponse getUserNotifications(Long userId, Pageable pageable);
    void markAllAsRead(Long userId);
}
