package com.allforone.starvestop.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    INVALID_TOKEN
}
