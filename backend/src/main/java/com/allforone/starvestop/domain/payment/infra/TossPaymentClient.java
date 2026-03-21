package com.allforone.starvestop.domain.payment.infra;

import com.allforone.starvestop.domain.payment.dto.response.TossConfirmResponse;
import com.allforone.starvestop.domain.payment.dto.response.TossPaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    private final @Qualifier("paymentWebClient") WebClient paymentWebClient;

    public TossPaymentResponse getPayment(String paymentKey) {
        return paymentWebClient.get()
                .uri("/v1/payments/{paymentKey}", paymentKey)
                .retrieve()
                .bodyToMono(TossPaymentResponse.class)
                .block();
    }

    public TossConfirmResponse confirmPayment(Map<String, Object> requestPayload) {
        return paymentWebClient.post()
                .uri("/v1/payments/confirm")
                .bodyValue(requestPayload)
                .retrieve()
                .bodyToMono(TossConfirmResponse.class)
                .block();
    }
}
