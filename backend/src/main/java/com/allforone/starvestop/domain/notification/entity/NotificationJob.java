package com.allforone.starvestop.domain.notification.entity;


import com.allforone.starvestop.domain.notification.enums.JobStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name="notification_jobs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "nj_target_token_sub",
                        columnNames = {"targetDatetime", "token", "subscriptionName"}
                )
        },
        indexes = {
                @Index(name = "idx_job_status_target_id", columnList = "status, targetDatetime, id"),
                @Index(name = "idx_job_token", columnList = "token")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationJob {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private String subscriptionName;

    @Column(nullable = false)
    private LocalDateTime targetDatetime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public NotificationJob(Long userId, String token, String subscriptionName, LocalDateTime targetDatetime) {
        this.userId = userId;
        this.token = token;
        this.subscriptionName = subscriptionName;
        this.targetDatetime = targetDatetime;
        this.status = JobStatus.PENDING;
    }

    // 상태 변경 메서드
    public void changeStatus(JobStatus newStatus) {
        this.status = status;
    }
}

