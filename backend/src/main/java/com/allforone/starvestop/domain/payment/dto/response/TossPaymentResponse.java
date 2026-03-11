package com.allforone.starvestop.domain.payment.dto.response;

public record TossPaymentResponse(
        String paymentKey,
        String orderId,
        Long totalAmount,
        String status
) {}