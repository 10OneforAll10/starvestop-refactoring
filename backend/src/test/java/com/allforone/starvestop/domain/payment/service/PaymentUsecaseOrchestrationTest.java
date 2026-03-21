package com.allforone.starvestop.domain.payment.service;

import com.allforone.starvestop.common.exception.CustomException;
import com.allforone.starvestop.common.exception.ErrorCode;
import com.allforone.starvestop.domain.order.service.OrderService;
import com.allforone.starvestop.domain.payment.dto.response.PaymentConfirmResponse;
import com.allforone.starvestop.domain.payment.dto.response.TossConfirmResponse;
import com.allforone.starvestop.domain.payment.dto.response.TossPaymentResponse;
import com.allforone.starvestop.domain.payment.event.PaymentEventRelay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentUsecaseOrchestrationTest {

    @Mock private PaymentService paymentService;
    @Mock private PrepareConfirmTx prepareConfirmTx;
    @Mock private FinalizeSuccessTx finalizeSuccessTx;
    @Mock private FinalizeFailTx finalizeFailTx;
    @Mock private OrderService orderService;
    @Mock private PaymentEventRelay paymentEventRelay;
    @Mock private PaymentVerifier paymentVerifier;

    @InjectMocks
    private PaymentUsecase paymentUsecase;

    @Test
    @DisplayName("confirmSuccess는 prepare -> PG confirm -> verify -> PG 조회 -> verify -> finalize 순으로 동작한다")
    void confirmSuccess_runsInExpectedOrder() {
        String orderKey = "ok_123";
        String paymentKey = "pk_123";
        long amount = 1000L;
        TossConfirmResponse confirmResponse = new TossConfirmResponse(paymentKey, orderKey, amount, "DONE");
        TossPaymentResponse paymentResponse = new TossPaymentResponse(paymentKey, orderKey, amount, "DONE");

        when(prepareConfirmTx.prepare(orderKey, paymentKey, amount))
                .thenReturn(PrepareConfirmResult.proceed(55L, Map.of(
                        "paymentKey", paymentKey,
                        "orderId", orderKey,
                        "amount", amount
                )));
        when(paymentService.tossApiConfirm(anyMap())).thenReturn(confirmResponse);
        when(paymentService.getPayment(paymentKey)).thenReturn(paymentResponse);
        when(finalizeSuccessTx.finalizeSuccess(orderKey, paymentKey)).thenReturn(55L);

        PaymentConfirmResponse response = paymentUsecase.confirmSuccess(paymentKey, orderKey, amount);

        assertEquals(55L, response.orderId());
        InOrder inOrder = inOrder(prepareConfirmTx, paymentService, paymentVerifier, finalizeSuccessTx);
        inOrder.verify(prepareConfirmTx).prepare(orderKey, paymentKey, amount);
        inOrder.verify(paymentService).tossApiConfirm(anyMap());
        inOrder.verify(paymentVerifier).verify(orderKey, paymentKey, BigDecimal.valueOf(amount), confirmResponse);
        inOrder.verify(paymentService).getPayment(paymentKey);
        inOrder.verify(paymentVerifier).verify(orderKey, paymentKey, BigDecimal.valueOf(amount), paymentResponse);
        inOrder.verify(finalizeSuccessTx).finalizeSuccess(orderKey, paymentKey);
    }

    @Test
    @DisplayName("confirmSuccess는 PG 응답 오류 시 실패 확정 후 PAYMENT_FAIL을 던진다")
    void confirmSuccess_handlesWebClientResponseException() {
        String orderKey = "ok_123";
        String paymentKey = "pk_123";
        long amount = 1000L;

        when(prepareConfirmTx.prepare(orderKey, paymentKey, amount))
                .thenReturn(PrepareConfirmResult.proceed(55L, Map.of("amount", amount)));

        WebClientResponseException ex = WebClientResponseException.create(
                500, "ISE", HttpHeaders.EMPTY, new byte[]{}, StandardCharsets.UTF_8
        );
        when(paymentService.tossApiConfirm(anyMap())).thenThrow(ex);

        CustomException thrown = assertThrows(CustomException.class,
                () -> paymentUsecase.confirmSuccess(paymentKey, orderKey, amount));

        assertEquals(ErrorCode.PAYMENT_FAIL, thrown.getErrorCode());
        verify(finalizeFailTx).finalizeFailure(orderKey, ex);
    }

    @Test
    @DisplayName("confirmSuccess는 네트워크 오류 시 실패 확정 없이 PAYMENT_CONFIRM_PENDING을 던진다")
    void confirmSuccess_handlesWebClientRequestException() {
        String orderKey = "ok_123";
        String paymentKey = "pk_123";
        long amount = 1000L;

        when(prepareConfirmTx.prepare(orderKey, paymentKey, amount))
                .thenReturn(PrepareConfirmResult.proceed(55L, Map.of("amount", amount)));

        WebClientRequestException ex = new WebClientRequestException(
                new RuntimeException("timeout"), HttpMethod.POST, URI.create("http://localhost"), HttpHeaders.EMPTY
        );
        when(paymentService.tossApiConfirm(anyMap())).thenThrow(ex);

        CustomException thrown = assertThrows(CustomException.class,
                () -> paymentUsecase.confirmSuccess(paymentKey, orderKey, amount));

        assertEquals(ErrorCode.PAYMENT_CONFIRM_PENDING, thrown.getErrorCode());
        verify(finalizeFailTx, never()).finalizeFailure(anyString(), any());
    }
}
