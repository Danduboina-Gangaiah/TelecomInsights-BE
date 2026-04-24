package com.telecom.insights.controller;

import com.telecom.insights.agents.NLQAgent;
import com.telecom.insights.model.QueryLog;
import com.telecom.insights.repository.QueryLogRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/insights")
@Tag(name = "Telecom Insights API")
public class QueryController {

    private static final Logger logger =
            LoggerFactory.getLogger(QueryController.class);

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
            logger.info("CACHE HIT for query: {}", normalized);
            return ResponseEntity.ok(existing.get().getResponse());
        }

        logger.info("Calling Gemini for new query: {}", normalized);

        String response = nlqAgent.processQuery(msg);

        QueryLog log = new QueryLog(normalized, response);
        queryLogRepository.save(log);

        logger.info("Response cached successfully for query: {}", normalized);

        return ResponseEntity.ok(response);
    }
}
