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

    private static final Logger logger = LoggerFactory.getLogger(SchemaMetadataIngestor.class);
    private final VectorStore vectorStore;

    public SchemaMetadataIngestor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ingestSchemaMetadata() {
        logger.info("Initializing 5G Schema Metadata in Vector Store...");

        // The "Brain" of your RAG - providing schema, regions, and the 2024 time constraint.
        String networkDataSchema= """
Table: network_metrics
Columns:
region_id, cell_id, avg_latency_ms, download_speed_mbps,
upload_speed_mbps, packet_loss_pct, active_users, timestamp
Known regions: Mumbai, Kolkata, New York, Delhi, Chennai, Tokyo, Berlin, San Francisco.
Business meanings:
low packet loss = stability
high active_users = traffic load
high latency = poor performance
""";


        Document schemaDoc = new Document(networkDataSchema, Map.of(
                "type", "schema",
                "domain", "5G_telecom",
                "table", "network_metrics"
        ));

        // Add to pgvector store
        vectorStore.add(List.of(schemaDoc));
        logger.info("Schema metadata with 2024 time-context and region constraints ingested successfully.");
    }
}