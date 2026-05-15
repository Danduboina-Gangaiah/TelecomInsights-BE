package com.telecom.insights.service;

import com.telecom.insights.model.AnomalyResult;
import com.telecom.insights.model.KpiReading;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnomalyDetectionService {

    public List<AnomalyResult> detectAnomalies(List<KpiReading> data) {

        List<AnomalyResult> anomalies = new ArrayList<>();

        if (data == null || data.isEmpty()) return anomalies;

        // Calculate averages
        double avgLatency = data.stream().mapToDouble(KpiReading::getLatency).average().orElse(0);
        double avgPacketLoss = data.stream().mapToDouble(KpiReading::getPacketloss).average().orElse(0);
        double avgDownloadSpeed = data.stream().mapToDouble(KpiReading::getDownloadSpeed).average().orElse(0);

        for (KpiReading reading : data) {

            // 🔴 LATENCY ANOMALY
            if (reading.getLatency() > avgLatency * 1.5) {
                anomalies.add(buildResult(reading, "Latency", reading.getLatency(), "HIGH LATENCY"));
            }

            // 🔴 PACKET LOSS ANOMALY
            if (reading.getPacketloss() > avgPacketLoss * 2) {
                anomalies.add(buildResult(reading, "PacketLoss", reading.getPacketloss(), "HIGH PACKET LOSS"));
            }

            // 🔴 DOWNLOAD SPEED ANOMALY
            if (reading.getDownloadSpeed() < avgDownloadSpeed * 0.5) {
                anomalies.add(buildResult(reading, "DownloadSpeed", reading.getDownloadSpeed(), "LOW DOWNLOAD SPEED"));
            }
        }

        return anomalies;
    }

    private AnomalyResult buildResult(KpiReading reading, String metric, double value, String message) {

        AnomalyResult result = new AnomalyResult();

        result.setRegion(reading.getRegion());
        result.setMetric(metric);
        result.setValue(value);
        result.setTimestamp(reading.getTimestamp());
        result.setSeverity(getSeverity(metric, value));
        result.setMessage(message);

        return result;
    }

    private String getSeverity(String metric, double value) {

        switch (metric) {

            case "Latency":
                if (value > 200) return "HIGH";
                if (value > 100) return "MEDIUM";
                break;

            case "PacketLoss":
                if (value > 5) return "HIGH";
                if (value > 2) return "MEDIUM";
                break;

            case "DownloadSpeed":
                if (value < 10) return "HIGH";
                if (value < 25) return "MEDIUM";
                break;
        }

        return "LOW";
    }
}