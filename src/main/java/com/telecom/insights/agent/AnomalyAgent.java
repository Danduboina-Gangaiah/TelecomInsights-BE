package com.telecom.insights.agent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AnomalyAgent {

    public Map<String, Object> runManualCheck() {

        boolean anomalyDetected = true;

        return Map.of(

                "source", "ANOMALY_AGENT",

                "status", "SUCCESS",

                "anomalyDetected", anomalyDetected,

                "severity", "HIGH",

                "affectedRegion", "Chicago",

                "issue", "Latency Spike",

                "recommendedAction",
                "Investigate tower congestion and packet routing",

                "networkStable", false,

                "alertsGenerated", 1,

                "anomalies", List.of(

                        Map.of(
                                "region", "Chicago",
                                "latency", 420,
                                "packetLoss", 18.5,
                                "severity", "HIGH",
                                "status", "CRITICAL"
                        ),

                        Map.of(
                                "region", "Dallas",
                                "latency", 310,
                                "packetLoss", 12.1,
                                "severity", "MEDIUM",
                                "status", "WARNING"
                        )
                )
        );
    }
}