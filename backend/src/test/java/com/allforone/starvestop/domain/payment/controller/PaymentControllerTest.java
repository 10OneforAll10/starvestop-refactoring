package com.allforone.starvestop.domain.payment.controller;

import com.allforone.starvestop.domain.payment.dto.response.PaymentConfirmResponse;
import com.allforone.starvestop.domain.payment.service.PaymentUsecase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentUsecase paymentUsecase;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    @DisplayName("success 콜백은 orderId 파라미터를 orderKey로 받아 명시적인 DTO를 반환한다")
    void success_returnsTypedResponse() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();

        when(paymentUsecase.confirmSuccess("pay_123", "order_key_123", 1000L))
                .thenReturn(PaymentConfirmResponse.success(18L, "order_key_123"));

        mockMvc.perform(get("/payments/success")
                        .param("paymentKey", "pay_123")
                        .param("orderId", "order_key_123")
                        .param("amount", "1000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(18))
                .andExpect(jsonPath("$.data.orderKey").value("order_key_123"))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
    }
}
