package com.telecom.insights.rag;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Component
public class DatasetIngestor {

    private static final Logger logger =
            LoggerFactory.getLogger(DatasetIngestor.class);

    private final JdbcClient jdbcClient;

    @Value("classpath:data/5g_network_data.csv")
    private Resource csvFile;

    public DatasetIngestor(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadDataOnStartup() {

        createTableIfNotExists();

        Long count = jdbcClient
                .sql("SELECT COUNT(*) FROM network_metrics")
                .query(Long.class)
                .single();

        if (count > 0) {
            logger.info("Database already contains {} records. Skipping CSV ingestion.", count);
            return;
        }

        logger.info("Starting CSV data ingestion into network_metrics...");

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        csvFile.getInputStream(),
                                        StandardCharsets.UTF_8));

                CSVParser csvParser =
                        new CSVParser(
                                reader,
                                CSVFormat.DEFAULT
                                        .withFirstRecordAsHeader()
                                        .withIgnoreHeaderCase()
                                        .withTrim())
        ) {

            for (CSVRecord record : csvParser) {

                try {

                    jdbcClient.sql("""
                        INSERT INTO network_metrics
                        (
                            "timestamp",
                            region_id,
                            cell_id,
                            avg_latency_ms,
                            download_speed_mbps,
                            upload_speed_mbps,
                            packet_loss_pct,
                            active_users
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """)
                            .params(
                                    record.get("Timestamp"),
                                    record.get("Location"),
                                    record.get("Device Model"),
                                    num(record.get("Latency (ms)")),
                                    num(record.get("Download Speed (Mbps)")),
                                    num(record.get("Upload Speed (Mbps)")),
                                    num(record.get("Jitter (ms)")),
                                    integer(record.get("Ping to Google (ms)"))
                            )
                            .update();

                } catch (Exception e) {
                    logger.warn(
                            "Skipping bad row {} - Reason: {}",
                            record.getRecordNumber(),
                            e.getMessage()
                    );
                }
            }

            logger.info("Successfully loaded data from CSV into PostgreSQL.");

        } catch (Exception e) {
            logger.error("Failed to load CSV data", e);
        }
    }

    private void createTableIfNotExists() {

        jdbcClient.sql("""
            CREATE TABLE IF NOT EXISTS network_metrics(
                id SERIAL PRIMARY KEY,
                "timestamp" VARCHAR(100),
                region_id VARCHAR(100),
                cell_id VARCHAR(100),
                avg_latency_ms NUMERIC,
                download_speed_mbps NUMERIC,
                upload_speed_mbps NUMERIC,
                packet_loss_pct NUMERIC,
                active_users INTEGER
            )
        """).update();
    }

    private BigDecimal num(String val) {
        if (val == null) return BigDecimal.ZERO;

        String cleaned = val.replaceAll("[^0-9.]", "");

        if (cleaned.isBlank()) return BigDecimal.ZERO;

        return new BigDecimal(cleaned);
    }

    private Integer integer(String val) {
        if (val == null) return 0;

        String cleaned = val.replaceAll("[^0-9]", "");

        if (cleaned.isBlank()) return 0;

        return Integer.parseInt(cleaned);
    }
}