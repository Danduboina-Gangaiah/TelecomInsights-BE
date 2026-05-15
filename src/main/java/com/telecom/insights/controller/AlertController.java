package com.telecom.insights.controller;

import com.telecom.insights.repository.AnomalyAlertRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    private final AnomalyAlertRepository alertRepository;

    public AlertController(
            AnomalyAlertRepository alertRepository
    ) {
        this.alertRepository = alertRepository;
    }

    // GET UNREAD ALERTS
    @GetMapping("/unread")
    public ResponseEntity<List<Map<String, Object>>> getUnreadAlerts() {

        return ResponseEntity.ok(
                alertRepository.getUnreadAlerts()
        );
    }

    // MARK ALERT AS READ
    @PutMapping("/{id}/read")
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