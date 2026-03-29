package com.allforone.starvestop.domain.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class PickupTimeDto {
    private String name;
    private LocalTime pickupTime;
}
