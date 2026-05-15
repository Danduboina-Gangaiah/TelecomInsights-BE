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
            LoggerFactory.getLogger(
                    OrchestratorService.class
            );

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
    // THRESHOLD
    // =====================================================

    private static final double THRESHOLD = 0.45;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public OrchestratorService(

            EmbeddingModel embeddingModel,

            NLQAgent nlqAgent,

            InsightAgent insightAgent,

            AnomalyAgent anomalyAgent,

            GuardrailResponseHandler guardrail
    ) {

        this.vectorStore =
                SimpleVectorStore.builder(
                        embeddingModel
                ).build();

        this.nlqAgent = nlqAgent;

        this.insightAgent = insightAgent;

        this.anomalyAgent = anomalyAgent;

        this.guardrail = guardrail;

        initializeSemanticRoutes();
    }

    // =====================================================
    // DOMAIN VALIDATION
    // =====================================================

    private boolean isTelecomIntent(
            String question
    ) {

        String q =
                question.toLowerCase();

        return

                q.contains("latency")

                        || q.contains("download")

                        || q.contains("upload")

                        || q.contains("packet")

                        || q.contains("carrier")

                        || q.contains("network")

                        || q.contains("5g")

                        || q.contains("telecom")

                        || q.contains("signal")

                        || q.contains("region")

                        || q.contains("city")

                        || q.contains("device")

                        || q.contains("speed")

                        || q.contains("quality")

                        || q.contains("congestion")

                        || q.contains("drop")

                        || q.contains("iphone")

                        || q.contains("galaxy")

                        || q.contains("pixel")

                        || q.contains("nord")

                        || q.contains("verizon")

                        || q.contains("at&t")

                        || q.contains("bsnl")

                        || q.contains("vi")

                        || q.contains("network_band")

                        || q.contains("packet loss")

                        || q.contains("active users")

                        || q.contains("quality score")

                        || q.contains("telecom analytics");
    }

    // =====================================================
    // INTENT CLASSIFIER
    // =====================================================

    private AgentType classifyIntent(
            String question
    ) {

        String q =
                question.toLowerCase();

        // =====================================================
        // INSIGHT QUESTIONS
        // =====================================================

        if (

                q.contains("why")

                        || q.contains("explain")

                        || q.contains("analyze")

                        || q.contains("analysis")

                        || q.contains("trend")

                        || q.contains("root cause")

                        || q.contains("reason")

                        || q.contains("insight")

                        || q.contains("summary")

                        || q.contains("recommendation")

                        || q.contains("performing better")

                        || q.contains("performance issue")

                        || q.contains("degradation")
        ) {

            return AgentType.INSIGHT_AGENT;
        }

        // =====================================================
        // ANOMALY QUESTIONS
        // =====================================================

        if (

                q.contains("anomaly")

                        || q.contains("abnormal")

                        || q.contains("outage")

                        || q.contains("spike")

                        || q.contains("failure")

                        || q.contains("suspicious")
        ) {

            return AgentType.ANOMALY_AGENT;
        }

        // =====================================================
        // DEFAULT → NLQ
        // =====================================================

        return AgentType.NLQ_AGENT;
    }

    // =====================================================
    // SEMANTIC ROUTES
    // =====================================================

    private void initializeSemanticRoutes() {

        logger.info(
                "Initializing telecom semantic routes..."
        );

        List<Document> routes = List.of(

                new Document(
                        """
                        telecom KPI metrics
                        telecom statistics
                        compare download speed
                        compare upload speed
                        compare latency
                        packet loss metrics
                        network utilization
                        telecom performance metrics
                        carrier analytics
                        region analytics
                        device analytics
                        network analytics
                        telecom trends
                        anomaly detection
                        """,
                        Map.of(
                                "domain",
                                "telecom"
                        )
                )
        );

        vectorStore.add(routes);

        logger.info(
                "Telecom semantic routes initialized successfully."
        );
    }

    // =====================================================
    // MAIN ROUTER
    // =====================================================

    public Object route(String question) {

        try {

            question =
                    question.trim();

            logger.info(
                    "Routing Question: {}",
                    question
            );

            // =====================================================
            // DOMAIN VALIDATION
            // =====================================================

            if (!isTelecomIntent(question)) {

                logger.warn(
                        "Rejected non telecom query"
                );

                return guardrail.handleOffTopic(
                        question
                );
            }

            // =====================================================
            // VECTOR SEARCH
            // =====================================================

            List<Document> matches =
                    vectorStore.similaritySearch(

                            SearchRequest.builder()

                                    .query(question)

                                    .topK(1)

                                    .similarityThreshold(
                                            THRESHOLD
                                    )

                                    .build()
                    );

            // =====================================================
            // SEMANTIC GUARDRAIL
            // =====================================================

            if (matches == null
                    || matches.isEmpty()) {

                logger.warn(
                        "Semantic Guardrail Triggered"
                );

                return guardrail.handleOffTopic(
                        question
                );
            }

            // =====================================================
            // CLASSIFY INTENT
            // =====================================================

            AgentType agentType =
                    classifyIntent(question);

            logger.info(
                    "Classified Agent: {}",
                    agentType
            );

            // =====================================================
            // ROUTING
            // =====================================================

            return switch (agentType) {

                // =====================================================
                // NLQ
                // =====================================================

                case NLQ_AGENT -> {

                    logger.info(
                            "Routing → NLQ_AGENT"
                    );

                    yield nlqAgent.processQuestion(
                            question
                    );
                }

                // =====================================================
                // INSIGHT
                // =====================================================

                case INSIGHT_AGENT -> {

                    logger.info(
                            "Routing → INSIGHT_AGENT"
                    );

                    // =============================================
                    // SUPPORTING ANALYTICS QUERY
                    // =============================================

                    String supportingQuestion =
                            """
                            Give telecom performance metrics including
                            download speed,
                            upload speed,
                            latency,
                            packet loss,
                            congestion level,
                            and network utilization
                            related to:
                            """
                                    + question;

                    Map<String, Object> analyticsData =
                            nlqAgent.processQuestion(
                                    supportingQuestion
                            );

                    if (analyticsData == null) {

                        yield Map.of(
                                "status", "FAILED",

                                "source",
                                "INSIGHT_AGENT",

                                "reason",
                                "Failed to fetch telecom analytics"
                        );
                    }

                    Object rawData =
                            analyticsData.get(
                                    "rawData"
                            );

                    if (!(rawData instanceof List<?> rawList)
                            || rawList.isEmpty()) {

                        yield Map.of(
                                "status", "FAILED",

                                "source",
                                "INSIGHT_AGENT",

                                "reason",
                                "No telecom insight data available"
                        );
                    }

                    List<Map<String, Object>> data =
                            (List<Map<String, Object>>)
                                    rawData;

                    yield insightAgent.generateInsights(

                            question,

                            data
                    );
                }

                // =====================================================
                // ANOMALY
                // =====================================================

                case ANOMALY_AGENT -> {

                    logger.info(
                            "Routing → ANOMALY_AGENT"
                    );

                    yield anomalyAgent.runManualCheck();
                }

                // =====================================================
                // DEFAULT
                // =====================================================

                default -> guardrail.handleOffTopic(
                        question
                );
            };

        } catch (Exception e) {

            logger.error(
                    "Semantic Routing Error",
                    e
            );

            return Map.of(

                    "status", "FAILED",

                    "source", "ORCHESTRATOR",

                    "reason",
                    "Unable to process telecom request"
            );
        }
    }
}