package com.allforone.starvestop.domain.notification.scheduler;

import com.allforone.starvestop.domain.notification.enums.JobStatus;
import com.allforone.starvestop.domain.notification.service.NotificationJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyNotificationScheduler {

    private final NotificationJobService jobService;


    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void preloadToday() {
        jobService.preload();
    }


    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void cleanupOldNotifications() {
        LocalDateTime oneWeekAgo = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // 상태가 PENDING이 아니면서, 타겟 시간이 7일 이상 지난 데이터 삭제
        int deletedCount = jobService.deleteOldJobs(
                List.of(JobStatus.SUCCESS, JobStatus.INVALID_TOKEN, JobStatus.FAILED),
                oneWeekAgo);

        log.info("알림 발송 로그 {}건 삭제", deletedCount);
    }
}
