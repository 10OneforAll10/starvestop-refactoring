package com.allforone.starvestop.domain.payment.dto.response;

public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        Long totalAmount,
        String status
) {
}