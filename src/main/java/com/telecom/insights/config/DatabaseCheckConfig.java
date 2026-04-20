package com.telecom.insights.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseCheckConfig implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseCheckConfig.class);
    private final DataSource dataSource;

    public DatabaseCheckConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("---------------------------------------------------------");
        logger.info("Checking Database Connection & Schema...");

        try (Connection connection = dataSource.getConnection()) {
            // For Postgres, getCatalog() is often the DB name
            String dbName = connection.getCatalog();
            String productName = connection.getMetaData().getDatabaseProductName();

            logger.info("Successfully connected to Database: {}", dbName);
            logger.info("Database Product: {}", productName);

            DatabaseMetaData metaData = connection.getMetaData();
            // Use "public" for PostgreSQL default schema
            try (ResultSet tables = metaData.getTables(null, "public", "%", new String[]{"TABLE"})) {
                List<String> tableNames = new ArrayList<>();
                while (tables.next()) {
                    tableNames.add(tables.getString("TABLE_NAME"));
                }

                if (tableNames.isEmpty()) {
                    logger.warn("0 tables currently present in the 'public' schema.");
                } else {
                    logger.info("Tables found ({}): {}", tableNames.size(), String.join(", ", tableNames));
                }
            }
        } catch (Exception e) {
            logger.error("CRITICAL: Failed to connect to the database on startup!", e);
        }
        logger.info("---------------------------------------------------------");
    }
}