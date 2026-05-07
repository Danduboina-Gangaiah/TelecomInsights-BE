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

    private final SimpleVectorStore vectorStore;

    private final NLQAgent nlqAgent;
    private final InsightAgent insightAgent;
    private final AnomalyAgent anomalyAgent;

    private final GuardrailResponseHandler guardrail;

    private static final double THRESHOLD = 0.65;

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

        initializeRoutes();
    }

    // =====================================================
    // SEMANTIC ROUTES
    // =====================================================

    private void initializeRoutes() {

        logger.info("Initializing semantic routing...");

        List<Document> routes = List.of(

                // =====================================================
                // NLQ ROUTE
                // =====================================================

                new Document(
                        """
                        show latency
                        show average latency
                        show latency by region
                        latency report
                        telecom latency analytics
                        network metrics
                        telecom KPI
                        KPI dashboard
                        packet loss
                        packet loss by region
                        average packet loss
                        show download speed
                        average download speed
                        show upload speed
                        bandwidth metrics
                        network statistics
                        telecom analytics
                        network performance metrics
                        latency and packet loss
                        regional telecom performance
                        """,
                        Map.of("agent", AgentType.NLQ_AGENT.name())
                ),

                // =====================================================
                // INSIGHT ROUTE
                // =====================================================

                new Document(
                        """
                        analyze network performance
                        analyze telecom performance
                        analyze trends
                        generate insights
                        business insights
                        executive summary
                        summarize network performance
                        summarize telecom KPIs
                        telecom trend analysis
                        performance analysis
                        telecom business report
                        regional performance insights
                        network quality insights
                        detailed telecom insights
                        analyze latency trends
                        network performance trends
                        give insights
                        generate telecom insights
                        """,
                        Map.of("agent", AgentType.INSIGHT_AGENT.name())
                ),

                // =====================================================
                // ANOMALY ROUTE
                // =====================================================

                new Document(
                        """
                        detect anomaly
                        detect anomalies
                        anomaly
                        anomalies
                        anomaly detection
                        detect anomaly in network
                        detect anomaly in telecom
                        network anomaly
                        telecom anomaly
                        latency spike
                        high latency
                        packet loss issue
                        abnormal packet loss
                        outage detection
                        abnormal behavior
                        abnormal network activity
                        suspicious network behavior
                        network issue
                        telecom issue
                        network outage
                        critical network problem
                        identify network problems
                        identify telecom anomalies
                        check anomalies
                        check network anomaly
                        analyze anomalies
                        network failure
                        latency issue
                        critical latency
                        telecom failure
                        """
                        ,
                        Map.of("agent", AgentType.ANOMALY_AGENT.name())
                )
        );

        vectorStore.add(routes);

        logger.info("Semantic routing initialized");
    }

    // =====================================================
    // MAIN ROUTER
    // =====================================================

    public Object route(String question) {

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
        // GET AGENT
        // =====================================================

        String agentName =
                (String) matches.get(0)
                        .getMetadata()
                        .get("agent");

        logger.info("Matched Agent: {}", agentName);

        AgentType agent;

        try {

            agent = AgentType.valueOf(agentName);

        } catch (Exception e) {

            logger.error("Invalid Agent Mapping");

            return guardrail.handleOffTopic(question);
        }

        // =====================================================
        // ROUTING SWITCH
        // =====================================================

        return switch (agent) {

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

                Map<String, Object> nlqResult =
                        nlqAgent.processQuestion(
                                "show average latency packet loss and download speed by region"
                        );

                if (nlqResult == null) {

                    yield Map.of(
                            "source", "INSIGHT_AGENT",
                            "status", "FAILED",
                            "reason", "NLQ result is null"
                    );
                }

                Object rawData =
                        nlqResult.get("rawData");

                if (rawData == null) {

                    yield Map.of(
                            "source", "INSIGHT_AGENT",
                            "status", "FAILED",
                            "reason", "NLQ did not return rawData"
                    );
                }

                if (!(rawData instanceof List<?> rawList)) {

                    yield Map.of(
                            "source", "INSIGHT_AGENT",
                            "status", "FAILED",
                            "reason", "Invalid rawData format"
                    );
                }

                if (rawList.isEmpty()) {

                    yield Map.of(
                            "source", "INSIGHT_AGENT",
                            "status", "FAILED",
                            "reason", "No insight data found"
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
    }
}