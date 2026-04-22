package com.telecom.insights.controller;

import com.telecom.insights.agents.NLQAgent;
import com.telecom.insights.model.QueryLog;
import com.telecom.insights.repository.QueryLogRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/insights")
@Tag(name = "Telecom Insights API")
public class QueryController {

    private final NLQAgent nlqAgent;
    private final QueryLogRepository queryLogRepository;

    public QueryController(NLQAgent nlqAgent,
                           QueryLogRepository queryLogRepository) {
        this.nlqAgent = nlqAgent;
        this.queryLogRepository = queryLogRepository;
    }

    @GetMapping("/health")
    @Operation(summary = "Health Check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Application Running");
    }

    @GetMapping("/nlq")
    @Operation(summary = "Ask Gemini with Cache")
    public ResponseEntity<String> ask(@RequestParam String msg) {


        String normalized = msg.trim().toLowerCase();


        Optional<QueryLog> existing =
                queryLogRepository.findByQuestion(normalized);

        if (existing.isPresent()) {
            System.out.println("⚡ CACHE HIT");
            return ResponseEntity.ok(existing.get().getResponse());
        }


        System.out.println("🤖 CALLING GEMINI");
        String response = nlqAgent.processQuery(msg);

        QueryLog log = new QueryLog(normalized, response);
        queryLogRepository.save(log);
        
        return ResponseEntity.ok(response);
    }
}