package com.telecom.insights.repository;

import com.telecom.insights.model.QueryLog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QueryLogRepository
        extends JpaRepository<QueryLog, Long> {

    Optional<QueryLog> findByQuestion(String question);
}