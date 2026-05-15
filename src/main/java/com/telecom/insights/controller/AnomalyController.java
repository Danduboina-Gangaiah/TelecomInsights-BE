package com.telecom.insights.controller;

import com.telecom.insights.agent.AnomalyAgent;
import com.telecom.insights.repository.AnomalyAlertRepository;

import org.springframework.http.ResponseEntity;
<<<<<<< Updated upstream
=======

>>>>>>> Stashed changes
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
<<<<<<< Updated upstream
@RequestMapping("/anomaly")
=======
@RequestMapping("/api/v1/anomaly")
>>>>>>> Stashed changes
@CrossOrigin(origins = "*")
public class AnomalyController {

    private final AnomalyAgent anomalyAgent;

    private final AnomalyAlertRepository alertRepository;

    public AnomalyController(
            AnomalyAgent anomalyAgent,
            AnomalyAlertRepository alertRepository
    ) {

        this.anomalyAgent = anomalyAgent;
<<<<<<< Updated upstream
=======

>>>>>>> Stashed changes
        this.alertRepository = alertRepository;
    }

    // =====================================================
    // MANUAL ANOMALY SCAN
    // =====================================================

    @GetMapping("/check")
    public ResponseEntity<?> runManualCheck() {

<<<<<<< Updated upstream
        String result = anomalyAgent.runManualCheck();
=======
        String result =
                anomalyAgent.runManualCheck();
>>>>>>> Stashed changes

        return ResponseEntity.ok(
                Map.of(
                        "status", "SUCCESS",
                        "message", result
                )
        );
    }

    // =====================================================
<<<<<<< Updated upstream
    // GET UNREAD ALERTS
    // =====================================================

    @GetMapping("/alerts")
    public ResponseEntity<List<Map<String, Object>>> getUnreadAlerts() {
=======
    // FETCH GENERATED ALERTS
    // =====================================================

    @GetMapping("/alerts")
    public ResponseEntity<List<Map<String, Object>>> getAlerts() {
>>>>>>> Stashed changes

        return ResponseEntity.ok(
                alertRepository.getUnreadAlerts()
        );
<<<<<<< Updated upstream
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
=======
>>>>>>> Stashed changes
    }
}