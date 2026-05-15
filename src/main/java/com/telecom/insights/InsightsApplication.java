package com.telecom.insights;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
@EnableScheduling
public class InsightsApplication implements ApplicationRunner {

	private static final Logger logger =
			LoggerFactory.getLogger(InsightsApplication.class);

	private final DataSource dataSource;

	public InsightsApplication(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static void main(String[] args) {
		SpringApplication.run(InsightsApplication.class, args);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Override
	public void run(ApplicationArguments args) {

		logger.info("Checking database connection...");

		try (Connection connection = dataSource.getConnection()) {

			logger.info("Successfully connected to Database: {}", connection.getCatalog());
			logger.info("Database Product: {}", connection.getMetaData().getDatabaseProductName());

			logger.info("Application started successfully.");
			logger.info("Base URL: http://localhost:9021/insights");

		} catch (Exception e) {
			logger.error("CRITICAL: Failed to connect to the database!", e);
		}
	}
}