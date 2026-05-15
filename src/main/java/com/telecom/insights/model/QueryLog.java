package com.telecom.insights.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "query_logs")
public class QueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 1000)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String responseJson;

    private LocalDateTime createdAt;

    public QueryLog() {
    }

    public QueryLog(String question, String responseJson) {
        this.question = question;
        this.responseJson = responseJson;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}