package com.telecom.insights.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SchemaMetadataIngestor {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    SchemaMetadataIngestor.class
            );

    private final VectorStore vectorStore;

    public SchemaMetadataIngestor(
            VectorStore vectorStore
    ) {
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ingestSchemaMetadata() {

        logger.info(
                "Initializing telecom schema metadata..."
        );

        String schema = """
====================================================
TELECOM ANALYTICS DATABASE SCHEMA
====================================================

TABLE NAME:
refined_network_metrics

====================================================
AVAILABLE COLUMNS + DATA TYPES
====================================================

timestamp (TIMESTAMP)

hour_of_day (INTEGER)

is_peak_hour (INTEGER)
1 = peak hour
0 = non peak hour

region (VARCHAR)

state (VARCHAR)

city (VARCHAR)

network_band (VARCHAR)

environment_type (VARCHAR)

avg_latency_ms (NUMERIC)

download_speed_mbps (NUMERIC)

upload_speed_mbps (NUMERIC)

packet_loss_pct (NUMERIC)

active_users (INTEGER)

network_utilization_pct (NUMERIC)

congestion_level (VARCHAR)

dropped_calls (INTEGER)

weather_condition (VARCHAR)

quality_score (NUMERIC)

device_model (VARCHAR)

carrier (VARCHAR)

====================================================
BUSINESS DEFINITIONS
====================================================

high download_speed_mbps
= better download performance

high upload_speed_mbps
= better upload performance

high avg_latency_ms
= poor latency

high packet_loss_pct
= unstable network

high dropped_calls
= poor call quality

high network_utilization_pct
= network congestion / heavy traffic

high quality_score
= strong telecom quality

====================================================
KNOWN CARRIERS
====================================================

Verizon
AT&T
T-Mobile
BSNL
Vi

====================================================
KNOWN DEVICES
====================================================

iPhone 14
Galaxy S23
Pixel 7
Nord 4

====================================================
KNOWN REGIONS
====================================================

Northeast
South
Midwest
West

====================================================
KNOWN ENVIRONMENTS
====================================================

Urban
Rural

====================================================
KNOWN NETWORK BANDS
====================================================

4G LTE
5G NSA
5G SA
5G Sub-6

====================================================
POSTGRESQL SQL RULES
====================================================

Use ONLY:
refined_network_metrics

NEVER invent columns

NEVER invent tables

Use PostgreSQL syntax only

Use LOWER(column_name)
for case-insensitive comparisons

Use ILIKE
for text searches

Use GROUP BY
for aggregate queries

Use ORDER BY DESC
for highest/top/best queries

Use ORDER BY ASC
for lowest/worst queries

Use LIMIT
for ranking/top queries

====================================================
IMPORTANT COLUMN RULES
====================================================

is_peak_hour is INTEGER
NOT BOOLEAN

Correct:
is_peak_hour = 1

Wrong:
is_peak_hour = TRUE

Correct:
is_peak_hour = 0

Wrong:
is_peak_hour = FALSE

dropped_calls is INTEGER

active_users is INTEGER

quality_score is NUMERIC

packet_loss_pct is NUMERIC

download_speed_mbps is NUMERIC

upload_speed_mbps is NUMERIC

avg_latency_ms is NUMERIC

====================================================
QUERY GENERATION RULES
====================================================

For highest/best:
use MAX() or ORDER BY DESC

For lowest/worst:
use MIN() or ORDER BY ASC

For averages:
use AVG()

For counts:
use COUNT()

For comparisons:
use GROUP BY

For ranking:
use LIMIT

Never generate invalid PostgreSQL syntax

Never compare INTEGER with BOOLEAN

Return ONLY valid PostgreSQL SQL
====================================================
""";

        Document schemaDoc =
                new Document(
                        schema,
                        Map.of(
                                "type", "schema",
                                "domain", "telecom"
                        )
                );

        vectorStore.add(
                List.of(schemaDoc)
        );

        logger.info(
                "Telecom schema metadata ingested successfully."
        );
    }
}