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

    public DatasetIngestor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {

        Integer existingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refined_network_metrics",
                Integer.class
        );

        if (existingCount != null && existingCount > 0) {

            log.info("=================================");
            log.info("DATA ALREADY EXISTS");
            log.info("TOTAL ROWS : {}", existingCount);
            log.info("SKIPPING DATA INGESTION");
            log.info("=================================");

            return;
        }

        long start = System.currentTimeMillis();

        log.info("STARTING DATASET INGESTION...");

        BufferedReader br = new BufferedReader(
                new InputStreamReader(csvFile.getInputStream())
        );

        br.readLine();

        String sql = """
        INSERT INTO refined_network_metrics (
            timestamp,
            hour_of_day,
            is_peak_hour,
            region,
            state,
            city,
            network_band,
            environment_type,
            avg_latency_ms,
            download_speed_mbps,
            upload_speed_mbps,
            packet_loss_pct,
            active_users,
            network_utilization_pct,
            congestion_level,
            dropped_calls,
            weather_condition,
            quality_score,
            device_model,
            carrier
        )
        VALUES (
            ?,?,?,?,?,?,?,?,?,?,
            ?,?,?,?,?,?,?,?,?,?
        )
        """;

        List<Object[]> batchArgs = new ArrayList<>();

        String line;

        int success = 0;
        int failed = 0;

        while ((line = br.readLine()) != null) {

            try {

                String[] data =
                        line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                batchArgs.add(new Object[]{

                        Timestamp.valueOf(data[0]),

                        Integer.parseInt(data[21]),

                        Integer.parseInt(data[30]),

                        data[23],

                        data[22],

                        data[1],

                        data[10],

                        data[24],

                        Double.parseDouble(data[5]),

                        Double.parseDouble(data[3]),

                        Double.parseDouble(data[4]),

                        Double.parseDouble(data[26]),

                        Integer.parseInt(data[25]),

                        Double.parseDouble(data[28]),

                        data[18],

                        Integer.parseInt(data[20]),

                        data[27],

                        Double.parseDouble(data[29]),

                        data[8],

                        data[9]
                });

                success++;

            } catch (Exception e) {

                failed++;
            }
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);

        long end = System.currentTimeMillis();

        log.info("=================================");
        log.info("TOTAL INSERTED : {}", success);
        log.info("TOTAL FAILED   : {}", failed);
        log.info("TIME TAKEN     : {} seconds",
                (end - start) / 1000);
        log.info("=================================");
    }
}