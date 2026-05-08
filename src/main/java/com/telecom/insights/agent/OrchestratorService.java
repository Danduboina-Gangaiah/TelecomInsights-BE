package com.telecom.insights.agent;

import com.telecom.insights.handler.GuardrailResponseHandler;
import com.telecom.insights.model.AgentType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OrchestratorService {

    private static final Logger logger =
            LoggerFactory.getLogger(OrchestratorService.class);

    // =====================================================
    // VECTOR STORE
    // =====================================================

    private final SimpleVectorStore vectorStore;

    // =====================================================
    // AGENTS
    // =====================================================

    private final NLQAgent nlqAgent;
    private final InsightAgent insightAgent;
    private final AnomalyAgent anomalyAgent;

    private final GuardrailResponseHandler guardrail;

    // =====================================================
    // SEMANTIC THRESHOLD
    // =====================================================

    private static final double THRESHOLD = 0.45;

    public OrchestratorService(
            EmbeddingModel embeddingModel,
            NLQAgent nlqAgent,
            InsightAgent insightAgent,
            AnomalyAgent anomalyAgent,
            GuardrailResponseHandler guardrail
    ) {

        this.vectorStore =
                SimpleVectorStore.builder(embeddingModel).build();

        this.nlqAgent = nlqAgent;
        this.insightAgent = insightAgent;
        this.anomalyAgent = anomalyAgent;
        this.guardrail = guardrail;

        initializeSemanticRoutes();
    }

    // =====================================================
    // SEMANTIC ROUTES
    // =====================================================

    private void initializeSemanticRoutes() {

        logger.info("Initializing semantic vector routes...");

        List<Document> routes = List.of(

                // =====================================================
                // NLQ AGENT
                // =====================================================

                new Document(
                        """
                        show telecom data
                        show telecom metrics
                        show KPI data
                        telecom statistics
                        network statistics
                        telecom dashboard
                        average latency
                        average packet loss
                        average download speed
                        average upload speed
                        compare download speed
                        compare upload speed
                        compare telecom metrics
                        compare carriers
                        compare cities
                        compare regions
                        compare states
                        show top telecom regions
                        show best carrier
                        show highest download speed
                        show network utilization
                        show dropped calls
                        show signal strength
                        show quality score
                        telecom query
                        telecom report
                        network report
                        city with best speed
                        carrier with best latency
                        which city has highest download speed
                        which region has best upload speed
                        show average telecom metrics
                        compare network performance
                        best telecom region
                        top telecom carrier
                        telecom analytics query
                        """,
                        Map.of(
                                "agent",
                                AgentType.NLQ_AGENT.name()
                        )
                ),

                // =====================================================
                // INSIGHT AGENT
                // =====================================================

                new Document(
                        """
                        why network quality is poor
                        why latency is high
                        why packet loss is increasing
                        why telecom performance is degrading
                        explain telecom issues
                        explain network issues
                        explain telecom trends
                        analyze telecom trends
                        analyze network trends
                        analyze network quality
                        generate telecom insights
                        provide business insights
                        telecom executive insights
                        telecom strategic insights
                        telecom recommendations
                        suggest network improvements
                        root cause analysis
                        telecom optimization recommendations
                        analyze congestion impact
                        analyze dropped calls
                        explain poor telecom regions
                        explain bad network quality
                        why some regions perform poorly
                        why network performance differs
                        carrier performance analysis
                        business summary
                        operational insights
                        analyze telecom behavior
                        explain telecom performance
                        why some carriers perform better
                        analyze regional telecom issues
                        suggest telecom optimizations
                        explain network degradation
                        why download speed is low
                        """,
                        Map.of(
                                "agent",
                                AgentType.INSIGHT_AGENT.name()
                        )
                ),

                // =====================================================
                // ANOMALY AGENT
                // =====================================================

                new Document(
                        """
                        detect anomalies
                        anomaly detection
                        telecom anomaly
                        network anomaly
                        abnormal behavior
                        abnormal latency
                        abnormal packet loss
                        suspicious telecom activity
                        unusual network activity
                        detect telecom problems
                        network outage
                        telecom outage
                        telecom failure
                        critical latency
                        abnormal utilization
                        anomaly report
                        detect network spikes
                        unusual packet loss
                        identify telecom anomalies
                        abnormal telecom metrics
                        suspicious latency spikes
                        abnormal download speed
                        network instability
                        detect telecom failure
                        """,
                        Map.of(
                                "agent",
                                AgentType.ANOMALY_AGENT.name()
                        )
                )
        );

        vectorStore.add(routes);

        logger.info("Semantic vector routes initialized successfully");
    }

    // =====================================================
    // MAIN ROUTER
    // =====================================================

    public Object route(String question) {

        try {

            // =====================================================
            // NORMALIZE QUESTION
            // =====================================================

            question =
                    question.toLowerCase().trim();

            logger.info("Routing Question: {}", question);

            // =====================================================
            // VECTOR SEARCH
            // =====================================================

            List<Document> matches =
                    vectorStore.similaritySearch(
                            SearchRequest.builder()
                                    .query(question)
                                    .topK(1)
                                    .similarityThreshold(THRESHOLD)
                                    .build()
                    );

            // =====================================================
            // GUARDRAIL
            // =====================================================

            if (matches == null || matches.isEmpty()) {

                logger.warn("Guardrail triggered");

                return guardrail.handleOffTopic(question);
            }

            // =====================================================
            // MATCHED AGENT
            // =====================================================

            Document bestMatch =
                    matches.get(0);

            String matchedAgent =
                    (String) bestMatch
                            .getMetadata()
                            .get("agent");

            logger.info(
                    "Semantic Match Agent: {}",
                    matchedAgent
            );

            AgentType agentType =
                    AgentType.valueOf(matchedAgent);

            // =====================================================
            // ROUTING
            // =====================================================

            return switch (agentType) {

                // =====================================================
                // NLQ AGENT
                // =====================================================

                case NLQ_AGENT -> {

                    logger.info("Routing → NLQ Agent");

                    yield nlqAgent.processQuestion(question);
                }

                // =====================================================
                // INSIGHT AGENT
                // =====================================================

                case INSIGHT_AGENT -> {

                    logger.info("Routing → Insight Agent");

                    Map<String, Object> analyticsData =
                            nlqAgent.processQuestion(
                                    "show average latency, average packet loss, average quality score, average download speed, average upload speed by region"
                            );

                    if (analyticsData == null) {

                        yield Map.of(
                                "status", "FAILED",
                                "source", "INSIGHT_AGENT",
                                "reason", "Failed to fetch telecom analytics"
                        );
                    }

                    Object rawData =
                            analyticsData.get("rawData");

                    if (!(rawData instanceof List<?> rawList)
                            || rawList.isEmpty()) {

                        yield Map.of(
                                "status", "FAILED",
                                "source", "INSIGHT_AGENT",
                                "reason", "No telecom insight data available"
                        );
                    }

                    List<Map<String, Object>> data =
                            (List<Map<String, Object>>) rawData;

                    yield insightAgent.generateInsights(
                            question,
                            data
                    );
                }

                // =====================================================
                // ANOMALY AGENT
                // =====================================================

                case ANOMALY_AGENT -> {

                    logger.info("Routing → Anomaly Agent");

                    yield anomalyAgent.runManualCheck();
                }

                // =====================================================
                // DEFAULT
                // =====================================================

                default -> guardrail.handleOffTopic(question);
            };

        } catch (Exception e) {

            logger.error(
                    "Semantic Routing Error",
                    e
            );

            return Map.of(
                    "status", "FAILED",
                    "source", "ORCHESTRATOR",
                    "reason", e.getMessage()
            );
        }
    }
}