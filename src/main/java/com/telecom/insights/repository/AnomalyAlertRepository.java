package com.telecom.insights.repository;

import com.telecom.insights.model.AnomalyAlert;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class AnomalyAlertRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnomalyAlertRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =====================================================
    // SAVE ALERT
    // =====================================================

    public void save(AnomalyAlert alert) {

        String sql = """
                INSERT INTO anomaly_alerts
                (
                    severity,
                    title,
                    message,
                    raw_data
                )
                VALUES (?, ?, ?, CAST(? AS jsonb))
                """;

        jdbcTemplate.update(
                sql,
                alert.getSeverity(),
                alert.getTitle(),
                alert.getMessage(),
                alert.getRawData()
        );
    }

    // =====================================================
    // GET UNREAD ALERTS
    // =====================================================

    public List<Map<String, Object>> getUnreadAlerts() {

        String sql = """
                SELECT *
                FROM anomaly_alerts
                WHERE is_read = false
                ORDER BY created_at DESC
                """;

        return jdbcTemplate.queryForList(sql);
    }

    // =====================================================
    // MARK ALERT AS READ
    // =====================================================

    public void markAsRead(Long id) {

        String sql = """
                UPDATE anomaly_alerts
                SET is_read = true
                WHERE id = ?
                """;

        jdbcTemplate.update(
                sql,
                id
        );
    }

    // =====================================================
    // DUPLICATE ALERT SUPPRESSION
    // =====================================================

    public boolean recentAlertExistsForTopic(
            String titleKeyword,
            int hoursToSuppress
    ) {

        String sql = """
                SELECT COUNT(*)
                FROM anomaly_alerts
                WHERE title LIKE ?
                AND created_at >
                NOW() - CAST(? AS INTERVAL)
                """;

        String intervalValue =
                hoursToSuppress + " hour";

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                "%" + titleKeyword + "%",
                intervalValue
        );

        return count != null && count > 0;
    }
}