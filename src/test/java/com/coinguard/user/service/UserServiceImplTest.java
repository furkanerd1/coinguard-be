package com.coinguard.user.service;

import com.coinguard.common.exception.InvalidPasswordException;
import com.coinguard.common.exception.UserNotFoundException;
import com.coinguard.messaging.producer.EmailMessageProducer;
import com.coinguard.messaging.producer.NotificationMessageProducer;
import com.coinguard.user.dto.request.UpdatePasswordRequest;
import com.coinguard.user.dto.request.UpdateUserRequest;
import com.coinguard.user.dto.response.UserResponse;
import com.coinguard.user.dto.response.UserSummaryResponse;
import com.coinguard.user.entity.User;
import com.coinguard.user.enums.UserRole;
import com.coinguard.user.mapper.UserMapper;
import com.coinguard.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationMessageProducer notificationProducer;

    @Mock
    private EmailMessageProducer emailProducer;


    @Test
    @DisplayName("Should return user profile when user exists")
    void getLoggedInUser_Success() {
        // GIVEN
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .username("mario")
                .email("mario@test.com")
                .role(UserRole.USER)
                .build();

        UserResponse expectedResponse = new UserResponse(
                userId, "mario", "mario@test.com", "Mario Lemina", "+90555", true, true, UserRole.USER, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(expectedResponse);

        // WHEN
        UserResponse actualResponse = userService.getLoggedInUser(userId);

        // THEN
        assertNotNull(actualResponse);
        assertEquals("mario", actualResponse.username());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void getLoggedInUser_NotFound() {
        // GIVEN
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(UserNotFoundException.class, () -> userService.getLoggedInUser(userId));
    }


    @Test
    @DisplayName("Should return user summaries when query matches")
    void searchUsers_Success() {
        // GIVEN
        String query = "Mario";
        Long currentUserId = 1L;

        User user2 = User.builder().id(2L).username("mario_gomez").fullName("Mario Gomez").build();
        UserSummaryResponse summary = new UserSummaryResponse(2L, "mario_gomez", "Mario Gomez", "gomez@test.com");

        when(userRepository.searchUsers(query, currentUserId)).thenReturn(List.of(user2));
        when(userMapper.toUserSummaryResponse(user2)).thenReturn(summary);

        // WHEN
        List<UserSummaryResponse> results = userService.searchUsers(query, currentUserId);

        // THEN
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals("Mario Gomez", results.get(0).fullName());

        verify(userRepository).searchUsers(query, currentUserId);
    }

    @Test
    @DisplayName("Should return empty list when no match found")
    void searchUsers_NoMatch() {
        // GIVEN
        String query = "NonExistent";
        Long currentUserId = 1L;

        when(userRepository.searchUsers(query, currentUserId)).thenReturn(List.of());

        // WHEN
        List<UserSummaryResponse> results = userService.searchUsers(query, currentUserId);

        // THEN
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when query is too short")
    void searchUsers_ShortQuery() {
        // GIVEN
        String shortQuery = "ab";
        Long currentUserId = 1L;

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class, () -> userService.searchUsers(shortQuery, currentUserId));

        verify(userRepository, never()).searchUsers(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should trim query string before searching")
    void searchUsers_ShouldTrimQuery() {
        // GIVEN
        String messyQuery = "  Mario  ";
        Long currentUserId = 1L;

        // WHEN
        userService.searchUsers(messyQuery, currentUserId);

        // THEN
        verify(userRepository).searchUsers("Mario", currentUserId);
    }

    @Test
    @DisplayName("Should update user profile successfully")
    void updateUser_Success() {
        // GIVEN
        Long userId = 1L;
        UpdateUserRequest request = new UpdateUserRequest("New Name", "newusername", "+905559998877");

        User existingUser = User.builder()
                .id(userId)
                .username("oldusername")
                .fullName("Old Name")
                .phoneNumber("+905551234567")
                .build();

        User updatedUser = User.builder()
                .id(userId)
                .username("newusername")
                .fullName("New Name")
                .phoneNumber("+905559998877")
                .build();

        UserResponse expectedResponse = new UserResponse(
                userId, "newusername", "test@mail.com", "New Name", "+905559998877", true, true, UserRole.USER, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toUserResponse(updatedUser)).thenReturn(expectedResponse);

        // WHEN
        UserResponse response = userService.updateUser(userId, request);

        // THEN
        assertNotNull(response);
        assertEquals("newusername", response.username());
        assertEquals("New Name", response.fullName());
        assertEquals("+905559998877", response.phoneNumber());

        verify(userRepository).save(existingUser);
        verify(userMapper).toUserResponse(updatedUser);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void updateUser_UserNotFound() {
        // GIVEN
        Long userId = 99L;
        UpdateUserRequest request = new UpdateUserRequest("New Name", "newuser", "+905559998877");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(UserNotFoundException.class, () -> userService.updateUser(userId, request));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update password successfully")
    void updatePassword_Success() {
        // GIVEN
        Long userId = 1L;
        UpdatePasswordRequest request = new UpdatePasswordRequest("oldPassword123", "newPassword456");

        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .fullName("Test User")
                .password("encodedOldPassword")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword123", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword456")).thenReturn("encodedNewPassword");
        doNothing().when(notificationProducer).sendNotificationMessage(any());
        doNothing().when(emailProducer).sendEmailMessage(any());

        // WHEN
        userService.updatePassword(userId, request);

        // THEN
        verify(userRepository).save(user);
        verify(passwordEncoder).encode("newPassword456");
        verify(notificationProducer).sendNotificationMessage(any());
        verify(emailProducer).sendEmailMessage(any());
    }

    @Test
    @DisplayName("Should throw exception when current password is incorrect")
    void updatePassword_IncorrectCurrentPassword() {
        // GIVEN
        Long userId = 1L;
        UpdatePasswordRequest request = new UpdatePasswordRequest("wrongPassword", "newPassword456");

        User user = User.builder()
                .id(userId)
                .password("encodedOldPassword")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedOldPassword")).thenReturn(false);

        // WHEN & THEN
        assertThrows(InvalidPasswordException.class, () -> userService.updatePassword(userId, request));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when new password is same as current")
    void updatePassword_SamePassword() {
        // GIVEN
        Long userId = 1L;
        UpdatePasswordRequest request = new UpdatePasswordRequest("password123", "password123");

        User user = User.builder()
                .id(userId)
                .password("encodedPassword")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // WHEN & THEN
        assertThrows(InvalidPasswordException.class, () -> userService.updatePassword(userId, request));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found during password update")
    void updatePassword_UserNotFound() {
        // GIVEN
        Long userId = 99L;
        UpdatePasswordRequest request = new UpdatePasswordRequest("oldPassword", "newPassword");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThrows(UserNotFoundException.class, () -> userService.updatePassword(userId, request));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any());
    }
}

