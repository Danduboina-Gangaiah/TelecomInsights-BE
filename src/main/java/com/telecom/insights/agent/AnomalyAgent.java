package com.telecom.insights.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.insights.model.AnomalyAlert;
import com.telecom.insights.repository.AnomalyAlertRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AnomalyAgent {

    private static final Logger logger =
            LoggerFactory.getLogger(AnomalyAgent.class);

    private final JdbcTemplate jdbcTemplate;

    private final ChatClient chatClient;

    private final AnomalyAlertRepository alertRepository;

    private final ObjectMapper objectMapper;

    @Value("${anomaly.agent.suppression-hours:0}")
    private int suppressionHours;

    @Value("${anomaly.agent.threshold.quality-score:0.40}")
    private double qualityScoreThreshold;

    @Value("${anomaly.agent.threshold.quality-drops:5}")
    private int qualityDropsThreshold;

    @Value("${anomaly.agent.threshold.latency:100}")
    private int latencyThreshold;

    private final String SYSTEM_PROMPT = """
You are a Telecom Network AI Operations Agent.

Analyze telecom anomaly data and generate
a concise executive operational alert.

RULES:
1. Maximum 4 sentences.
2. Mention root cause.
3. Mention affected city and region.
4. Mention operational impact.
5. Mention recommendation.
6. Never mention JSON or SQL.
7. Use telecom terminology.
8. Sound like a telecom NOC engineer.
9. Be concise and executive-friendly.

RAW DATA:
{raw_data}
""";

    public AnomalyAgent(
            JdbcTemplate jdbcTemplate,
            GoogleGenAiChatModel geminiChatModel,
            AnomalyAlertRepository alertRepository,
            ObjectMapper objectMapper
    ) {

        this.jdbcTemplate = jdbcTemplate;

        this.alertRepository = alertRepository;

        this.objectMapper = objectMapper;

        this.chatClient = ChatClient.create(geminiChatModel);
    }

    // =====================================================
    // AUTO SCHEDULER
    // =====================================================

    @Scheduled(
            fixedRateString =
                    "${anomaly.agent.polling-rate:10000}")
    public void monitorNetworkHealth() {

        logger.info("Anomaly Agent Started...");

        try {

            checkQualityDegradation();

            checkLatencySpikes();

            checkDeviceFailures();

        } catch (Exception e) {

            e.printStackTrace();

            logger.error(
                    "Anomaly Agent Failure: {}",
                    e.getMessage()
            );
        }
    }

    // =====================================================
    // MANUAL TRIGGER
    // =====================================================

    public String runManualCheck() {

        logger.info("Manual anomaly check triggered");

        try {

            monitorNetworkHealth();

            return "Manual anomaly scan completed successfully";

        } catch (Exception e) {

            logger.error(
                    "Manual anomaly scan failed",
                    e
            );

            return "Manual anomaly scan failed";
        }
    }

    // =====================================================
    // QUALITY DEGRADATION DETECTION
    // =====================================================

    private void checkQualityDegradation()
            throws Exception {

        String sql = """
                SELECT
                    region,
                    city,
                    quality_score,
                    dropped_calls,
                    avg_latency_ms
                FROM refined_network_metrics
                WHERE quality_score < ?
                OR dropped_calls > ?
                ORDER BY timestamp DESC
                LIMIT 5
                """;

        List<Map<String, Object>> anomalies =
                jdbcTemplate.queryForList(
                        sql,
                        qualityScoreThreshold,
                        qualityDropsThreshold
                );

        if(anomalies.isEmpty()) {

            logger.info("No quality anomalies detected");

            return;
        }

        Map<String, Object> latest =
                anomalies.get(0);

        String region =
                (String) latest.get("region");

        String city =
                (String) latest.get("city");

        // DUPLICATE SUPPRESSION
        if(alertRepository
                .recentAlertExistsForTopic(
                        city,
                        suppressionHours
                )) {

            logger.info(
                    "Duplicate alert suppressed"
            );

            return;
        }

        logger.warn(
                "Realtime network quality anomaly detected"
        );

        String rawJson =
                objectMapper.writeValueAsString(
                        anomalies
                );

        String finalPrompt =
                SYSTEM_PROMPT.replace(
                        "{raw_data}",
                        rawJson
                );

        String diagnosis =
                chatClient.prompt()
                        .user(finalPrompt)
                        .call()
                        .content();

        AnomalyAlert alert =
                new AnomalyAlert(
                        "CRITICAL",
                        "Quality Degradation - " + city,
                        diagnosis,
                        rawJson
                );

        alertRepository.save(alert);

        logger.info(
                "AI quality alert stored successfully"
        );
    }

    // =====================================================
    // LATENCY SPIKE DETECTION
    // =====================================================

    private void checkLatencySpikes()
            throws Exception {

        String sql = """
                SELECT
                    region,
                    city,
                    avg_latency_ms,
                    quality_score
                FROM refined_network_metrics
                WHERE avg_latency_ms > ?
                ORDER BY timestamp DESC
                LIMIT 5
                """;

        List<Map<String, Object>> faults =
                jdbcTemplate.queryForList(
                        sql,
                        latencyThreshold
                );

        if(faults.isEmpty()) {

            logger.info("No latency spikes detected");

            return;
        }

        Map<String, Object> data =
                faults.get(0);

        String region =
                (String) data.get("region");

        String city =
                (String) data.get("city");

        Number latency =
                (Number) data.get("avg_latency_ms");

        String message = String.format(
                "Critical realtime latency spike detected in %s, %s. Peak latency reached %sms indicating severe congestion or transport degradation.",
                city,
                region,
                latency
        );

        String rawJson =
                objectMapper.writeValueAsString(
                        faults
                );

        AnomalyAlert alert =
                new AnomalyAlert(
                        "WARNING",
                        "Latency Spike - " + city,
                        message,
                        rawJson
                );

        alertRepository.save(alert);

        logger.info(
                "Latency anomaly alert stored successfully"
        );
    }

    // =====================================================
    // DEVICE FAILURE DETECTION
    // =====================================================

    private void checkDeviceFailures()
            throws Exception {

        String sql = """
                SELECT
                    device_model,
                    city,
                    dropped_calls,
                    quality_score
                FROM refined_network_metrics
                WHERE dropped_calls > ?
                ORDER BY timestamp DESC
                LIMIT 5
                """;

        List<Map<String, Object>> faults =
                jdbcTemplate.queryForList(
                        sql,
                        qualityDropsThreshold
                );

        if(faults.isEmpty()) {

            logger.info("No device anomalies detected");

            return;
        }

        Map<String, Object> data =
                faults.get(0);

        String device =
                (String) data.get("device_model");

        String city =
                (String) data.get("city");

        Number drops =
                (Number) data.get("dropped_calls");

        String message = String.format(
                "Abnormal dropped call concentration detected for %s devices in %s. Dropped calls reached %s indicating possible firmware or radio instability.",
                device,
                city,
                drops
        );

        String rawJson =
                objectMapper.writeValueAsString(
                        faults
                );

        AnomalyAlert alert =
                new AnomalyAlert(
                        "WARNING",
                        "Device Failure - " + device,
                        message,
                        rawJson
                );

        alertRepository.save(alert);

        logger.info(
                "Device anomaly alert stored successfully"
        );
    }
}