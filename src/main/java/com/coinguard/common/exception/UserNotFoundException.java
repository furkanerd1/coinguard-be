package com.coinguard.common.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User with ID " + userId + " not found.");
    }

    public UserNotFoundException(String message,Long userId) {
        super(message + " User ID: " + userId);
    }
}
