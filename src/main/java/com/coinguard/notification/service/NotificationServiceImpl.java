package com.coinguard.notification.service;

import com.coinguard.common.exception.UserNotFoundException;
import com.coinguard.messaging.dto.NotificationMessage;
import com.coinguard.notification.dto.PagedNotificationResponse;
import com.coinguard.notification.entity.Notification;
import com.coinguard.notification.entity.NotificationType;
import com.coinguard.notification.mapper.NotificationMapper;
import com.coinguard.notification.repository.NotificationRepository;
import com.coinguard.user.entity.User;
import com.coinguard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public void saveNotification(NotificationMessage message) {
        log.info("Saving notification to database for user: {}", message.userId());

        User user = userRepository.findById(message.userId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + message.userId()));

        Notification notification = Notification.builder()
                .user(user)
                .title(message.title())
                .message(message.message())
                .type(NotificationType.valueOf(message.type()))
                .build();

        notificationRepository.save(notification);
        log.info("Notification successfully saved to database.");
    }

    @Override
    @Transactional(readOnly = true)
    public PagedNotificationResponse getUserNotifications(Long userId, Pageable pageable) {
        log.info("Fetching notifications for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        Page<Notification> notificationPage = notificationRepository
                .findAllByUserOrderByCreatedAtDesc(user, pageable);

        return notificationMapper.toPagedResponse(notificationPage);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        log.info("Marking all notifications as read for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        notificationRepository.markAllAsReadByUser(user);
        log.info("All notifications marked as read for user: {}", userId);
    }
}