package com.allforone.starvestop.domain.payment.service;

import com.allforone.starvestop.common.exception.CustomException;
import com.allforone.starvestop.common.exception.ErrorCode;
import com.allforone.starvestop.domain.order.entity.Order;
import com.allforone.starvestop.domain.order.enums.OrderStatus;
import com.allforone.starvestop.domain.order.service.OrderService;
import com.allforone.starvestop.domain.payment.entity.Payment;
import com.allforone.starvestop.domain.payment.event.PaymentEventRelay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrepareConfirmTxTest {

    @Mock private PaymentService paymentService;
    @Mock private OrderService orderService;
    @Mock private PaymentEventRelay paymentEventRelay;
    @Mock private FinalizeFailTx finalizeFailTx;

    @InjectMocks
    private PrepareConfirmTx prepareConfirmTx;

    @Test
    @DisplayName("이미 결제가 완료된 주문이면 추가 승인 없이 alreadySucceeded를 반환한다")
    void prepare_returnsAlreadySucceeded_whenOrderAlreadyPaid() {
        Payment payment = mock(Payment.class, RETURNS_DEEP_STUBS);
        Order order = mock(Order.class);

        when(paymentService.findByOrderKeyForUpdate("order_key")).thenReturn(payment);
        when(payment.getOrder().getId()).thenReturn(10L);
        when(orderService.getForPayment(10L)).thenReturn(order);
        when(order.getStatus()).thenReturn(OrderStatus.PAID);
        when(order.getId()).thenReturn(10L);

        PrepareConfirmResult result = prepareConfirmTx.prepare("order_key", "payment_key", 1000L);

        assertTrue(result.alreadySucceeded());
        assertEquals(10L, result.orderId());
        assertNull(result.requestPayload());
        verify(paymentEventRelay, never()).relayFrom(any());
    }

    @Test
    @DisplayName("금액이 일치하면 PENDING으로 전이하고 요청 payload를 반환한다")
    void prepare_movesToPending_whenAmountMatches() {
        Order order = mock(Order.class);
        Payment payment = Payment.request(1L, order, "order_key", BigDecimal.valueOf(1000));

        when(paymentService.findByOrderKeyForUpdate("order_key")).thenReturn(payment);
        when(orderService.getForPayment(anyLong())).thenReturn(order);
        when(order.getId()).thenReturn(10L);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING);

        PrepareConfirmResult result = prepareConfirmTx.prepare("order_key", "payment_key", 1000L);

        assertFalse(result.alreadySucceeded());
        assertEquals(10L, result.orderId());
        assertEquals("payment_key", result.requestPayload().get("paymentKey"));
        assertEquals("order_key", result.requestPayload().get("orderId"));
        assertEquals(1000L, result.requestPayload().get("amount"));
        assertEquals(com.allforone.starvestop.domain.payment.enums.PaymentStatus.PENDING, payment.getStatus());
        verify(paymentEventRelay).relayFrom(payment);
    }

    @Test
    @DisplayName("금액이 다르면 실패 처리 후 PAYMENT_FAIL 예외를 던진다")
    void prepare_failsImmediately_whenAmountMismatch() {
        Order order = mock(Order.class);
        Payment payment = Payment.request(1L, order, "order_key", BigDecimal.valueOf(900));

        when(paymentService.findByOrderKeyForUpdate("order_key")).thenReturn(payment);
        when(orderService.getForPayment(anyLong())).thenReturn(order);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING);

        CustomException thrown = assertThrows(CustomException.class,
                () -> prepareConfirmTx.prepare("order_key", "payment_key", 1000L));

        assertEquals(ErrorCode.PAYMENT_FAIL, thrown.getErrorCode());
        verify(finalizeFailTx).failAmountMismatchInSameTx(payment, "payment_key", "order_key");
        verify(paymentEventRelay, never()).relayFrom(payment);
    }

    @Test
    @DisplayName("이미 PENDING 상태면 중복 승인 요청을 막는다")
    void prepare_throws_whenPaymentAlreadyPending() {
        Payment payment = mock(Payment.class, RETURNS_DEEP_STUBS);
        Order order = mock(Order.class);

        when(paymentService.findByOrderKeyForUpdate("order_key")).thenReturn(payment);
        when(payment.getOrder().getId()).thenReturn(10L);
        when(orderService.getForPayment(10L)).thenReturn(order);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING);
        when(payment.getStatus()).thenReturn(com.allforone.starvestop.domain.payment.enums.PaymentStatus.PENDING);

        CustomException thrown = assertThrows(CustomException.class,
                () -> prepareConfirmTx.prepare("order_key", "payment_key", 1000L));

        assertEquals(ErrorCode.PAYMENT_FAIL, thrown.getErrorCode());
        verify(paymentEventRelay, never()).relayFrom(any());
    }
}
