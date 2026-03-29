package com.allforone.starvestop.domain.notification.repository;

import com.allforone.starvestop.domain.notification.entity.NotificationJob;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class NotificationJobJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void bulkInsertIgnore(List<NotificationJob> jobs) {
        String sql = """
            INSERT IGNORE INTO notification_jobs
            (user_id, token, subscription_name, target_datetime, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, 'PENDING', NOW(), NOW())
        """;

        jdbcTemplate.batchUpdate(sql, jobs, 1000, (ps, j) -> {
            ps.setLong(1, j.getUserId());
            ps.setString(2, j.getToken());
            ps.setString(3, j.getSubscriptionName());
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(j.getTargetDatetime()));
        });
    }
}
