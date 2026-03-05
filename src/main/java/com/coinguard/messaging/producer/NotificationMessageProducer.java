package com.coinguard.messaging.producer;

import com.coinguard.common.config.RabbitMQConfig;
import com.coinguard.messaging.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Send notification message to RabbitMQ queue
     *
     * @param notificationMessage notification message to send
     */
    public void sendNotificationMessage(NotificationMessage notificationMessage) {
        try {
            log.info("Sending notification message to queue: {}", notificationMessage);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                    notificationMessage
            );
            log.info("Notification message sent successfully");
        } catch (Exception e) {
            log.error("Error sending notification message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send notification message", e);
        }
    }
}

