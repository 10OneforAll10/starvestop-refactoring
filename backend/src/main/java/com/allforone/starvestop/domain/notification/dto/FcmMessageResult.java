package com.allforone.starvestop.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FcmMessageResult {
    boolean isSuccessful;
    String errorCode; // 에러 발생 시 원인
}
