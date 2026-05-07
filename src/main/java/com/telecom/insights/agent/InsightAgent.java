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
            String context,
            List<Map<String, Object>> queryResults
    ) {

        try {

            String cacheKey =
                    "insight_" + context.toLowerCase();

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
            // INSIGHT PROMPT
            // =====================================================

            String insightPrompt = """
You are an enterprise telecom Insights Agent.

Your task:
- Analyze the provided structured data
- Identify trends and deviations
- Highlight best and worst regions
- Suggest root causes
- Provide executive-level narration

IMPORTANT:
- Do NOT generate SQL
- Use business language

Analysis Context:
%s

Structured Data:
%s
""".formatted(context, queryResults);

            String insights =
                    chatClient.prompt(insightPrompt)
                            .call()
                            .content();

            // =====================================================
            // FINAL RESPONSE
            // =====================================================

            Map<String, Object> out =
                    new LinkedHashMap<>();

            out.put("source", "LLM");
            out.put("status", "SUCCESS");
            out.put("insights", insights);
            out.put("input_data", queryResults);

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
                    "source", "INSIGHT_AGENT",
                    "status", "FAILED",
                    "reason", e.getMessage()
            );
        }
    }
}