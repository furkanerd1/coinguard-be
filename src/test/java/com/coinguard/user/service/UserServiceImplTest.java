package com.coinguard.user.service;

import com.coinguard.common.exception.UserNotFoundException;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;


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
}