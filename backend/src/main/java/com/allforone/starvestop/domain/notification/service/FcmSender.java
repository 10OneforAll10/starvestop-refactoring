package com.allforone.starvestop.domain.notification.service;

import com.allforone.starvestop.domain.notification.dto.FcmMessageResult;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FcmSender {
    CompletableFuture<List<FcmMessageResult>> sendAllAsync(List<Message> messageList);

    List<FcmMessageResult> sendAllSync(List<Message> messageList) throws InterruptedException, FirebaseMessagingException;
}
