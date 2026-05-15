package com.telecom.insights.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.insights.model.QueryLog;
import com.telecom.insights.repository.QueryLogRepository;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NLQAgent {

    private final ChatClient chatClient;

    private final JdbcTemplate jdbcTemplate;

    private final QueryLogRepository logRepository;

    private final ObjectMapper objectMapper;

    private final VectorStore vectorStore;

    public NLQAgent(

            ChatClient.Builder builder,

            JdbcTemplate jdbcTemplate,

            QueryLogRepository logRepository,

            ObjectMapper objectMapper,

            VectorStore vectorStore
    ) {

        this.chatClient =
                builder.build();

        this.jdbcTemplate =
                jdbcTemplate;

        this.logRepository =
                logRepository;

        this.objectMapper =
                objectMapper;

        this.vectorStore =
                vectorStore;
    }

    // =====================================================
    // MAIN NLQ ENGINE
    // =====================================================

    public Map<String, Object> processQuestion(
            String question
    ) {

        long start =
                System.currentTimeMillis();

        try {

            // =====================================================
            // NORMALIZE QUESTION
            // =====================================================

            String normalizedQuestion =
                    question
                            .toLowerCase()
                            .trim();

            // =====================================================
            // CACHE CHECK
            // =====================================================

            Optional<QueryLog> cached =
                    logRepository.findByQuestion(
                            normalizedQuestion
                    );

            if (cached.isPresent()) {

                Map<String, Object> cachedMap =
                        objectMapper.readValue(

                                cached.get()
                                        .getResponseJson(),

                                Map.class
                        );

                cachedMap.put(
                        "source",
                        "NLQ_DB_CACHE"
                );

                return cachedMap;
            }

            // =====================================================
            // VECTOR SEARCH
            // =====================================================

            List<Document> docs =
                    vectorStore.similaritySearch(

                            SearchRequest.builder()

                                    .query(question)

                                    .topK(5)

                                    .build()
                    );

            String context =
                    docs.stream()

                            .map(Document::getText)

                            .reduce(
                                    "",
                                    (a, b) -> a + "\n" + b
                            );

            // =====================================================
            // SQL PROMPT
            // =====================================================

            String prompt = """
You are an expert PostgreSQL Telecom Analytics AI.

Generate VALID PostgreSQL SQL ONLY.

STRICT RULES:

1. Use ONLY this table:
refined_network_metrics

2. NEVER invent columns

3. Return ONLY executable SQL

4. NO markdown
5. NO explanations
6. NO comments
7. NO ```sql

8. ALWAYS use LOWER() for string comparison

9. ALWAYS use ILIKE for text filtering

10. ALWAYS use LIMIT for:
top, best, highest, fastest

11. If aggregation is used:
ALWAYS include GROUP BY

12. For comparisons:
ALWAYS include compared entities

13. If user asks highest/best:
ORDER BY DESC

14. If user asks lowest/worst:
ORDER BY ASC

15. NEVER generate incomplete SQL

16. NEVER hallucinate schema columns

17. NEVER use AVG(), SUM(), MIN(), MAX()
on VARCHAR/TEXT columns

18. TEXT COLUMNS:

- congestion_level
- weather_condition
- region
- state
- city
- device_model
- carrier
- network_band
- environment_type

19. NUMERIC COLUMNS:

- download_speed_mbps
- upload_speed_mbps
- avg_latency_ms
- packet_loss_pct
- active_users
- network_utilization_pct
- quality_score
- dropped_calls

20. NEVER use:
AVG(congestion_level)

21. For congestion_level:
ONLY use GROUP BY or COUNT()

22. If unsupported:
return exactly:

SELECT 'INVALID_QUERY' AS error;

SCHEMA:
%s

QUESTION:
%s
"""
                    .formatted(
                            context,
                            question
                    );

            // =====================================================
            // GENERATE SQL
            // =====================================================

            String generatedSql =
                    chatClient.prompt(prompt)
                            .call()
                            .content();

            // =====================================================
            // CLEAN SQL
            // =====================================================

            generatedSql =
                    generatedSql

                            .replace("```sql", "")

                            .replace("```", "")

                            .replace(";", "")

                            .trim();

            System.out.println(
                    "================================="
            );

            System.out.println(
                    "GENERATED SQL:"
            );

            System.out.println(
                    generatedSql
            );

            System.out.println(
                    "================================="
            );

            // =====================================================
            // VALIDATION
            // =====================================================

            String lowerSql =
                    generatedSql.toLowerCase();

            // ONLY SELECT

            if (!lowerSql.startsWith("select")) {

                return Map.of(

                        "status", "FAILED",

                        "source", "NLQ_AGENT",

                        "reason",
                        "Invalid SQL generated"
                );
            }

            // DANGEROUS SQL

            if (

                    lowerSql.contains("delete")

                            || lowerSql.contains("drop")

                            || lowerSql.contains("update")

                            || lowerSql.contains("insert")

                            || lowerSql.contains("alter")
            ) {

                return Map.of(

                        "status", "FAILED",

                        "source", "NLQ_AGENT",

                        "reason",
                        "Unsafe SQL blocked"
                );
            }

            // INVALID QUERY

            if (lowerSql.contains("invalid_query")) {

                return Map.of(

                        "status", "FAILED",

                        "source", "NLQ_AGENT",

                        "reason",
                        "Question not supported by telecom dataset"
                );
            }

            // INCOMPLETE SQL

            if (

                    lowerSql.endsWith("=")

                            || lowerSql.endsWith("where")

                            || lowerSql.endsWith("group by")

                            || lowerSql.endsWith("order by")
            ) {

                return Map.of(

                        "status", "FAILED",

                        "source", "NLQ_AGENT",

                        "reason",
                        "Incomplete SQL generated"
                );
            }

            // BLOCK INVALID VARCHAR AGGREGATIONS

            if (

                    lowerSql.contains("avg(congestion_level)")

                            || lowerSql.contains("sum(congestion_level)")

                            || lowerSql.contains("max(congestion_level)")

                            || lowerSql.contains("min(congestion_level)")
            ) {

                return Map.of(

                        "status", "FAILED",

                        "source", "NLQ_AGENT",

                        "reason",
                        "Invalid aggregation on congestion_level"
                );
            }

            // =====================================================
            // EXECUTE SQL
            // =====================================================

            List<Map<String, Object>> results =
                    jdbcTemplate.queryForList(
                            generatedSql
                    );

            System.out.println(
                    "TOTAL ROWS: "
                            + results.size()
            );

            // =====================================================
            // EMPTY RESULTS
            // =====================================================

            if (results.isEmpty()) {

                return Map.of(

                        "status", "FAILED",

                        "source", "NLQ_AGENT",

                        "reason",
                        "No matching telecom data found"
                );
            }

            // =====================================================
            // ANSWER PROMPT
            // =====================================================

            String answerPrompt = """
You are a Telecom Analytics AI Assistant.

Generate a concise telecom analytics response.

STRICT RULES:

- Use clean business English
- Mention important findings
- Mention best/worst performers
- Mention regions/carriers/devices if relevant
- NEVER mention SQL
- NEVER mention databases
- NEVER dump JSON
- NO markdown
- NO bullet points
- Keep response under 6 lines

QUESTION:
%s

RESULTS:
%s
"""
                    .formatted(
                            question,
                            results
                    );

            String finalAnswer =
                    chatClient.prompt(answerPrompt)
                            .call()
                            .content();

            // =====================================================
            // EXECUTION TIME
            // =====================================================

            long executionMs =
                    System.currentTimeMillis()
                            - start;

            // =====================================================
            // FINAL RESPONSE
            // =====================================================

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "question",
                    question
            );

            response.put(
                    "queryType",
                    "NLQ"
            );

            response.put(
                    "generatedSql",
                    generatedSql
            );

            response.put(
                    "rawData",
                    results
            );

            response.put(
                    "answer",
                    finalAnswer
            );

            response.put(
                    "status",
                    "SUCCESS"
            );

            response.put(
                    "executionMs",
                    executionMs
            );

            response.put(
                    "source",
                    "NLQ_AGENT"
            );

            // =====================================================
            // SAVE CACHE
            // =====================================================

            String json =
                    objectMapper.writeValueAsString(
                            response
                    );

            logRepository.save(

                    new QueryLog(
                            normalizedQuestion,
                            json
                    )
            );

            return response;

        } catch (Exception e) {

            e.printStackTrace();

            return Map.of(

                    "status", "FAILED",

                    "source", "NLQ_AGENT",

                    "reason",
                    "Unable to process telecom analytics query"
            );
        }
    }
}