package com.coinguard.user.service;

import com.coinguard.common.exception.UserNotFoundException;
import com.coinguard.user.dto.response.UserResponse;
import com.coinguard.user.dto.response.UserSummaryResponse;
import com.coinguard.user.mapper.UserMapper;
import com.coinguard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

//TODO : Swagger docs
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getLoggedInUser(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public List<UserSummaryResponse> searchUsers(String query, Long currentUserId) {
        if (query == null || query.trim().length() < 3) {
            throw new IllegalArgumentException("Search query must be at least 3 characters");
        }

        return userRepository.searchUsers(query.trim(), currentUserId)
                .stream()
                .limit(10)
                .map(userMapper::toUserSummaryResponse)
                .toList();
    }
}
