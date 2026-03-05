package com.coinguard.common.service;

import com.coinguard.common.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    private static final String FROM_EMAIL = "noreply@coinguard.com";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_FULL_NAME = "Test User";
    private static final String TEST_RESET_TOKEN = "test-reset-token-123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
    }

    @Test
    void sendWelcomeEmail_ShouldSendSuccessfully() {
        // Given
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendWelcomeEmail(TEST_EMAIL, TEST_FULL_NAME);

        // Then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendWelcomeEmail_ShouldHandleException() {
        // Given
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendWelcomeEmail(TEST_EMAIL, TEST_FULL_NAME);

        // Then - Should not throw exception (handled internally)
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPasswordResetEmail_ShouldSendSuccessfully() {
        // Given
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_RESET_TOKEN);

        // Then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPasswordResetEmail_ShouldHandleException() {
        // Given
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendPasswordResetEmail(TEST_EMAIL, TEST_RESET_TOKEN);

        // Then - Should not throw exception (handled internally)
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}

