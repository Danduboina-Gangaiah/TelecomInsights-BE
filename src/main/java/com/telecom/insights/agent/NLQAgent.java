package com.telecom.insights.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.insights.model.QueryLog;
import com.telecom.insights.repository.QueryLogRepository;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
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

        this.chatClient = builder.build();
        this.jdbcTemplate = jdbcTemplate;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
        this.vectorStore = vectorStore;
    }

    public Map<String, Object> processQuestion(String question) {

        long start = System.currentTimeMillis();

        try {

            // =====================================================
            // NORMALIZE QUESTION
            // =====================================================

            String normalizedQuestion =
                    question.toLowerCase().trim();

            // =====================================================
            // CACHE CHECK
            // =====================================================

            Optional<QueryLog> cached =
                    logRepository.findByQuestion(normalizedQuestion);

            if (cached.isPresent()) {

                Map<String, Object> cachedMap =
                        objectMapper.readValue(
                                cached.get().getResponseJson(),
                                Map.class
                        );

                cachedMap.put("source", "NLQ_DB_CACHE");

                return cachedMap;
            }

            // =====================================================
            // VECTOR SEARCH
            // =====================================================

            List<Document> docs =
                    vectorStore.similaritySearch(question);

            String context =
                    docs.stream()
                            .map(Document::getText)
                            .reduce("", (a, b) -> a + "\n" + b);

            // =====================================================
            // SQL GENERATION PROMPT
            // =====================================================

            String prompt = """
You are an expert PostgreSQL Telecom Analytics AI.

Your task:
Convert telecom business questions into VALID PostgreSQL SQL.

STRICT RULES:

1. Use ONLY this table:
refined_network_metrics

2. Use ONLY existing columns from schema context

3. NEVER invent columns

4. Return ONLY executable PostgreSQL SQL

5. NO markdown
6. NO explanations
7. NO comments
8. NO ```sql

9. ALWAYS use LIMIT when asking:
best/top/highest/fastest

10. If question asks:
- best/highest/fastest → use DESC
- worst/lowest/slowest → use ASC

11. If aggregation is used:
ALWAYS include GROUP BY

12. NEVER generate incomplete WHERE clauses

13. NEVER use columns not present in schema

14. If no matching column exists:
return:
SELECT 'INVALID_QUERY' AS error;

Schema:
%s

Question:
%s
""".formatted(context, question);

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
                            .trim();

            System.out.println("=================================");
            System.out.println("GENERATED SQL:");
            System.out.println(generatedSql);
            System.out.println("=================================");

            // =====================================================
            // SQL VALIDATION
            // =====================================================

            String lowerSql =
                    generatedSql.toLowerCase();

            if (!lowerSql.contains("select")) {

                return Map.of(
                        "status", "FAILED",
                        "source", "NLQ_AGENT",
                        "reason", "Invalid SQL generated"
                );
            }

            if (lowerSql.endsWith("=")
                    || lowerSql.endsWith("where")
                    || lowerSql.endsWith("group by")
                    || lowerSql.endsWith("order by")) {

                return Map.of(
                        "status", "FAILED",
                        "source", "NLQ_AGENT",
                        "reason", "Incomplete SQL generated"
                );
            }

            if (generatedSql.contains("INVALID_QUERY")) {

                return Map.of(
                        "status", "FAILED",
                        "source", "NLQ_AGENT",
                        "reason", "Question not supported by dataset schema"
                );
            }

            // =====================================================
            // EXECUTE SQL
            // =====================================================

            List<Map<String, Object>> results =
                    jdbcTemplate.queryForList(generatedSql);

            System.out.println("TOTAL ROWS: " + results.size());

            // =====================================================
            // EMPTY RESULTS
            // =====================================================

            if (results.isEmpty()) {

                return Map.of(
                        "status", "FAILED",
                        "source", "NLQ_AGENT",
                        "reason", "No matching telecom data found"
                );
            }

            // =====================================================
            // ANSWER GENERATION
            // =====================================================

            String answerPrompt = """
You are a Telecom Analytics AI Assistant.

Generate a clean natural language answer.

RULES:
- Be concise
- Use business language
- Mention key findings
- Mention best/worst performers if available
- Do NOT mention SQL
- Do NOT mention databases
- Directly answer the user question

Question:
%s

Results:
%s
""".formatted(question, results);

            String finalAnswer =
                    chatClient.prompt(answerPrompt)
                            .call()
                            .content();

            // =====================================================
            // EXECUTION TIME
            // =====================================================

            long executionMs =
                    System.currentTimeMillis() - start;

            // =====================================================
            // FINAL RESPONSE
            // =====================================================

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("question", question);
            response.put("queryType", "NLQ");
            response.put("generatedSql", generatedSql);
            response.put("rawData", results);
            response.put("answer", finalAnswer);
            response.put("status", "SUCCESS");
            response.put("executionMs", executionMs);
            response.put("source", "NLQ_AGENT");

            // =====================================================
            // SAVE CACHE
            // =====================================================

            String json =
                    objectMapper.writeValueAsString(response);

            logRepository.save(
                    new QueryLog(normalizedQuestion, json)
            );

            return response;

        } catch (Exception e) {

            e.printStackTrace();

            return Map.of(
                    "status", "FAILED",
                    "source", "NLQ_AGENT",
                    "reason", e.getMessage()
            );
        }
    }
}