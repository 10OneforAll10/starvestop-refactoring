package com.allforone.starvestop.domain.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class SubscriptionTimesDto {
    @NotBlank(message = "구독 시간명을 입력해주세요")
    private String name;
    @NotNull(message = "구독 상품 수령 시간을 입력해주세요")
    private LocalTime pickupTime;

}
