package com.allforone.starvestop.domain.notification.service;

import com.allforone.starvestop.domain.notification.dto.FcmMessageResult;
import com.allforone.starvestop.domain.notification.dto.NotificationTargetDto;
import com.allforone.starvestop.domain.notification.entity.NotificationJob;
import com.allforone.starvestop.domain.notification.enums.DayBit;
import com.allforone.starvestop.domain.notification.enums.JobStatus;
import com.allforone.starvestop.domain.notification.repository.NotificationJobJdbcRepository;
import com.allforone.starvestop.domain.notification.repository.NotificationJobRepository;
import com.allforone.starvestop.domain.notification.repository.UserNotificationRepository;
import com.allforone.starvestop.domain.subscription.entity.SubscriptionTime;
import com.allforone.starvestop.domain.subscription.repository.SubscriptionTimeRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationJobService {

    private final ClearNotificationTokenService clearTokenService;
    private final UserNotificationRepository userNotificationRepository;
    private final NotificationJobJdbcRepository notificationJobJdbcRepository;
    private final NotificationJobRepository notificationJobRepository;
    private final SubscriptionTimeRepository subscriptionTimeRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final FcmSender fcmSender;

    @Transactional
    public void preload() {
        int dayBit = DayBit.todayBit();
        LocalDate today = LocalDate.now(KST);

        final int pageSize = 5000;
        long cursor = 0L;

        while (true) {

            List<NotificationTargetDto> targets =
                    userNotificationRepository.findByTargetList(dayBit, cursor, pageSize);

            if (targets.isEmpty()) return;

            // IN절을 위한 구독 아이디 추출
            List<Long> subscriptionIdList = targets.stream()
                    .map(NotificationTargetDto::subscriptionId)
                    .distinct()
                    .toList();


            List<SubscriptionTime> times = subscriptionTimeRepository.findBySubscriptionIdIn(subscriptionIdList);
            Map<Long, List<SubscriptionTime>> timeMap = times.stream()
                    .collect(Collectors.groupingBy(t -> t.getSubscription().getId()));

            //위의 데이터 조립
            List<NotificationJob> jobs = targets.stream()
                    .flatMap( t-> {
                        List<SubscriptionTime> subTimes = timeMap.getOrDefault(t.subscriptionId(), Collections.emptyList());

                        return subTimes.stream().map(st -> {
                            LocalDateTime targetTime = LocalDateTime.of(today, st.getPickupTime()).minusMinutes(30);
                            return new NotificationJob(t.userId(), t.token(), t.subscriptionName(), targetTime);
                        });
                    })
                    .filter(j -> j.getToken() != null && !j.getToken().isBlank())
                    .toList();

            if (!jobs.isEmpty()) {
                try {
                    notificationJobJdbcRepository.bulkInsertIgnore(jobs);
                } catch (DataIntegrityViolationException e) {
                    //유니크 충돌 무시
                }
            }

            long nextCursor = targets.get(targets.size() - 1).cursorId();
            if (!(nextCursor > cursor)) break;
            cursor = nextCursor;
        }
    }

    public void sendNotificationJob(LocalDateTime now) {
        while (true) {
            // PENDING 상태이며 타켓 시간이 지난 알림 500개씩 조회
            List<NotificationJob> jobList = notificationJobRepository.findTop500ByStatusAndTargetDatetimeLessThanEqualOrderByIdAsc(JobStatus.PENDING, now);

            if (jobList.isEmpty()) {
                break;
            }

            List<Message> messageList = getMessageList(jobList);

            try {
                // 비동기로 변경하여 네트워크 대기 시간 최소화
                // .get()을 통해 현재 chunk 발송이 끝날 때까지만 대기
                BatchResponse response = FirebaseMessaging.getInstance().sendEachAsync(messageList).get();

                List<String> invalidTokenList = new ArrayList<>();
                List<Long> successIdList = new ArrayList<>();
                List<Long> invalidIdList = new ArrayList<>();

                for (int i = 0; i < response.getResponses().size(); ++i) {
                    SendResponse r = response.getResponses().get(i);
                    Long jobId = jobList.get(i).getId();

                    if (r.isSuccessful()) {
                        successIdList.add(jobId);
                    } else {
                        MessagingErrorCode code = r.getException().getMessagingErrorCode();
                        if (code != null && (code.equals(MessagingErrorCode.UNREGISTERED)
                                || code.equals(MessagingErrorCode.INVALID_ARGUMENT))) {
                            invalidTokenList.add(jobList.get(i).getToken());
                            invalidIdList.add(jobId);
                        }
                    }
                }

                if (!successIdList.isEmpty()) {
                    notificationJobRepository.updateStatusByIds(JobStatus.SUCCESS, successIdList);
                }
                if (!invalidIdList.isEmpty()) {
                    notificationJobRepository.updateStatusByIds(JobStatus.INVALID_TOKEN, invalidIdList);
                }

                // 기존 무효 토큰 삭제 로직
                if (!invalidTokenList.isEmpty()) {
                    clearTokenService.invalidToken(invalidTokenList);
                    userNotificationRepository.deleteByTokenIn(invalidTokenList);
                }

            } catch (Exception e) {
                log.error("[FCM] 발송 중 에러 발생", e);
                break;
            }
        }

    }

    private List<Message> getMessageList(List<NotificationJob> jobList) {
        return jobList.stream()
                .map(job -> Message.builder()
                        .setToken(job.getToken())
                        .setNotification(Notification.builder()
                                .setTitle("Starve stop")
                                .setBody(job.getSubscriptionName() + " 상품 픽업 30분 전입니다")
                                .build())
                        .build()
                ).toList();
    }

    public void asyncTest(LocalDateTime now){
        long startTime = System.currentTimeMillis();
        while (true){
            List<NotificationJob> jobList = notificationJobRepository.findTop500ByStatusAndTargetDatetimeLessThanEqualOrderByIdAsc(JobStatus.PENDING, now);

            if (jobList.isEmpty()) {
                break;
            }

            List<Long> processingIdList = jobList.stream().map(NotificationJob::getId).toList();
            notificationJobRepository.updateStatusByIds(JobStatus.PROCESSING, processingIdList);

            List<Message> messageList = getMessageList(jobList);

            fcmSender.sendAllAsync(messageList)
                    .thenAccept(resultList -> {
                        List<String> invalidTokenList = new ArrayList<>();
                        List<Long> successIdList = new ArrayList<>();
                        List<Long> invalidIdList = new ArrayList<>();

                        for (int i = 0; i < resultList.size(); ++i) {
                            FcmMessageResult result = resultList.get(i);
                            NotificationJob job = jobList.get(i);

                            if (result.isSuccessful()) {
                                successIdList.add(job.getId());
                            } else {
                                if ("UNREGISTERED".equals(result.getErrorCode())) {
                                    invalidTokenList.add(job.getToken());
                                    invalidIdList.add(job.getId());
                                }
                            }
                        }

                        if (!successIdList.isEmpty()) {
                            notificationJobRepository.updateStatusByIds(JobStatus.SUCCESS, successIdList);
                        }
                        if (!invalidIdList.isEmpty()) {
                            notificationJobRepository.updateStatusByIds(JobStatus.INVALID_TOKEN, invalidIdList);
                        }

                        // 기존 무효 토큰 삭제 로직
                        if (!invalidTokenList.isEmpty()) {
                            clearTokenService.invalidToken(invalidTokenList);
                            userNotificationRepository.deleteByTokenIn(invalidTokenList);
                        }
                        long cycleTime = System.currentTimeMillis() - startTime;

                        log.info("--- 비동기 처리 소요시간 : {}ms---", cycleTime);
                    })
                    .exceptionally(e -> {
                        log.error("비동기 발송 콜백 중 에러 발생", e);
                        return null;
                    });
        }
        long totalTime = System.currentTimeMillis() - startTime;

        log.info("--- api 비동기처리 소요시간 : {}ms---", totalTime);
    }


    public void syncTest(LocalDateTime now) {
        long startTime = System.currentTimeMillis();
        while (true){
            List<NotificationJob> jobList = notificationJobRepository
                    .findTop500ByStatusAndTargetDatetimeLessThanEqualOrderByIdAsc(JobStatus.PENDING, now);

            if (jobList.isEmpty()) break;

            List<Message> messageList = getMessageList(jobList);

            try {

                List<FcmMessageResult> resultList = fcmSender.sendAllSync(messageList);

                // ==========================================

                List<String> invalidTokenList = new ArrayList<>();
                List<Long> successIdList = new ArrayList<>();
                List<Long> invalidIdList = new ArrayList<>();

                for (int i = 0; i < resultList.size(); ++i) {
                    FcmMessageResult result = resultList.get(i);
                    NotificationJob job = jobList.get(i);

                    if (result.isSuccessful()) {
                        successIdList.add(job.getId());
                    } else {
                        if (result.getErrorCode().equals("UNREGISTERED")) {
                            invalidTokenList.add(job.getToken());
                            invalidIdList.add(job.getId());
                        }
                    }
                }

                if (!successIdList.isEmpty()) {
                    notificationJobRepository.updateStatusByIds(JobStatus.SUCCESS, successIdList);
                }
                if (!invalidIdList.isEmpty()) {
                    notificationJobRepository.updateStatusByIds(JobStatus.INVALID_TOKEN, invalidIdList);
                }

                // 기존 무효 토큰 삭제 로직
                if (!invalidTokenList.isEmpty()) {
                    clearTokenService.invalidToken(invalidTokenList);
                    userNotificationRepository.deleteByTokenIn(invalidTokenList);
                }

            } catch (Exception e) {
                log.error("[FCM] 발송 중 에러 발생", e);
                break;
            }
            long cycleTime = System.currentTimeMillis() - startTime;
            log.info("--- 동기 처리 소요시간 : {}ms---", cycleTime);
        }
        long totalTime = System.currentTimeMillis() - startTime;

        log.info("--- api 동기처리 소요시간 : {}ms---", totalTime);
    }

    @Transactional
    public int deleteOldJobs(List<JobStatus> jobStatusList, LocalDateTime now) {
        return notificationJobRepository.deleteOldJobs(jobStatusList, now);
    }

    public void recoverZombieJobs(LocalDateTime now) {
        LocalDateTime targetTime = now.minusMinutes(10);

        int recoverCount = notificationJobRepository.recoverZombieJobs(
                JobStatus.PROCESSING,
                JobStatus.PENDING,
                targetTime
        );

        if (recoverCount > 0) {
            log.warn("--- 좀비 작업 {}건 PENDING으로 전환 ---", recoverCount);
        }
    }
}
