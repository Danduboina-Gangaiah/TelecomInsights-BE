package com.telecom.insights.controller;

import com.telecom.insights.agents.NLQAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@CrossOrigin("http://localhost:5173/")

@RestController
@RequestMapping("/insights")
@Tag(name = "Telecom Insights API")
public class QueryController {

    private final NLQAgent nlqAgent;

    public QueryController(NLQAgent nlqAgent) {
        this.nlqAgent = nlqAgent;
    }

    @GetMapping("/health")
    @Operation(summary = "Health Check")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Application Running");
    }

    @GetMapping("/nlq")
    @Operation(summary = "Ask Gemini")
    public ResponseEntity<String> ask(@RequestParam String message) {
        return ResponseEntity.ok(nlqAgent.processQuery(message));
    }
}