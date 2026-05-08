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
            LoggerFactory.getLogger(SchemaMetadataIngestor.class);

    private final VectorStore vectorStore;

    public SchemaMetadataIngestor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ingestSchemaMetadata() {

        logger.info("Initializing Telecom Schema Metadata...");

        // CLEAR OLD VECTOR DATA
        try {
            vectorStore.delete("type == 'schema'");
            logger.info("Old schema metadata cleared.");
        } catch (Exception e) {
            logger.info("No old schema metadata found.");
        }

        // NEW UPDATED SCHEMA
        String networkDataSchema = """
You are working with a PostgreSQL telecom analytics database.

MAIN TABLE:
refined_network_metrics

AVAILABLE COLUMNS:

timestamp
hour_of_day
is_peak_hour
region
state
city
network_band
environment_type
avg_latency_ms
download_speed_mbps
upload_speed_mbps
packet_loss_pct
active_users
network_utilization_pct
congestion_level
dropped_calls
weather_condition
quality_score
device_model
carrier

IMPORTANT RULES:

- Always use refined_network_metrics table
- Never use network_metrics table
- Never use region_id
- Never use cell_id

KNOWN CARRIERS:
Verizon
AT&T
T-Mobile
US Cellular

KNOWN REGIONS:
West
South
Midwest
Northeast

KNOWN STATES:
CA
TX
FL
NY
IL

KNOWN NETWORK BANDS:
n260
n78
n28
n258
n41
5G mmWave
5G Sub-6
4G LTE

BUSINESS MEANINGS:

- high avg_latency_ms = poor network
- low download_speed_mbps = slow internet
- high packet_loss_pct = unstable network
- high dropped_calls = severe telecom issue
- high network_utilization_pct = congestion
- high quality_score = better network performance

QUERY RULES:

- Use proper PostgreSQL SQL
- Always query refined_network_metrics
- Use aggregate functions when needed
- Use GROUP BY for comparisons
- Use LIMIT only when user asks
""";

        Document schemaDoc = new Document(
                networkDataSchema,
                Map.of(
                        "type", "schema",
                        "domain", "telecom",
                        "table", "refined_network_metrics"
                )
        );

        vectorStore.add(List.of(schemaDoc));

        logger.info("Updated schema metadata ingested successfully.");
    }
}