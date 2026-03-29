package com.allforone.starvestop.domain.notification.scheduler;

import com.allforone.starvestop.domain.notification.service.NotificationJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class SubscriptionTimeNotificationScheduler {

    private final NotificationJobService notificationJobService;
    private final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sweepPendingNotifications() {
        LocalDateTime now = LocalDateTime.now(KST);
        notificationJobService.sendNotificationJob(now);
    }
}
