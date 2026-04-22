package com.telecom.insights.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "query_logs")
public class QueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique question for caching
    @Column(unique = true, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String response;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Default constructor
    public QueryLog() {}

    public QueryLog(String question, String response) {
        this.question = question;
        this.response = response;
        this.createdAt = LocalDateTime.now();
    }

    // GETTERS
    public Long getId() { return id; }

    public String getQuestion() { return question; }

    public String getResponse() { return response; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    // SETTERS
    public void setQuestion(String question) {
        this.question = question;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}