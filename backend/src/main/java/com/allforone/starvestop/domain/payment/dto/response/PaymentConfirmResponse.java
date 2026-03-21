package com.allforone.starvestop.domain.payment.dto.response;

public record PaymentConfirmResponse(
        Long orderId,
        String orderKey,
        String status
) {
    public static PaymentConfirmResponse success(Long orderId, String orderKey) {
        return new PaymentConfirmResponse(orderId, orderKey, "SUCCEEDED");
    }
}
