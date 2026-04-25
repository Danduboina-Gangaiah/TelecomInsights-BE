package com.telecom.insights.agent;

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

    public NLQAgent(ChatClient.Builder builder, VectorStore vectorStore,
                    SafeSqlExecutor sqlExecutor, QueryLogRepository logRepository){
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.sqlExecutor = sqlExecutor;
        this.logRepository = logRepository;
    }

    public Map<String,Object> processQuestion(String question){
        long start = System.currentTimeMillis();

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(3).build());

        String context = docs.stream().map(Document::getText).collect(Collectors.joining("\n"));

        String sql = chatClient.prompt()
                .system(s -> s.text("""
You are a PostgreSQL expert.
Use ONLY SELECT queries.
Use ONLY schema columns provided.
Never use DELETE UPDATE INSERT DROP ALTER.
Return SQL only.
Schema Context:
{ctx}
""").param("ctx", context))
                .user(question)
                .call().content().replace("```sql","").replace("```","").trim();

        List<Map<String,Object>> rows = sqlExecutor.executeReadOnlyQuery(sql);

        String answer = chatClient.prompt()
                .user("Question: "+question+"\nData: "+rows+"\nWrite concise business answer.")
                .call().content();

        long ms = System.currentTimeMillis()-start;

        logRepository.save(new QueryLog(question, answer));

        Map<String,Object> out = new LinkedHashMap<>();
        out.put("question", question);
        out.put("queryType", "NLQ");
        out.put("answer", answer);
        out.put("generatedSql", sql);
        out.put("rawData", rows);
        out.put("status", "SUCCESS");
        out.put("executionMs", ms);
        return out;
    }
}
