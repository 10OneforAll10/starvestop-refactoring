package com.allforone.starvestop.domain.notification.service;

import com.allforone.starvestop.common.config.RedisStreamConfig;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationStreamConsumer {

    private final StringRedisTemplate redisTemplate;
    private final FcmSender fcmSender;

    private static final String CONSUMER_NAME = "work-1";
    private static final int CHUNK_SIZE = 500;

    @Scheduled(fixedDelay = 1000)
    public void consumeAndSend() {
        List<MapRecord<String, Object, Object>> messageList = redisTemplate.opsForStream().read(
                Consumer.from(RedisStreamConfig.CONSUMER_GROUP, CONSUMER_NAME),
                StreamReadOptions.empty().count(CHUNK_SIZE).block(Duration.ofMillis(500)),
                StreamOffset.create(RedisStreamConfig.STREAM_KEY, ReadOffset.lastConsumed())
        );

        if (messageList == null || messageList.isEmpty()) {
            return;
        }

        List<Message> fcmMessageList = new ArrayList<>();
        List<String> recordIdList = new ArrayList<>();

        for (MapRecord<String, Object, Object> message : messageList) {
            Map<Object, Object> value = message.getValue();

            fcmMessageList.add(Message.builder()
                    .setToken((String) value.get("token"))
                    .setNotification(Notification.builder()
                            .setTitle((String) value.get("title"))
                            .setBody((String) value.get("body"))
                            .build())
                    .build());

            recordIdList.add(message.getId().getValue());
        }

        try {
            fcmSender.sendAllAsync(fcmMessageList).join();

            redisTemplate.opsForStream().acknowledge(
                    RedisStreamConfig.STREAM_KEY,
                    RedisStreamConfig.CONSUMER_GROUP,
                    recordIdList.toArray(new String[0])
            );
        } catch (Exception ignored) {}
    }
}
