package com.allforone.starvestop.domain.subscription.repository;

import com.allforone.starvestop.domain.subscription.entity.SubscriptionTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionTimeRepository extends JpaRepository<SubscriptionTime, Long> {
    List<SubscriptionTime> findBySubscriptionIdIn(List<Long> attr0);

    List<SubscriptionTime> findBySubscriptionId(Long id);
}
