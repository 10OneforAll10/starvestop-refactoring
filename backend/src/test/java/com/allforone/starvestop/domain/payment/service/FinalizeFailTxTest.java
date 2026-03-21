package com.allforone.starvestop.domain.payment.service;

import com.allforone.starvestop.common.exception.CustomException;
import com.allforone.starvestop.common.exception.ErrorCode;
import com.allforone.starvestop.domain.order.entity.Order;
import com.allforone.starvestop.domain.order.entity.OrderProduct;
import com.allforone.starvestop.domain.order.service.OrderProductService;
import com.allforone.starvestop.domain.payment.entity.Payment;
import com.allforone.starvestop.domain.payment.event.PaymentEventRelay;
import com.allforone.starvestop.domain.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinalizeFailTxTest {

    @Mock private PaymentService paymentService;
    @Mock private OrderProductService orderProductService;
    @Mock private ProductService productService;
    @Mock private PaymentEventRelay paymentEventRelay;

    @InjectMocks
    private FinalizeFailTx finalizeFailTx;

    @Test
    @DisplayName("5xx 실패는 retryable로 처리하고 재고는 복구하지 않는다")
    void finalizeFailure_marksRetryable_whenPgReturns5xx() {
        Order order = mock(Order.class);
        Payment payment = Payment.request(1L, order, "order_key", BigDecimal.valueOf(1000));
        payment.pending();
        payment.pullDomainEvents();

        when(paymentService.findByOrderKeyForUpdate("order_key")).thenReturn(payment);
        when(paymentService.toJson(any())).thenReturn("{payload}");

        WebClientResponseException ex = WebClientResponseException.create(
                500, "ISE", HttpHeaders.EMPTY, new byte[]{}, StandardCharsets.UTF_8
        );

        finalizeFailTx.finalizeFailure("order_key", ex);

        assertEquals(com.allforone.starvestop.domain.payment.enums.PaymentStatus.FAILED_RETRYABLE, payment.getStatus());
        assertFalse(payment.isStockReleased());
        verify(orderProductService, never()).findListByOrderId(anyLong());
        verify(productService, never()).increaseById(anyLong(), anyInt());
        verify(paymentEventRelay).relayFrom(payment);
    }

    @Test
    @DisplayName("4xx 또는 비재시도성 실패는 non-retryable로 처리하고 재고를 복구한다")
    void finalizeFailure_marksNonRetryableAndReleasesStock() {
        Order order = mock(Order.class);
        Payment payment = Payment.request(1L, order, "order_key", BigDecimal.valueOf(1000));
        payment.pending();
        payment.pullDomainEvents();

        OrderProduct op1 = OrderProduct.create(order, 101L, "A", 2, BigDecimal.valueOf(1000));
        OrderProduct op2 = OrderProduct.create(order, 202L, "B", 1, BigDecimal.valueOf(500));

        when(paymentService.findByOrderKeyForUpdate("order_key")).thenReturn(payment);
        when(paymentService.toJson(any())).thenReturn("{payload}");
        when(orderProductService.findListByOrderId(anyLong())).thenReturn(List.of(op1, op2));
        when(order.getId()).thenReturn(10L);

        CustomException ex = new CustomException(ErrorCode.PAYMENT_ORDER_MISMATCH);
        finalizeFailTx.finalizeFailure("order_key", ex);

        assertEquals(com.allforone.starvestop.domain.payment.enums.PaymentStatus.FAILED_NON_RETRYABLE, payment.getStatus());
        assertTrue(payment.isStockReleased());
        verify(productService).increaseById(101L, 2);
        verify(productService).increaseById(202L, 1);
        verify(paymentEventRelay).relayFrom(payment);
    }

    @Test
    @DisplayName("금액 불일치는 REQUESTED 상태에서도 즉시 non-retryable 실패 처리된다")
    void failAmountMismatchInSameTx_marksFailureFromRequestedState() {
        Order order = mock(Order.class);
        Payment payment = Payment.request(1L, order, "order_key", BigDecimal.valueOf(900));
        OrderProduct op = OrderProduct.create(order, 101L, "A", 2, BigDecimal.valueOf(1000));

        when(paymentService.toJson(any())).thenReturn("{payload}");
        when(order.getId()).thenReturn(10L);
        when(orderProductService.findListByOrderId(10L)).thenReturn(List.of(op));

        assertDoesNotThrow(() -> finalizeFailTx.failAmountMismatchInSameTx(payment, "payment_key", "order_key"));
        assertEquals(com.allforone.starvestop.domain.payment.enums.PaymentStatus.FAILED_NON_RETRYABLE, payment.getStatus());
        assertTrue(payment.isStockReleased());
        verify(productService).increaseById(101L, 2);
        verify(paymentEventRelay).relayFrom(payment);
    }
}
