package com.telecom.insights.controller;

import com.telecom.insights.agent.AnomalyAgent;
import com.telecom.insights.repository.AnomalyAlertRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/anomaly")
@CrossOrigin(origins = "*")
public class AnomalyController {

    private final AnomalyAgent anomalyAgent;

    private final AnomalyAlertRepository alertRepository;

    public AnomalyController(
            AnomalyAgent anomalyAgent,
            AnomalyAlertRepository alertRepository
    ) {

        this.anomalyAgent = anomalyAgent;
        this.alertRepository = alertRepository;
    }

    // =====================================================
    // MANUAL ANOMALY SCAN
    // =====================================================

    @GetMapping("/check")
    public ResponseEntity<?> runManualCheck() {

        String result = anomalyAgent.runManualCheck();

        return ResponseEntity.ok(
                Map.of(
                        "status", "SUCCESS",
                        "message", result
                )
        );
    }

    // =====================================================
    // GET UNREAD ALERTS
    // =====================================================

    @GetMapping("/alerts")
    public ResponseEntity<List<Map<String, Object>>> getUnreadAlerts() {

        return ResponseEntity.ok(
                alertRepository.getUnreadAlerts()
        );
    }

    // =====================================================
    // MARK ALERT AS READ
    // =====================================================

    @PutMapping("/alerts/{id}/read")
    public ResponseEntity<Map<String, Object>> markAlertAsRead(
            @PathVariable Long id
    ) {

        alertRepository.markAsRead(id);

        return ResponseEntity.ok(
                Map.of(
                        "status", "SUCCESS",
                        "message", "Alert marked as read",
                        "alertId", id
                )
        );
    }
}