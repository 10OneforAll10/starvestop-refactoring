package com.allforone.starvestop.domain.payment.service;

import com.allforone.starvestop.common.exception.CustomException;
import com.allforone.starvestop.common.exception.ErrorCode;
import com.allforone.starvestop.domain.order.entity.Order;
import com.allforone.starvestop.domain.order.service.OrderService;
import com.allforone.starvestop.domain.payment.dto.response.*;
import com.allforone.starvestop.domain.payment.entity.Payment;
import com.allforone.starvestop.domain.payment.event.PaymentEventRelay;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentUsecase {

    private final PaymentService paymentService;
    private final PrepareConfirmTx prepareConfirmTx;
    private final FinalizeSuccessTx finalizeSuccessTx;
    private final FinalizeFailTx finalizeFailTx;
    private final OrderService orderService;
    private final PaymentEventRelay paymentEventRelay;
    private final PaymentVerifier paymentVerifier;

    @Transactional
    public CreatePaymentResponse createPayment(Long userId, Long orderId) {
        Order order = orderService.getForPayment(orderId);

        if (!userId.equals(order.getUser().getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        String orderKey = order.getOrderKey();
        BigDecimal amount = order.getAmount();

        Optional<Payment> existing = paymentService.findByOrderKey(orderKey);
        if (existing.isPresent()) {
            return CreatePaymentResponse.from(existing.get());
        }

        try {
            Payment payment = Payment.request(userId, order, orderKey, amount);
            paymentService.saveAndFlush(payment);

            payment.markRequestedEvent();
            paymentEventRelay.relayFrom(payment);

            return CreatePaymentResponse.from(payment);

        } catch (DataIntegrityViolationException e) {
            Payment found = paymentService.getByOrderKey(orderKey);
            return CreatePaymentResponse.from(found);
        }
    }

    public PaymentConfirmResponse confirmSuccess(String paymentKey, String orderKey, Long amount) {
        PrepareConfirmResult prepareResult = prepareConfirm(orderKey, paymentKey, amount);

        if (prepareResult.alreadySucceeded()) {
            return PaymentConfirmResponse.success(prepareResult.orderId(), orderKey);
        }

        try {
            TossConfirmResponse confirmResponse = requestPaymentConfirmation(prepareResult);
            verifyConfirmedPayment(orderKey, paymentKey, amount, confirmResponse);

            TossPaymentResponse paymentResponse = fetchConfirmedPayment(paymentKey);
            verifyFetchedPayment(orderKey, paymentKey, amount, paymentResponse);

            return finalizeConfirmedPayment(orderKey, paymentKey);

        } catch (WebClientResponseException e) {
            handlePgResponseFailure(orderKey, e);
            throw new CustomException(ErrorCode.PAYMENT_FAIL);

        } catch (WebClientRequestException e) {
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_PENDING);

        } catch (CustomException e) {
            handleVerificationFailure(orderKey, e);
            throw e;
        }
    }

    private PrepareConfirmResult prepareConfirm(String orderKey, String paymentKey, Long amount) {
        return prepareConfirmTx.prepare(orderKey, paymentKey, amount);
    }

    private TossConfirmResponse requestPaymentConfirmation(PrepareConfirmResult prepareResult) {
        return paymentService.tossApiConfirm(prepareResult.requestPayload());
    }

    private void verifyConfirmedPayment(String orderKey, String paymentKey, Long amount, TossConfirmResponse confirmResponse) {
        paymentVerifier.verify(orderKey, paymentKey, BigDecimal.valueOf(amount), confirmResponse);
    }

    private TossPaymentResponse fetchConfirmedPayment(String paymentKey) {
        return paymentService.getPayment(paymentKey);
    }

    private void verifyFetchedPayment(String orderKey, String paymentKey, Long amount, TossPaymentResponse paymentResponse) {
        paymentVerifier.verify(orderKey, paymentKey, BigDecimal.valueOf(amount), paymentResponse);
    }

    private PaymentConfirmResponse finalizeConfirmedPayment(String orderKey, String paymentKey) {
        Long confirmedOrderId = finalizeSuccessTx.finalizeSuccess(orderKey, paymentKey);
        return PaymentConfirmResponse.success(confirmedOrderId, orderKey);
    }

    private void handlePgResponseFailure(String orderKey, WebClientResponseException e) {
        finalizeFailTx.finalizeFailure(orderKey, e);
    }

    private void handleVerificationFailure(String orderKey, CustomException e) {
        finalizeFailTx.finalizeFailure(orderKey, e);
    }

    public void failRedirect(String code, String orderId) {
        throw new CustomException(ErrorCode.PAYMENT_FAIL);
    }

    public Page<GetPaymentResponse> getMyPaymentList(Long userId, Pageable pageable) {
        Page<Payment> paymentList = paymentService.findByOrderUserId(userId, pageable);
        return paymentList.map(GetPaymentResponse::from);
    }

    public GetPaymentDetailsResponse getPayment(Long userId, Long paymentId) {
        Payment payment = paymentService.findById(paymentId);

        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return GetPaymentDetailsResponse.from(payment);
    }
}