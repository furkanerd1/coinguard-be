package com.coinguard.user.service;

import com.coinguard.common.exception.InvalidPasswordException;
import com.coinguard.common.exception.UserNotFoundException;
import com.coinguard.messaging.dto.EmailMessage;
import com.coinguard.messaging.dto.NotificationMessage;
import com.coinguard.messaging.producer.EmailMessageProducer;
import com.coinguard.messaging.producer.NotificationMessageProducer;
import com.coinguard.user.dto.request.UpdatePasswordRequest;
import com.coinguard.user.dto.request.UpdateUserRequest;
import com.coinguard.user.dto.response.UserResponse;
import com.coinguard.user.dto.response.UserSummaryResponse;
import com.coinguard.user.entity.User;
import com.coinguard.user.mapper.UserMapper;
import com.coinguard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//TODO : Swagger docs
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationMessageProducer notificationProducer;
    private final EmailMessageProducer emailProducer;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getLoggedInUser(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    @Transactional(readOnly = true)
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

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }

        if (request.username() != null && !request.username().isBlank()) {
            user.setUsername(request.username());
        }

        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            user.setPhoneNumber(request.phoneNumber());
        }

        User updatedUser = userRepository.save(user);

        // Send notification after successful profile update
        notificationProducer.sendNotificationMessage(new NotificationMessage(
                userId,
                "Profile Updated",
                "Your profile information has been updated successfully",
                "INFO"
        ));

        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new InvalidPasswordException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Send security alert notification
        notificationProducer.sendNotificationMessage(new NotificationMessage(
                userId,
                "Security Alert",
                "Your password has been changed successfully",
                "WARNING"
        ));

        // Send email notification
        emailProducer.sendEmailMessage(new EmailMessage(
                user.getEmail(),
                "Password Changed",
                user.getFullName(),
                "PASSWORD_CHANGED"
        ));
    }
}
