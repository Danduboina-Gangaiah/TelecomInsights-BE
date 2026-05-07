package com.telecom.insights.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AnomalyAlert {

    private Long id;
    private LocalDateTime createdAt;
    private String severity;
    private String title;
    private String message;
    private String rawData;
    private boolean isRead;

    public AnomalyAlert(String severity, String title, String message, String rawData) {
        this.createdAt = LocalDateTime.now();
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.rawData = rawData;
        this.isRead = false;
    }
}