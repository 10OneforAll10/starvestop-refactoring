package com.allforone.starvestop.domain.payment.service;

import com.allforone.starvestop.common.exception.CustomException;
import com.allforone.starvestop.common.exception.ErrorCode;
import com.allforone.starvestop.domain.order.entity.Order;
import com.allforone.starvestop.domain.order.enums.OrderStatus;
import com.allforone.starvestop.domain.order.service.OrderService;
import com.allforone.starvestop.domain.payment.entity.Payment;
import com.allforone.starvestop.domain.payment.enums.PaymentStatus;
import com.allforone.starvestop.domain.payment.event.PaymentEventRelay;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrepareConfirmTx {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentEventRelay paymentEventRelay;
    private final FinalizeFailTx finalizeFailTx;

    @Transactional
    public PrepareConfirmResult prepare(String orderKey, String paymentKey, Long amount) {
        Payment payment = paymentService.findByOrderKeyForUpdate(orderKey);
        Order order = orderService.getForPayment(payment.getOrder().getId());

        if (order.getStatus() == OrderStatus.PAID ||
                payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return PrepareConfirmResult.alreadySucceeded(order.getId());
        }

        if (payment.getStatus() == PaymentStatus.PENDING) {
            throw new CustomException(ErrorCode.PAYMENT_FAIL);
        }

        if (payment.getStatus() == PaymentStatus.CANCELED ||
                payment.getStatus() == PaymentStatus.FAILED_NON_RETRYABLE) {
            throw new CustomException(ErrorCode.PAYMENT_FAIL);
        }

        if (payment.getAmount().compareTo(BigDecimal.valueOf(amount)) != 0) {
            finalizeFailTx.failAmountMismatchInSameTx(payment, paymentKey, orderKey);
            throw new CustomException(ErrorCode.PAYMENT_FAIL);
        }

        payment.pending();
        paymentEventRelay.relayFrom(payment);

        Map<String, Object> requestPayload = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderKey,
                "amount", amount
        );

        return PrepareConfirmResult.proceed(order.getId(), requestPayload);
    }
}