package com.allforone.starvestop.domain.subscription.entity;

import com.allforone.starvestop.common.entity.BaseEntity;
import com.allforone.starvestop.common.exception.CustomException;
import com.allforone.starvestop.common.exception.ErrorCode;
import com.allforone.starvestop.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int day;

    @Column(nullable = false)
    private BigDecimal price;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubscriptionTime> subscriptionTimes = new ArrayList<>();

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private boolean isJoinable;


    public Subscription(Store store, String name, String description, int day, BigDecimal price, Integer stock) {
        this.store = store;
        this.name = name;
        this.description = description;
        this.day = day;
        this.price = price;
        this.stock = stock;
        this.isJoinable = true;
    }

    public static Subscription create(Store store, String subscriptionName, String description, int day, BigDecimal price, Integer stock) {
        return new Subscription(store, subscriptionName, description, day, price, stock);
    }

    public void addTime(String name, LocalTime pickupTime) {
        this.subscriptionTimes.add(new SubscriptionTime(this, name, pickupTime));
    }

    public void changeIsJoinable(boolean joinable) {
        this.isJoinable = joinable;
    }

    public void decrease(Integer count) {
        if (this.stock == 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.stock -= count;
    }

    public void increase(Integer count) {
        this.stock += count;
    }
}
