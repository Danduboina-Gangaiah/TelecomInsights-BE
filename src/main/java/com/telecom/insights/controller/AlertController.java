package com.telecom.insights.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    @GetMapping
    public List<Map<String, Object>> getAlerts() {

        return List.of(

                Map.of(
                        "id", 1,
                        "region", "Chicago",
                        "severity", "HIGH",
                        "issue", "Latency Spike",
                        "active", true,
                        "networkDown", false,
                        "packetLoss", 18.5,
                        "recommendedAction", "Restart edge router"
                ),

                Map.of(
                        "id", 2,
                        "region", "Dallas",
                        "severity", "MEDIUM",
                        "issue", "Packet Loss",
                        "active", true,
                        "networkDown", false,
                        "packetLoss", 10.3,
                        "recommendedAction", "Check fiber connectivity"
                ),

                Map.of(
                        "id", 3,
                        "region", "Los Angeles",
                        "severity", "LOW",
                        "issue", "Bandwidth Fluctuation",
                        "active", false,
                        "networkDown", false,
                        "packetLoss", 2.1,
                        "recommendedAction", "Monitor traffic"
                )
        );
    }
}