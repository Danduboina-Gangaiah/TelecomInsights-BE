package com.telecom.insights.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.insights.model.QueryLog;
import com.telecom.insights.repository.QueryLogRepository;
import com.telecom.insights.repository.SafeSqlExecutor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NLQAgent {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final SafeSqlExecutor sqlExecutor;
    private final QueryLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public NLQAgent(
            ChatClient.Builder builder,
            VectorStore vectorStore,
            SafeSqlExecutor sqlExecutor,
            QueryLogRepository logRepository,
            ObjectMapper objectMapper
    ) {

        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.sqlExecutor = sqlExecutor;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> processQuestion(String question) {

        try {

            String normalized =
                    question.trim().toLowerCase();

            // =====================================================
            // DB CACHE CHECK
            // =====================================================

            Optional<QueryLog> cached =
                    logRepository.findByQuestion(normalized);

            if (cached.isPresent()) {

                Map<String, Object> cachedMap =
                        objectMapper.readValue(
                                cached.get().getResponseJson(),
                                Map.class
                        );

                cachedMap.put("source", "NLQ_DB_CACHE");

                return cachedMap;
            }

            long start = System.currentTimeMillis();

            // =====================================================
            // VECTOR SEARCH
            // =====================================================

            List<Document> docs =
                    vectorStore.similaritySearch(
                            SearchRequest.builder()
                                    .query(question)
                                    .topK(3)
                                    .build()
                    );

            String context =
                    docs.stream()
                            .map(Document::getText)
                            .collect(Collectors.joining("\n"));

            // =====================================================
            // LLM SQL GENERATION
            // =====================================================

            String sql = chatClient.prompt()

                    .system(s -> s.text("""
You are a PostgreSQL telecom expert.

RULES:
- Use ONLY SELECT queries
- Never use DELETE UPDATE INSERT DROP ALTER
- Return SQL only

Schema Context:
{ctx}
""").param("ctx", context))

                    .user(question)
                    .call()
                    .content()
                    .replace("```sql", "")
                    .replace("```", "")
                    .trim();

            // =====================================================
            // SQL EXECUTION
            // =====================================================

            List<Map<String, Object>> rows =
                    sqlExecutor.executeReadOnlyQuery(sql);

            // =====================================================
            // BUSINESS RESPONSE
            // =====================================================

            String answer = chatClient.prompt()

                    .user("""
Question:
%s

Data:
%s

Write concise telecom business insights.
""".formatted(question, rows))

                    .call()
                    .content();

            long ms =
                    System.currentTimeMillis() - start;

            // =====================================================
            // FINAL RESPONSE
            // =====================================================

            Map<String, Object> out =
                    new LinkedHashMap<>();

            out.put("question", question);
            out.put("queryType", "NLQ");
            out.put("generatedSql", sql);
            out.put("rawData", rows);
            out.put("answer", answer);
            out.put("status", "SUCCESS");
            out.put("executionMs", ms);
            out.put("source", "LLM");

            // =====================================================
            // SAVE FULL RESPONSE TO DB
            // =====================================================

            String json =
                    objectMapper.writeValueAsString(out);

            logRepository.save(
                    new QueryLog(normalized, json)
            );

            return out;

        } catch (Exception e) {

            return Map.of(
                    "status", "FAILED",
                    "source", "NLQ_AGENT",
                    "reason", e.getMessage()
            );
        }
    }
}