package com.telecom.insights.model;

import java.time.Instant;

public class AnomalyResult {

    private String region;
    private String metric;   // latency / packetloss / downloadSpeed
    private double value;
    private String severity;
    private String message;
    private Instant timestamp;

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}