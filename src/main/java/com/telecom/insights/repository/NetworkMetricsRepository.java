package com.telecom.insights.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class NetworkMetricsRepository {

    private final JdbcTemplate jdbcTemplate;

    public NetworkMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> fetchRecentMetrics() {

        String sql = """
            SELECT region_id,
                   AVG(avg_latency_ms) AS avg_latency_ms,
                   AVG(download_speed_mbps) AS download_speed_mbps,
                   AVG(packet_loss_pct) AS packet_loss_pct,
                   SUM(active_users) AS active_users
            FROM network_metrics
            GROUP BY region_id
        """;

        return jdbcTemplate.queryForList(sql);
    }
}