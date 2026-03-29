package com.allforone.starvestop.domain.notification.repository;

import com.allforone.starvestop.domain.notification.entity.NotificationJob;
import com.allforone.starvestop.domain.notification.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, Long> {
    List<NotificationJob> findTop500ByStatusAndTargetDatetimeLessThanEqualOrderByIdAsc(JobStatus status, LocalDateTime now);

    @Transactional
    @Modifying
    @Query("UPDATE NotificationJob n SET n.status = :status WHERE n.id IN :idList")
    void updateStatusByIds(JobStatus status, List<Long> idList);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NotificationJob n WHERE n.status IN :statusList AND n.targetDatetime <:datetime")
    int deleteOldJobs(List<JobStatus> statusList, LocalDateTime datetime);

    @Transactional
    @Modifying
    @Query("""
                UPDATE NotificationJob n
                SET n.status = :newStatus
                WHERE n.status = :oldStatus
                  AND n.updatedAt <= :targetTime
            """)
    int recoverZombieJobs(JobStatus oldStatus, JobStatus newStatus, LocalDateTime targetTime);
}
