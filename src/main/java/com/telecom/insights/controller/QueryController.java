package com.telecom.insights.controller;

import com.telecom.insights.agent.OrchestratorService;
import com.telecom.insights.model.QueryLog;
import com.telecom.insights.repository.QueryLogRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/insights")
@CrossOrigin(origins = "*")
public class QueryController {

    private final OrchestratorService orchestratorService;
    private final QueryLogRepository logRepository;

    public QueryController(
            OrchestratorService orchestratorService,
            QueryLogRepository logRepository
    ) {
        this.orchestratorService = orchestratorService;
        this.logRepository = logRepository;
    }

    // =====================================================
    // HEALTH
    // =====================================================

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Application Running");
    }

    // =====================================================
    // MAIN AGENT ENTRY
    // =====================================================

    @PostMapping("/ask")
    public ResponseEntity<?> ask(
            @RequestBody Map<String, String> body
    ) {

        String question = body.get("question");

        return ResponseEntity.ok(
                orchestratorService.route(question)
        );
    }

    // =====================================================
    // HISTORY
    // =====================================================

    @GetMapping("/history")
    public ResponseEntity<List<QueryLog>> history() {

        return ResponseEntity.ok(
                logRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );
    }
}