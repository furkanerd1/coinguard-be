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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private NotificationMessage testMessage;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        testMessage = new NotificationMessage(
                1L,
                "Test Title",
                "Test Message",
                "INFO"
        );
    }

    @Test
    @DisplayName("Should save notification successfully")
    void shouldSaveNotificationSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        notificationService.saveNotification(testMessage);

        // Then
        verify(userRepository).findById(1L);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found for save notification")
    void shouldThrowExceptionWhenUserNotFoundForSaveNotification() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.saveNotification(testMessage))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found: 1");

        verify(userRepository).findById(1L);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get user notifications successfully")
    void shouldGetUserNotificationsSuccessfully() {
        // Given
        Notification notification = Notification.builder()
                .id(1L)
                .user(testUser)
                .title("Test Title")
                .message("Test Message")
                .type(NotificationType.INFO)
                .read(false)
                .createdAt(Instant.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> notificationPage = new PageImpl<>(List.of(notification), pageable, 1);
        PagedNotificationResponse expectedResponse = new PagedNotificationResponse(
                List.of(),
                0,
                1,
                1L,
                10,
                false,
                false
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.findAllByUserOrderByCreatedAtDesc(testUser, pageable))
                .thenReturn(notificationPage);
        when(notificationMapper.toPagedResponse(notificationPage)).thenReturn(expectedResponse);

        // When
        PagedNotificationResponse response = notificationService.getUserNotifications(1L, pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response).isEqualTo(expectedResponse);
        verify(userRepository).findById(1L);
        verify(notificationRepository).findAllByUserOrderByCreatedAtDesc(testUser, pageable);
        verify(notificationMapper).toPagedResponse(notificationPage);
    }

    @Test
    @DisplayName("Should throw exception when user not found for get notifications")
    void shouldThrowExceptionWhenUserNotFoundForGetNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.getUserNotifications(1L, pageable))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found: 1");

        verify(userRepository).findById(1L);
        verify(notificationRepository, never()).findAllByUserOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("Should mark all notifications as read successfully")
    void shouldMarkAllNotificationsAsReadSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(notificationRepository).markAllAsReadByUser(testUser);

        // When
        notificationService.markAllAsRead(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(notificationRepository).markAllAsReadByUser(testUser);
    }

    @Test
    @DisplayName("Should throw exception when user not found for mark all as read")
    void shouldThrowExceptionWhenUserNotFoundForMarkAllAsRead() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.markAllAsRead(1L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found: 1");

        verify(userRepository).findById(1L);
        verify(notificationRepository, never()).markAllAsReadByUser(any());
    }
}



