package com.telecom.insights.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.insights.agent.NLQAgent;
import com.telecom.insights.model.QueryLog;
import com.telecom.insights.repository.QueryLogRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/insights")
@CrossOrigin(origins = "http://localhost:5173")
public class QueryController {

    private static final Logger logger =
            LoggerFactory.getLogger(QueryController.class);

    private final NLQAgent nlqAgent;
    private final QueryLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public QueryController(NLQAgent nlqAgent,
                           QueryLogRepository logRepository,
                           ObjectMapper objectMapper) {
        this.nlqAgent = nlqAgent;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Application Running");
    }

    @GetMapping("/ask")
    public ResponseEntity<Object> ask(@RequestParam String message) {
        String normalized = message.trim().toLowerCase();

        try {
            Optional<QueryLog> cached = logRepository.findByQuestion(normalized);

            if (cached.isPresent()) {
                Map<String,Object> cachedMap =
                        objectMapper.readValue(cached.get().getResponse(), Map.class);
                cachedMap.put("source", "CACHE");
                return ResponseEntity.ok(cachedMap);
            }

            Map<String,Object> fresh =
                    (Map<String,Object>) nlqAgent.processQuestion(message);

            Map<String,Object> saveCopy = new java.util.LinkedHashMap<>(fresh);
            fresh.put("source", "NLQ");

            logRepository.save(
                    new QueryLog(
                            normalized,
                            objectMapper.writeValueAsString(saveCopy)
                    )
            );

            return ResponseEntity.ok(fresh);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("status","ERROR","message",e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<QueryLog>> history() {

        return ResponseEntity.ok(
                logRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );
    }
}