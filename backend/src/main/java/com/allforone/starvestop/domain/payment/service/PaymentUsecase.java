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
        PrepareConfirmResult prepareResult =
                prepareConfirmTx.prepare(orderKey, paymentKey, amount);

        if (prepareResult.alreadySucceeded()) {
            return PaymentConfirmResponse.success(prepareResult.orderId(), orderKey);
        }

        try {
            // 1. PG 승인 요청
            TossConfirmResponse confirmResponse =
                    paymentService.tossApiConfirm(prepareResult.requestPayload());

            // 2. 서버 재검증 (응답값 기반)
            paymentVerifier.verify(
                    orderKey,
                    paymentKey,
                    BigDecimal.valueOf(amount),
                    confirmResponse
            );

            // 3. 필요 시 PG 조회 기반 재검증까지 한 번 더
            TossPaymentResponse paymentResponse = paymentService.getPayment(paymentKey);

            paymentVerifier.verify(
                    orderKey,
                    paymentKey,
                    BigDecimal.valueOf(amount),
                    paymentResponse
            );

            // 4. 검증 통과 시 성공 확정
            Long confirmedOrderId = finalizeSuccessTx.finalizeSuccess(orderKey, paymentKey);
            return PaymentConfirmResponse.success(confirmedOrderId, orderKey);

        } catch (WebClientResponseException e) {
            // HTTP 4xx/5xx 응답을 받은 경우
            finalizeFailTx.finalizeFailure(orderKey, e);
            throw new CustomException(ErrorCode.PAYMENT_FAIL);

        } catch (WebClientRequestException e) {
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_PENDING);

        } catch (CustomException e) {
            // 재검증 실패 포함
            finalizeFailTx.finalizeFailure(orderKey, e);
            throw e;
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