package com.allforone.starvestop.domain.notification.service;

import com.allforone.starvestop.common.exception.CustomException;
import com.allforone.starvestop.common.exception.ErrorCode;
import com.allforone.starvestop.domain.notification.dto.FcmMessageResult;
import com.google.firebase.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@Profile({"local"})
public class DummyFcmSender implements FcmSender {

    private final Random random = new Random();

    private static final int MEAN_LATENCY = 700; // 평균 지연시간 700ms
    private static final int STD_DEV = 100; // 표준 편차 100ms

    @Override
    public CompletableFuture<List<FcmMessageResult>> sendAllAsync(List<Message> messageList) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long simulatedDelay = (long) (random.nextGaussian() * STD_DEV + MEAN_LATENCY);

                // 각각 측정 결과 최소 최대값 세팅.
                simulatedDelay = Math.max(400, Math.min(1000, simulatedDelay));

                // 데이터 처리 시 네트워크 오버헤드 만큼 쓰레드 대기.
                Thread.sleep(simulatedDelay);

                return generateMockResultList(messageList.size());
            } catch (InterruptedException e) {
                throw new CustomException(ErrorCode.ASYNC_ERROR);
            }
        });
    }

    @Override
    public List<FcmMessageResult> sendAllSync(List<Message> messageList) throws InterruptedException {
        List<FcmMessageResult> resultList = new ArrayList<>();
        long simulatedDelay = (long) (random.nextGaussian() * STD_DEV + MEAN_LATENCY);

        simulatedDelay = Math.max(400, Math.min(1000, simulatedDelay));

        Thread.sleep(simulatedDelay);

        for (int i = 0; i < messageList.size(); i++) {
            boolean isSuccess = random.nextInt(100) > 0;
            resultList.add(new FcmMessageResult(isSuccess, isSuccess ? null : "UNREGISTERED"));
        }
        return resultList;
    }

    private List<FcmMessageResult> generateMockResultList(int size) {
        List<FcmMessageResult> resultList = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            boolean isSuccess = random.nextInt(100) > 0;
            resultList.add(new FcmMessageResult(isSuccess, isSuccess ? null : "UNREGISTERED"));
        }
        return resultList;
    }
}
