package com.coinguard.user.service;

import com.coinguard.user.dto.request.UpdatePasswordRequest;
import com.coinguard.user.dto.request.UpdateUserRequest;
import com.coinguard.user.dto.response.UserResponse;
import com.coinguard.user.dto.response.UserSummaryResponse;

import java.util.List;

public interface UserService {

    UserResponse getLoggedInUser(Long userId);

    List<UserSummaryResponse> searchUsers(String query, Long currentUserId);

    UserResponse updateUser(Long userId, UpdateUserRequest request);

    void updatePassword(Long userId, UpdatePasswordRequest request);
}
