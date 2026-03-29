package com.allforone.starvestop.domain.subscription.dto.response;

import com.allforone.starvestop.domain.subscription.dto.PickupTimeDto;
import com.allforone.starvestop.domain.subscription.entity.Subscription;
import com.allforone.starvestop.domain.subscription.enums.Day;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class GetSubscriptionResponse {
    private final Long id;
    private final Long storeId;
    private final String storeName;
    private final Point location;
    private final String name;
    private final String description;
    private final List<Day> dayList;
    private final List<PickupTimeDto> mealTimeList;
    private final BigDecimal price;
    private final Integer stock;
    private final boolean isJoinable;

    public static GetSubscriptionResponse from(Subscription subscription, List<PickupTimeDto> timeDtoList) {
        List<Day> dayList = Day.from(subscription.getDay());
        return new GetSubscriptionResponse(
                subscription.getId(),
                subscription.getStore().getId(),
                subscription.getStore().getName(),
                subscription.getStore().getLocation(),
                subscription.getName(),
                subscription.getDescription(),
                dayList,
                timeDtoList,
                subscription.getPrice(),
                subscription.getStock(),
                subscription.isJoinable()
        );
    }
}
