package com.coinguard.common.service;

public interface EmailService {
    void sendWelcomeEmail(String to, String fullName);
    void sendPasswordResetEmail(String to, String resetToken);
}
