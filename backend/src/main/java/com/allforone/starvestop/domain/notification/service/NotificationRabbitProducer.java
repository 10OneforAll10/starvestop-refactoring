package com.allforone.starvestop.domain.notification.service;

import com.allforone.starvestop.domain.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRabbitProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publish(NotificationDto notification) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_NAME,
                RabbitMqConfig.ROUTING_KEY,
                notification
        );
    }
}
