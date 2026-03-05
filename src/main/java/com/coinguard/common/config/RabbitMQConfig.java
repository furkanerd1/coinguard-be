package com.coinguard.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * RabbitMQ Configuration
 * Configures queues, exchanges, bindings and message converters
 */
@Configuration
@SuppressWarnings("removal")
public class RabbitMQConfig {

    // Queue names
    public static final String EMAIL_QUEUE = "email.queue";
    public static final String NOTIFICATION_QUEUE = "notification.queue";

    // Exchange names
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";

    // Routing keys
    public static final String EMAIL_ROUTING_KEY = "email.routing.key";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.routing.key";

    /**
     * Email queue
     */
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "email.dlq") //Dead Letter Queue
                .build();
    }

    /**
     * Notification queue
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "notification.dlq")//Dead Letter Queue
                .build();
    }

    /**
     * Email exchange
     */
    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    /**
     * Notification exchange
     */
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    /**
     * Binding for email queue
     */
    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailQueue)
                .to(emailExchange)
                .with(EMAIL_ROUTING_KEY);
    }

    /**
     * Binding for notification queue
     */
    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange)
                .with(NOTIFICATION_ROUTING_KEY);
    }

    /**
     * Message converter for JSON serialization with custom ObjectMapper
     * Note: Jackson2JsonMessageConverter is deprecated but still functional in Spring AMQP 4.0
     * Will be replaced with a newer alternative in future versions
     */
    @Bean
    @SuppressWarnings("removal")
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate with JSON message converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}