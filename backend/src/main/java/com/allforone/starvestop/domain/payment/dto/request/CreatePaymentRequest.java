package com.allforone.starvestop.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "결제 생성 요청")
public record CreatePaymentRequest(
        @Schema(description = "주문 ID", example = "100")
        @NotNull
        Long orderId
) {
}