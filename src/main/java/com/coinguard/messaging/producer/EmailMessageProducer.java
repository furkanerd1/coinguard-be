package com.coinguard.messaging.producer;

import com.coinguard.common.config.RabbitMQConfig;
import com.coinguard.messaging.dto.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Send email message to RabbitMQ queue
     *
     * @param emailMessage email message to send
     */
    public void sendEmailMessage(EmailMessage emailMessage) {
        try {
            log.info("Sending email message to queue: {}", emailMessage);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EMAIL_EXCHANGE,
                    RabbitMQConfig.EMAIL_ROUTING_KEY,
                    emailMessage
            );
            log.info("Email message sent successfully");
        } catch (Exception e) {
            log.error("Error sending email message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email message", e);
        }
    }
}

