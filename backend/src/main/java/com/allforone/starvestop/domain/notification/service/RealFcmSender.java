package com.allforone.starvestop.domain.notification.service;

import com.allforone.starvestop.common.exception.CustomException;
import com.allforone.starvestop.common.exception.ErrorCode;
import com.allforone.starvestop.domain.notification.dto.FcmMessageResult;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@Profile({"dev"})
public class RealFcmSender implements FcmSender {
    @Override
    public CompletableFuture<List<FcmMessageResult>> sendAllAsync(List<Message> messageList) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long networkStart = System.currentTimeMillis();
                BatchResponse response = FirebaseMessaging.getInstance().sendEachAsync(messageList).get();
                long networkTime = System.currentTimeMillis() - networkStart;
                log.info("--- 처리완료. 네트워크 오버헤드 : {}ms", networkTime);
                return mapToCustomResult(response);
            } catch (InterruptedException e) {
                throw new CustomException(ErrorCode.ASYNC_ERROR);
            } catch (Exception e) {
                throw new CustomException(ErrorCode.NOTIFICATION_SEND_FAIL);
            }
        });
    }

    @Override
    public List<FcmMessageResult> sendAllSync(List<Message> messageList) throws InterruptedException, FirebaseMessagingException {
        long networkStart = System.currentTimeMillis();
        BatchResponse response = FirebaseMessaging.getInstance().sendEach(messageList);
        long networkTime = System.currentTimeMillis() - networkStart;
        log.info("--- 처리완료. 동기처리 오버헤드 : {}ms", networkTime);
        return mapToCustomResult(response);
    }

    private List<FcmMessageResult> mapToCustomResult(BatchResponse response) {
        return response.getResponses().stream()
                .map(res -> {
                    if (res.isSuccessful()) {

                        return new FcmMessageResult(true, null);
                    }

                    String errorCode = "UNKNOWN_ERROR";
                    if (res.getException() != null && res.getException().getMessagingErrorCode() != null) {
                        errorCode = res.getException().getMessagingErrorCode().name();
                    } else if (res.getException() != null && res.getException().getErrorCode() != null) {
                        // MessagingErrorCode가 없으면 상위 ErrorCode라도 가져옴
                        errorCode = res.getException().getErrorCode().name();
                    }

                    return new FcmMessageResult(false, errorCode);
                })
                .toList();
    }

}
