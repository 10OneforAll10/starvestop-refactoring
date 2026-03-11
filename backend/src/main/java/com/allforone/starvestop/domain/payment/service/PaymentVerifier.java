package com.allforone.starvestop.domain.payment.service;

import com.allforone.starvestop.common.exception.CustomException;
import com.allforone.starvestop.common.exception.ErrorCode;
import com.allforone.starvestop.domain.payment.dto.response.TossConfirmResponse;
import com.allforone.starvestop.domain.payment.dto.response.TossPaymentResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentVerifier {

    public void verify(
            String expectedOrderKey,
            String expectedPaymentKey,
            BigDecimal expectedAmount,
            TossConfirmResponse response
    ) {
        if (response == null) {
            throw new CustomException(ErrorCode.PAYMENT_VERIFY_FAIL);
        }

        if (!expectedOrderKey.equals(response.orderId())) {
            throw new CustomException(ErrorCode.PAYMENT_ORDER_MISMATCH);
        }

        if (!expectedPaymentKey.equals(response.paymentKey())) {
            throw new CustomException(ErrorCode.PAYMENT_KEY_MISMATCH);
        }

        if (response.totalAmount() == null ||
                expectedAmount.compareTo(BigDecimal.valueOf(response.totalAmount())) != 0) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        if (!"DONE".equals(response.status())) {
            throw new CustomException(ErrorCode.PAYMENT_STATUS_INVALID);
        }
    }

    public void verify(
            String expectedOrderKey,
            String expectedPaymentKey,
            BigDecimal expectedAmount,
            TossPaymentResponse response
    ) {
        if (response == null) {
            throw new CustomException(ErrorCode.PAYMENT_VERIFY_FAIL);
        }

        if (!expectedOrderKey.equals(response.orderId())) {
            throw new CustomException(ErrorCode.PAYMENT_ORDER_MISMATCH);
        }

        if (!expectedPaymentKey.equals(response.paymentKey())) {
            throw new CustomException(ErrorCode.PAYMENT_KEY_MISMATCH);
        }

        if (response.totalAmount() == null ||
                expectedAmount.compareTo(BigDecimal.valueOf(response.totalAmount())) != 0) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        if (!"DONE".equals(response.status())) {
            throw new CustomException(ErrorCode.PAYMENT_STATUS_INVALID);
        }
    }
}