package com.allforone.starvestop.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisStreamConfig {

    private final StringRedisTemplate redisTemplate;
    public static final String STREAM_KEY = "fcm:notifications";
    public static final String CONSUMER_GROUP = "fcm_sender-group";

    @PostConstruct
    public void initConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0-0"), CONSUMER_GROUP);
            log.info("Redis Stream Group Created {}", CONSUMER_GROUP);
        } catch (Exception ignored) {}
    }

}
