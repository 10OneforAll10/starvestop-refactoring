package com.allforone.starvestop.domain.payment.service;

import com.allforone.starvestop.domain.payment.dto.response.TossConfirmResponse;
import com.allforone.starvestop.domain.payment.dto.response.TossPaymentResponse;
import com.allforone.starvestop.domain.payment.infra.TossPaymentClient;
import com.allforone.starvestop.domain.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceDelegationTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private TossPaymentClient tossPaymentClient;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("결제 조회는 TossPaymentClient에 위임한다")
    void getPayment_delegatesToTossPaymentClient() {
        TossPaymentResponse response = new TossPaymentResponse("pk", "ok", 1000L, "DONE");
        when(tossPaymentClient.getPayment("pk")).thenReturn(response);

        TossPaymentResponse result = paymentService.getPayment("pk");

        assertSame(response, result);
        verify(tossPaymentClient).getPayment("pk");
    }

    @Test
    @DisplayName("결제 승인 요청은 TossPaymentClient에 위임한다")
    void tossApiConfirm_delegatesToTossPaymentClient() {
        Map<String, Object> payload = Map.of("paymentKey", "pk", "orderId", "ok", "amount", 1000L);
        TossConfirmResponse response = new TossConfirmResponse("pk", "ok", 1000L, "DONE");
        when(tossPaymentClient.confirmPayment(payload)).thenReturn(response);

        TossConfirmResponse result = paymentService.tossApiConfirm(payload);

        assertSame(response, result);
        verify(tossPaymentClient).confirmPayment(payload);
    }
}
