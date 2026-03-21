package com.allforone.starvestop.domain.payment.service;

import com.allforone.starvestop.domain.order.entity.Order;
import com.allforone.starvestop.domain.order.enums.OrderStatus;
import com.allforone.starvestop.domain.order.service.OrderService;
import com.allforone.starvestop.domain.payment.entity.Payment;
import com.allforone.starvestop.domain.payment.event.PaymentEventRelay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinalizeSuccessTxTest {

    @Mock private PaymentService paymentService;
    @Mock private OrderService orderService;
    @Mock private PaymentEventRelay paymentEventRelay;

    @InjectMocks
    private FinalizeSuccessTx finalizeSuccessTx;

    @Test
    @DisplayName("이미 성공 처리된 결제면 추가 상태 변경 없이 orderId만 반환한다")
    void finalizeSuccess_returnsOrderId_whenAlreadySucceeded() {
        Payment payment = mock(Payment.class, RETURNS_DEEP_STUBS);
        Order order = mock(Order.class);

        when(paymentService.findByOrderKeyForUpdate("order_key")).thenReturn(payment);
        when(payment.getOrder().getId()).thenReturn(10L);
        when(orderService.getForPayment(10L)).thenReturn(order);
        when(order.getStatus()).thenReturn(OrderStatus.PAID);
        when(order.getId()).thenReturn(10L);

        Long orderId = finalizeSuccessTx.finalizeSuccess("order_key", "payment_key");

        assertEquals(10L, orderId);
        verify(paymentEventRelay, never()).relayFrom(any());
    }

    @Test
    @DisplayName("PENDING 상태면 결제 성공과 주문 완료를 확정한다")
    void finalizeSuccess_marksSuccess_whenPaymentPending() {
        Order order = mock(Order.class);
        Payment payment = Payment.request(1L, order, "order_key", BigDecimal.valueOf(1000));
        payment.pending();
        payment.pullDomainEvents();

        when(paymentService.findByOrderKeyForUpdate("order_key")).thenReturn(payment);
        when(orderService.getForPayment(anyLong())).thenReturn(order);
        when(order.getId()).thenReturn(10L);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING);

        Long orderId = finalizeSuccessTx.finalizeSuccess("order_key", "payment_key");

        assertEquals(10L, orderId);
        assertEquals(com.allforone.starvestop.domain.payment.enums.PaymentStatus.SUCCEEDED, payment.getStatus());
        InOrder inOrder = inOrder(order, paymentEventRelay);
        inOrder.verify(order).paid(any());
        inOrder.verify(paymentEventRelay).relayFrom(payment);
    }
}
