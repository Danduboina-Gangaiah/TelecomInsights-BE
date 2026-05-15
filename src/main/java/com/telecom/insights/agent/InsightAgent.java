package com.telecom.insights.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.insights.model.QueryLog;
import com.telecom.insights.repository.QueryLogRepository;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InsightAgent {

    private final ChatClient chatClient;
    private final QueryLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public InsightAgent(
            ChatClient.Builder builder,
            QueryLogRepository logRepository,
            ObjectMapper objectMapper
    ) {

        this.chatClient = builder.build();
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
    }

    // =====================================================
    // STRICT TELECOM VALIDATION
    // =====================================================

    private boolean isTelecomQuestion(String question) {

        String q = question.toLowerCase();

        // =====================================================
        // REJECT NON TELECOM
        // =====================================================

        if (
                q.contains("movie") ||
                        q.contains("story") ||
                        q.contains("cricket") ||
                        q.contains("football") ||
                        q.contains("ipl") ||
                        q.contains("recipe") ||
                        q.contains("cooking") ||
                        q.contains("song") ||
                        q.contains("poem") ||
                        q.contains("joke") ||
                        q.contains("anime") ||
                        q.contains("love") ||
                        q.contains("romance") ||
                        q.contains("actor") ||
                        q.contains("actress") ||
                        q.contains("bitcoin") ||
                        q.contains("stock") ||
                        q.contains("weather") ||
                        q.contains("politics")
        ) {

            return false;
        }

        // =====================================================
        // ALLOW TELECOM
        // =====================================================

        return q.contains("latency") ||
                q.contains("packet") ||
                q.contains("network") ||
                q.contains("download") ||
                q.contains("upload") ||
                q.contains("telecom") ||
                q.contains("carrier") ||
                q.contains("signal") ||
                q.contains("5g") ||
                q.contains("bandwidth") ||
                q.contains("kpi") ||
                q.contains("congestion") ||
                q.contains("dropped") ||
                q.contains("tower") ||
                q.contains("quality") ||
                q.contains("coverage") ||
                q.contains("speed") ||
                q.contains("anomaly") ||
                q.contains("region") ||
                q.contains("city") ||
                q.contains("state") ||
                q.contains("utilization");
    }

    // =====================================================
    // MAIN INSIGHT ENGINE
    // =====================================================

    public Map<String, Object> generateInsights(
            String question,
            List<Map<String, Object>> queryResults
    ) {

        try {

            // =====================================================
            // STRICT GUARDRAIL
            // =====================================================

            if (!isTelecomQuestion(question)) {

                return Map.of(
                        "status", "REJECTED",
                        "source", "GUARDRAIL",
                        "message",
                        "The requested query is outside the supported telecom analytics domain."
                );
            }

            String cacheKey =
                    "insight_" + question.toLowerCase();

            // =====================================================
            // CACHE CHECK
            // =====================================================

            Optional<QueryLog> cached =
                    logRepository.findByQuestion(cacheKey);

            if (cached.isPresent()) {

                Map<String, Object> cachedMap =
                        objectMapper.readValue(
                                cached.get().getResponseJson(),
                                Map.class
                        );

                cachedMap.put(
                        "source",
                        "INSIGHT_DB_CACHE"
                );

                return cachedMap;
            }

            // =====================================================
            // EMPTY CHECK
            // =====================================================

            if (queryResults == null
                    || queryResults.isEmpty()) {

                return Map.of(
                        "status", "FAILED",
                        "source", "INSIGHT_AGENT",
                        "reason", "No insight data found"
                );
            }

            // =====================================================
            // LLM PROMPT
            // =====================================================

            String prompt = """
You are a Telecom Executive Insights AI.

Analyze the telecom KPI dataset and provide:

1. Executive Summary
2. Key Observations
3. Best Performing Regions or Carriers
4. Worst Performing Regions or Carriers
5. Latency Analysis
6. Download and Upload Performance
7. Packet Loss Analysis
8. Operational Recommendations

STRICT RULES:
- ONLY answer telecom analytics questions
- NEVER generate stories, jokes, poems, movies, politics, recipes, or entertainment
- NEVER answer non-telecom questions
- Use concise telecom executive language
- Do NOT generate SQL
- Do NOT mention JSON
- Do NOT dump raw database records
- Keep response business-oriented

Question:
%s

Dataset:
%s
"""
                    .formatted(question, queryResults);

            // =====================================================
            // CALL LLM
            // =====================================================

            String insights =
                    chatClient.prompt(prompt)
                            .call()
                            .content();

            // =====================================================
            // NULL SAFETY
            // =====================================================

            if (insights == null
                    || insights.isBlank()) {

                insights =
                        "No telecom insights could be generated.";
            }

            // =====================================================
            // FINAL RESPONSE
            // =====================================================

            Map<String, Object> out =
                    new LinkedHashMap<>();

            out.put("status", "SUCCESS");
            out.put("source", "INSIGHT_AGENT");
            out.put("question", question);
            out.put("insights", insights);

            // =====================================================
            // CACHE SAVE
            // =====================================================

            String json =
                    objectMapper.writeValueAsString(out);

            logRepository.save(
                    new QueryLog(cacheKey, json)
            );

            return out;

        } catch (Exception e) {

            return Map.of(
                    "status", "FAILED",
                    "source", "INSIGHT_AGENT",
                    "reason", e.getMessage()
            );
        }
    }
}