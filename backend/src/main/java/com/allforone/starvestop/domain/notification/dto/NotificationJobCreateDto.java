package com.allforone.starvestop.domain.notification.dto;

import java.time.LocalDateTime;

public record NotificationJobCreateDto(
        Long userId,
        String token,
        String subscriptionName,
        LocalDateTime targetTime
) {}

