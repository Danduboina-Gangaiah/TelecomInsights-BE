package com.telecom.insights.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =====================================================
    // KPI CARDS
    // =====================================================

    public Map<String, Object> getAggregateKPIs() {

        String sql = """
            SELECT
                ROUND(AVG(quality_score), 2) AS quality_score,
                SUM(dropped_calls) AS total_dropped_calls,
                ROUND(AVG(avg_latency_ms), 2) AS avg_latency,
                ROUND(AVG(download_speed_mbps), 2) AS avg_download
            FROM refined_network_metrics
        """;

        return jdbcTemplate.queryForMap(sql);
    }

    // =====================================================
    // 24 HOUR TREND
    // =====================================================

    public List<Map<String, Object>> getHourlyUtilizationTrend() {

        String sql = """
            SELECT
                hour_of_day,
                ROUND(AVG(download_speed_mbps), 2) AS download_speed_mbps,
                ROUND(AVG(avg_latency_ms), 2) AS avg_latency_ms
            FROM refined_network_metrics
            GROUP BY hour_of_day
            ORDER BY hour_of_day
        """;

        return jdbcTemplate.queryForList(sql);
    }

    // =====================================================
    // BAND ANALYSIS
    // =====================================================

    public List<Map<String, Object>> getBandPerformance() {

        String sql = """
            SELECT
                network_band,
                SUM(dropped_calls) AS dropped_calls
            FROM refined_network_metrics
            GROUP BY network_band
            ORDER BY dropped_calls DESC
        """;

        return jdbcTemplate.queryForList(sql);
    }

    // =====================================================
    // WORST STATES
    // =====================================================

    public List<Map<String, Object>> getWorstStates() {

        String sql = """
            SELECT
                state,
                SUM(dropped_calls) AS total_dropped_calls,
                ROUND(AVG(packet_loss_pct), 2) AS avg_packet_loss
            FROM refined_network_metrics
            GROUP BY state
            ORDER BY total_dropped_calls DESC
            LIMIT 5
        """;

        return jdbcTemplate.queryForList(sql);
    }

    // =====================================================
    // CARRIER SCORES
    // =====================================================

    public List<Map<String, Object>> getCarrierScores() {

        String sql = """
            SELECT
                carrier,
                ROUND(AVG(quality_score), 2) AS quality_score
            FROM refined_network_metrics
            GROUP BY carrier
            ORDER BY quality_score DESC
        """;

        return jdbcTemplate.queryForList(sql);
    }

    // =====================================================
    // CARRIER PERFORMANCE
    // =====================================================

    public List<Map<String, Object>> getCarrierPerformance() {

        String sql = """
            SELECT
                carrier AS region,
                ROUND(AVG(download_speed_mbps), 2) AS download_speed_mbps
            FROM refined_network_metrics
            GROUP BY carrier
            ORDER BY download_speed_mbps DESC
            LIMIT 5
        """;

        return jdbcTemplate.queryForList(sql);
    }
}