package com.allforone.starvestop.domain.notification.service;

import com.allforone.starvestop.common.config.RedisStreamConfig;
import com.allforone.starvestop.domain.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationStreamProducer {

    private final StringRedisTemplate redisTemplate;

    public void publishNotification(NotificationDto notification) {
        Map<String, String> payload = new HashMap<>();
        payload.put("token", notification.getToken());
        payload.put("title", notification.getTitle());
        payload.put("body", notification.getBody());

        MapRecord<String, String, String> record = MapRecord.create(RedisStreamConfig.STREAM_KEY, payload);
        RecordId recordId = redisTemplate.opsForStream().add(record);
    }
}
