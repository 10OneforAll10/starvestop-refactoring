package com.allforone.starvestop.domain.payment.service;

import com.allforone.starvestop.common.exception.CustomException;
import com.allforone.starvestop.domain.order.entity.OrderProduct;
import com.allforone.starvestop.domain.order.service.OrderProductService;
import com.allforone.starvestop.domain.payment.entity.Payment;
import com.allforone.starvestop.domain.payment.enums.PaymentStatus;
import com.allforone.starvestop.domain.payment.event.PaymentEventRelay;
import com.allforone.starvestop.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinalizeFailTx {

    private final PaymentService paymentService;
    private final OrderProductService orderProductService;
    private final ProductService productService;
    private final PaymentEventRelay paymentEventRelay;

    @Transactional
    public void finalizeFailure(String orderKey, Exception e) {
        Payment payment = paymentService.findByOrderKeyForUpdate(orderKey);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        if (isRetryable(e)) {
            payment.failRetryable(buildFailurePayload(e));
            paymentEventRelay.relayFrom(payment);
            return;
        }

        payment.failNonRetryable(buildFailurePayload(e));
        releaseReservedStock(payment);
        payment.markStockReleased();
        paymentEventRelay.relayFrom(payment);
    }

    @Transactional
    public void failAmountMismatchInSameTx(Payment payment, String paymentKey, String orderKey) {
        if (payment.getStatus() == PaymentStatus.SUCCEEDED ||
                payment.getStatus() == PaymentStatus.CANCELED ||
                payment.getStatus() == PaymentStatus.FAILED_NON_RETRYABLE) {
            return;
        }

        payment.failNonRetryable(paymentService.toJson(Map.of(
                "code", "PAYMENT_AMOUNT_MISMATCH",
                "message", "결제 금액이 올바르지 않습니다.",
                "orderId", orderKey,
                "paymentKey", paymentKey
        )));

        releaseReservedStock(payment);
        payment.markStockReleased();
        paymentEventRelay.relayFrom(payment);
    }

    private boolean isRetryable(Exception e) {
        if (e instanceof WebClientResponseException webEx) {
            return webEx.getStatusCode().is5xxServerError();
        }

        return false;
    }

    private String buildFailurePayload(Exception e) {
        if (e instanceof WebClientResponseException webEx) {
            return paymentService.toJson(Map.of(
                    "type", "WEBCLIENT_RESPONSE_EXCEPTION",
                    "status", webEx.getStatusCode().value(),
                    "message", webEx.getMessage(),
                    "responseBody", webEx.getResponseBodyAsString()
            ));
        }

        if (e instanceof CustomException customEx) {
            return paymentService.toJson(Map.of(
                    "type", "CUSTOM_EXCEPTION",
                    "errorCode", customEx.getErrorCode().name(),
                    "message", customEx.getMessage()
            ));
        }

        return paymentService.toJson(Map.of(
                "type", e.getClass().getSimpleName(),
                "message", e.getMessage()
        ));
    }

    private void releaseReservedStock(Payment payment) {
        List<OrderProduct> orderProducts =
                orderProductService.findListByOrderId(payment.getOrder().getId());

        for (OrderProduct op : orderProducts) {
            productService.increaseById(op.getProductId(), op.getQuantity());
        }
    }
}