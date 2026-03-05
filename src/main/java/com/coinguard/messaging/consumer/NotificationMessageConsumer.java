package com.coinguard.messaging.consumer;

import com.coinguard.common.config.RabbitMQConfig;
import com.coinguard.messaging.dto.NotificationMessage;
import com.coinguard.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationMessageConsumer {

    private final NotificationService notificationService;

    /**
     * Listen to notification queue and process messages
     *
     * @param notificationMessage notification message from queue
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consumeNotificationMessage(NotificationMessage notificationMessage) {
        try {
            log.info("Received notification message from queue: {}", notificationMessage);

            notificationService.saveNotification(notificationMessage);

            log.info("Notification message processed successfully");
        } catch (Exception e) {
            log.error("Error processing notification message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process notification message", e);
        }
    }
}

