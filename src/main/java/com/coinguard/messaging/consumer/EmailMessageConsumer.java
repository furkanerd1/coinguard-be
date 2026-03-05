package com.coinguard.messaging.consumer;

import com.coinguard.common.config.RabbitMQConfig;
import com.coinguard.common.service.EmailService;
import com.coinguard.messaging.dto.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailMessageConsumer {

    private final EmailService emailService;

    /**
     * Listen to email queue and process messages
     *
     * @param emailMessage email message from queue
     */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void consumeEmailMessage(EmailMessage emailMessage) {
        try {
            log.info("Received email message from queue: {}", emailMessage);

            if ("WELCOME".equals(emailMessage.templateName())) {
                emailService.sendWelcomeEmail(emailMessage.to(), emailMessage.body());
            } else if ("RESET_PASSWORD".equals(emailMessage.templateName())) {
                emailService.sendPasswordResetEmail(emailMessage.to(), emailMessage.body());
            }
            log.info("Email message processed successfully");
        } catch (Exception e) {
            log.error("Error processing email message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process email message", e);
        }
    }
}

