package com.allforone.starvestop.domain.notification.service;

import com.allforone.starvestop.domain.notification.dto.NotificationDto;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRabbitConsumer {

    private final FcmSender fcmSender;

    public void consumeBatch(List<NotificationDto> dtoList,
                             Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        List<Message> fcmMessageList = dtoList.stream()
                .map(dto -> Message.builder()
                        .setToken(dto.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(dto.getTitle())
                                .setBody(dto.getBody())
                                .build())
                        .build())
                .toList();

        try {
            fcmSender.sendAllAsync(fcmMessageList).join();

            channel.basicAck(tag, true);
            log.info("{}건 발송", fcmMessageList.size());
        } catch (Exception e) {
            channel.basicNack(tag, true, false);
        }
    }

}
