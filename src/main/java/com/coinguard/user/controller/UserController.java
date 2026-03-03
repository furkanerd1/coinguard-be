package com.coinguard.user.controller;


import com.coinguard.common.constant.RestApiPaths;
import com.coinguard.common.response.ApiResponse;
import com.coinguard.user.dto.request.UpdatePasswordRequest;
import com.coinguard.user.dto.request.UpdateUserRequest;
import com.coinguard.user.dto.response.UserResponse;
import com.coinguard.user.dto.response.UserSummaryResponse;
import com.coinguard.user.entity.User;
import com.coinguard.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(RestApiPaths.User.CTRL)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping(RestApiPaths.User.LOGGED_IN)
    public ResponseEntity<ApiResponse<UserResponse>> getLoggedInUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(userService.getLoggedInUser(user.getId())));
    }

    @PutMapping(RestApiPaths.User.LOGGED_IN)
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(user.getId(), request)));
    }

    @PutMapping(RestApiPaths.User.PASSWORD)
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully"));
    }

    @GetMapping(RestApiPaths.User.SEARCH)
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> searchUsers(
            @RequestParam String query,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.success(userService.searchUsers(query, user.getId())));
    }
}
