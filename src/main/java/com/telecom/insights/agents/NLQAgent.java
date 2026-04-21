package com.telecom.insights.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NLQAgent {

    private final ChatClient chatClient;
    private final JdbcTemplate jdbcTemplate;

    public NLQAgent(ChatClient.Builder builder,
                    JdbcTemplate jdbcTemplate) {
        this.chatClient = builder.build();
        this.jdbcTemplate = jdbcTemplate;
    }

    public String processQuery(String question) {
        try {
            String q = question.toLowerCase();
            String sql = "";

            if (q.contains("highest latency")) {
                sql = """
                        SELECT region, latency
                        FROM network_metrics
                        ORDER BY latency DESC
                        LIMIT 1
                        """;
                return jdbcTemplate.queryForList(sql).toString();
            }

            if (q.contains("lowest latency")) {
                sql = """
                        SELECT region, latency
                        FROM network_metrics
                        ORDER BY latency ASC
                        LIMIT 1
                        """;
                return jdbcTemplate.queryForList(sql).toString();
            }

            if (q.contains("download")) {
                sql = """
                        SELECT region, download_speed
                        FROM network_metrics
                        ORDER BY download_speed DESC
                        LIMIT 1
                        """;
                return jdbcTemplate.queryForList(sql).toString();
            }

            if (q.contains("upload")) {
                sql = """
                        SELECT region, upload_speed
                        FROM network_metrics
                        ORDER BY upload_speed DESC
                        LIMIT 1
                        """;
                return jdbcTemplate.queryForList(sql).toString();
            }

            if (q.contains("best signal")) {
                sql = """
                        SELECT region, signal_strength
                        FROM network_metrics
                        ORDER BY signal_strength DESC
                        LIMIT 1
                        """;
                return jdbcTemplate.queryForList(sql).toString();
            }

            if (q.contains("weak signal")) {
                sql = """
                        SELECT region, signal_strength
                        FROM network_metrics
                        ORDER BY signal_strength ASC
                        LIMIT 1
                        """;
                return jdbcTemplate.queryForList(sql).toString();
            }

            if (q.contains("dropped")) {
                sql = """
                        SELECT region, COUNT(*) AS dropped_count
                        FROM network_metrics
                        WHERE dropped_connection = true
                        GROUP BY region
                        ORDER BY dropped_count DESC
                        LIMIT 1
                        """;
                return jdbcTemplate.queryForList(sql).toString();
            }

            return "No matching query found.";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}