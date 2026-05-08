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

    public Map<String, Object> generateInsights(
            String question,
            List<Map<String, Object>> queryResults
    ) {

        try {

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

                cachedMap.put("source", "INSIGHT_DB_CACHE");

                return cachedMap;
            }

            // =====================================================
            // EMPTY CHECK
            // =====================================================

            if (queryResults == null || queryResults.isEmpty()) {

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

1. Key observations
2. Best performing regions/carriers
3. Worst performing regions/carriers
4. Latency trends
5. Download/upload performance
6. Packet loss issues
7. Business recommendations

IMPORTANT:
- Use professional telecom business language
- Keep response concise
- Do NOT generate SQL
- Do NOT mention JSON
- Do NOT mention raw database values repeatedly

Question:
%s

Dataset:
%s
""".formatted(question, queryResults);

            String insights =
                    chatClient.prompt(prompt)
                            .call()
                            .content();

            // =====================================================
            // FINAL RESPONSE
            // =====================================================

            Map<String, Object> out =
                    new LinkedHashMap<>();

            out.put("status", "SUCCESS");
            out.put("source", "INSIGHT AGENT");
            out.put("question", question);
            out.put("insights", insights);

            // =====================================================
            // SAVE CACHE
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