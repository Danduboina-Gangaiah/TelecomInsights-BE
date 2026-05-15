package com.telecom.insights.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

@Component
public class DatasetIngestor implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DatasetIngestor.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("classpath:data/refined_network_metrics.csv")
    private Resource csvFile;

    public DatasetIngestor(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {

        log.info("CHECKING EXISTING DATA...");

        Integer existingCount =
                jdbcTemplate.queryForObject(

                        "SELECT COUNT(*) FROM refined_network_metrics",

                        Integer.class
                );

        // =====================================================
        // SKIP IF DATA ALREADY EXISTS
        // =====================================================

        if (existingCount != null && existingCount > 0) {

            log.info("=================================");
            log.info("DATA ALREADY EXISTS IN DATABASE");
            log.info("SKIPPING INGESTION");
            log.info("TOTAL ROWS : {}", existingCount);
            log.info("=================================");

            return;
        }

        long start = System.currentTimeMillis();

        log.info("STARTING TELECOM DATASET INGESTION...");

        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        csvFile.getInputStream()
                )
        );

        // Skip CSV Header
        br.readLine();

        String sql = """
                
                INSERT INTO refined_network_metrics (
                
                    timestamp,
                    city,
                    signal_strength_dbm,
                    download_speed_mbps,
                    upload_speed_mbps,
                    avg_latency_ms,
                    jitter_ms,
                    network_type,
                    device_model,
                    carrier,
                    network_band,
                    battery_level_pct,
                    temperature_c,
                    connected_duration_min,
                    handover_count,
                    data_usage_mb,
                    video_streaming_quality,
                    vonr_enabled,
                    congestion_level,
                    ping_to_google_ms,
                    dropped_calls,
                    hour_of_day,
                    state,
                    region,
                    environment_type,
                    active_users,
                    packet_loss_pct,
                    weather_condition,
                    network_utilization_pct,
                    quality_score,
                    is_peak_hour
                
                )
                
                VALUES (
                
                    ?,?,?,?,?,?,?,?,?,?,
                    ?,?,?,?,?,?,?,?,?,?,
                    ?,?,?,?,?,?,?,?,?,?,
                    ?
                
                )
                """;

        String line;

        int batchSize = 1000;

        List<Object[]> batchArgs = new ArrayList<>();

        int success = 0;
        int failed = 0;

        while ((line = br.readLine()) != null) {

            try {

                String[] data =
                        line.split(
                                ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"
                        );

                batchArgs.add(new Object[]{

                        // 0
                        Timestamp.valueOf(data[0]),

                        // 1
                        data[1],

                        // 2
                        Double.parseDouble(data[2]),

                        // 3
                        Double.parseDouble(data[3]),

                        // 4
                        Double.parseDouble(data[4]),

                        // 5
                        Double.parseDouble(data[5]),

                        // 6
                        Double.parseDouble(data[6]),

                        // 7
                        data[7],

                        // 8
                        data[8],

                        // 9
                        data[9],

                        // 10
                        data[10],

                        // 11
                        Double.parseDouble(data[11]),

                        // 12
                        Double.parseDouble(data[12]),

                        // 13
                        Double.parseDouble(data[13]),

                        // 14
                        Integer.parseInt(data[14]),

                        // 15
                        Double.parseDouble(data[15]),

                        // 16
                        data[16],

                        // 17
                        Boolean.parseBoolean(data[17]),

                        // 18
                        data[18],

                        // 19
                        Double.parseDouble(data[19]),

                        // 20
                        Integer.parseInt(data[20]),

                        // 21
                        Integer.parseInt(data[21]),

                        // 22
                        data[22],

                        // 23
                        data[23],

                        // 24
                        data[24],

                        // 25
                        Integer.parseInt(data[25]),

                        // 26
                        Double.parseDouble(data[26]),

                        // 27
                        data[27],

                        // 28
                        Double.parseDouble(data[28]),

                        // 29
                        Double.parseDouble(data[29]),

                        // 30
                        Boolean.parseBoolean(data[30])

                });

                // =====================================================
                // BATCH INSERT
                // =====================================================

                if (batchArgs.size() >= batchSize) {

                    jdbcTemplate.batchUpdate(
                            sql,
                            batchArgs
                    );

                    success += batchArgs.size();

                    log.info(
                            "BATCH INSERTED : {} ROWS",
                            success
                    );

                    batchArgs.clear();
                }

            } catch (Exception e) {

                failed++;

                log.error(
                        "FAILED ROW : {}",
                        line
                );

                log.error(
                        "ERROR : {}",
                        e.getMessage()
                );
            }
        }

        // =====================================================
        // INSERT REMAINING ROWS
        // =====================================================

        if (!batchArgs.isEmpty()) {

            jdbcTemplate.batchUpdate(
                    sql,
                    batchArgs
            );

            success += batchArgs.size();
        }

        long end = System.currentTimeMillis();

        // =====================================================
        // FINAL LOGS
        // =====================================================

        log.info("=================================");
        log.info("DATASET INGESTION COMPLETED");
        log.info("TOTAL INSERTED : {}", success);
        log.info("TOTAL FAILED   : {}", failed);
        log.info(
                "TIME TAKEN     : {} seconds",
                (end - start) / 1000
        );
        log.info("=================================");
    }
}