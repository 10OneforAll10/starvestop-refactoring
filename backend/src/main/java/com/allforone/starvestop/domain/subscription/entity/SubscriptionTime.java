package com.allforone.starvestop.domain.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Entity
@Table(name="subscription_times")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalTime pickupTime;

    public SubscriptionTime(Subscription subscription, String name, LocalTime pickupTime) {
        this.subscription = subscription;
        this.name = name;
        this.pickupTime = pickupTime;
    }
}
