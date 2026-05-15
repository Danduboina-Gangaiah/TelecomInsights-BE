package com.telecom.insights.model;

import java.time.Instant;

public class KpiReading {

    private String region;
    private double latency;
    private double packetloss;
    private double downloadSpeed;
    private Instant timestamp;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public double getLatency() {
        return latency;
    }

    public void setLatency(double latency) {
        this.latency = latency;
    }

    public double getPacketloss() {
        return packetloss;
    }

    public void setPacketloss(double packetloss) {
        this.packetloss = packetloss;
    }

    public double getDownloadSpeed() {
        return downloadSpeed;
    }

    public void setDownloadSpeed(double downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}