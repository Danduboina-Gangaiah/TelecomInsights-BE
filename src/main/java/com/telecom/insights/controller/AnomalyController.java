package com.telecom.insights.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/anomaly")
public class AnomalyController {

    @GetMapping("/check")
    public Map<String, Object> checkAnomaly() {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("source", "ANOMALY_AGENT");
        response.put("status", "SUCCESS");
        response.put("anomalyCount", 1);

        List<Map<String, Object>> anomalies =
                new ArrayList<>();

        Map<String, Object> anomaly =
                new LinkedHashMap<>();

        anomaly.put("region", "Chicago");
        anomaly.put("severity", "HIGH");
        anomaly.put("issue", "Latency Spike");

        anomalies.add(anomaly);

        response.put("anomalies", anomalies);

        return response;
    }
}