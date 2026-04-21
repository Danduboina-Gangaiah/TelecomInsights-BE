package com.telecom.insights.rag;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;

@Component
public class DatasetIngestor {

    private final JdbcTemplate jdbcTemplate;

    public DatasetIngestor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)

    public void loadCsv() {
        try {

            jdbcTemplate.execute("DROP TABLE IF EXISTS network_metrics");

            jdbcTemplate.execute("""
            CREATE TABLE network_metrics (
                id SERIAL PRIMARY KEY,
                region VARCHAR(100),
                signal_strength NUMERIC,
                download_speed NUMERIC,
                upload_speed NUMERIC,
                latency NUMERIC,
                jitter NUMERIC,
                carrier VARCHAR(100),
                network_type VARCHAR(50),
                dropped_connection VARCHAR(20)
            )
        """);

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM network_metrics",
                    Integer.class
            );

            if (count != null && count > 0) return;

            Reader reader = new InputStreamReader(
                    new ClassPathResource("5g_network_data.csv").getInputStream()
            );

            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);

            for (CSVRecord r : parser) {

                jdbcTemplate.update("""
                    INSERT INTO network_metrics(
                        region,
                        signal_strength,
                        download_speed,
                        upload_speed,
                        latency,
                        jitter,
                        carrier,
                        network_type,
                        dropped_connection
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                        r.get("Location"),
                        Double.parseDouble(r.get("Signal Strength (dBm)")),
                        Double.parseDouble(r.get("Download Speed (Mbps)")),
                        Double.parseDouble(r.get("Upload Speed (Mbps)")),
                        Double.parseDouble(r.get("Latency (ms)")),
                        Double.parseDouble(r.get("Jitter (ms)")),
                        r.get("Carrier"),
                        r.get("Network Type"),
                        r.get("Dropped Connection")
                );
            }

            System.out.println("5G CSV dataset loaded successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}