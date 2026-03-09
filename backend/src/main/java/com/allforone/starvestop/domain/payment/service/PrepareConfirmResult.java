package com.allforone.starvestop.domain.payment.service;

import java.util.Map;

public record PrepareConfirmResult(
        boolean alreadySucceeded,
        Long orderId,
        Map<String, Object> requestPayload
) {
    public static PrepareConfirmResult alreadySucceeded(Long orderId) {
        return new PrepareConfirmResult(true, orderId, null);
    }

    public static PrepareConfirmResult proceed(Long orderId, Map<String, Object> requestPayload) {
        return new PrepareConfirmResult(false, orderId, requestPayload);
    }
}