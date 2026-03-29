package com.allforone.starvestop.domain.notification.dto;

public record NotificationTargetDto(Long cursorId, Long userId, String token, Long subscriptionId, String subscriptionName) {}