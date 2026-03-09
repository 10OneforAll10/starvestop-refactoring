package com.allforone.starvestop.domain.payment.service;

import com.allforone.starvestop.domain.order.entity.Order;
import com.allforone.starvestop.domain.order.enums.OrderStatus;
import com.allforone.starvestop.domain.order.service.OrderService;
import com.allforone.starvestop.domain.payment.entity.Payment;
import com.allforone.starvestop.domain.payment.enums.PaymentStatus;
import com.allforone.starvestop.domain.payment.event.PaymentEventRelay;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FinalizeSuccessTx {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentEventRelay paymentEventRelay;

    @Transactional
    public Long finalizeSuccess(String orderKey, String paymentKey) {
        Payment payment = paymentService.findByOrderKeyForUpdate(orderKey);
        Order order = orderService.getForPayment(payment.getOrder().getId());

        if (order.getStatus() == OrderStatus.PAID ||
                payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return order.getId();
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return order.getId();
        }

        payment.success(paymentKey);
        order.paid(LocalDateTime.now());
        paymentEventRelay.relayFrom(payment);

        return order.getId();
    }
}