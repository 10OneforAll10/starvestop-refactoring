package com.allforone.starvestop.domain.payment.service;

import com.allforone.starvestop.common.exception.CustomException;
import com.allforone.starvestop.common.exception.ErrorCode;
import com.allforone.starvestop.domain.order.entity.Order;
import com.allforone.starvestop.domain.order.service.OrderService;
import com.allforone.starvestop.domain.payment.dto.response.CreatePaymentResponse;
import com.allforone.starvestop.domain.payment.dto.response.GetPaymentDetailsResponse;
import com.allforone.starvestop.domain.payment.dto.response.GetPaymentResponse;
import com.allforone.starvestop.domain.payment.entity.Payment;
import com.allforone.starvestop.domain.payment.event.PaymentEventRelay;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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

            // REQUESTED 상태 이벤트 기록
            payment.markRequestedEvent();
            paymentEventRelay.relayFrom(payment);

            return CreatePaymentResponse.from(payment);

        } catch (DataIntegrityViolationException e) {
            Payment found = paymentService.getByOrderKey(orderKey);
            return CreatePaymentResponse.from(found);
        }
    }

    public Long confirmSuccess(String paymentKey, String orderKey, Long amount) {
        PrepareConfirmResult prepareResult =
                prepareConfirmTx.prepare(orderKey, paymentKey, amount);

        if (prepareResult.alreadySucceeded()) {
            return prepareResult.orderId();
        }

        try {
            paymentService.tossApiConfirm(prepareResult.requestPayload());
            return finalizeSuccessTx.finalizeSuccess(orderKey, paymentKey);
        } catch (WebClientResponseException e) {
            finalizeFailTx.finalizeFailure(orderKey, e);
            throw new CustomException(ErrorCode.PAYMENT_FAIL);
        }
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