package com.telecom.insights;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
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

	@Override
	public void run(ApplicationArguments args) {
		logger.info("Checking database connection...");

		try (Connection connection = dataSource.getConnection()) {

			String dbName = connection.getCatalog();

			logger.info("Successfully connected to Database: {}", dbName);
			logger.info("Database Product: {}",
					connection.getMetaData().getDatabaseProductName());

		} catch (Exception e) {
			logger.error("CRITICAL: Failed to connect to database!", e);
		}
	}
}