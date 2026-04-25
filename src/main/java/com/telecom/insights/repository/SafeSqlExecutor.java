package com.telecom.insights.repository;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Repository
public class SafeSqlExecutor {
    private final JdbcClient jdbcClient;
    public SafeSqlExecutor(JdbcClient jdbcClient){ this.jdbcClient = jdbcClient; }

    public List<Map<String,Object>> executeReadOnlyQuery(String sql){
        String q = sql.trim().toUpperCase();
        if(!q.startsWith("SELECT")) throw new RuntimeException("Only SELECT allowed");
        if(q.contains("DELETE") || q.contains("UPDATE") || q.contains("DROP") || q.contains("INSERT") || q.contains("ALTER")) {
            throw new RuntimeException("Unsafe SQL blocked");
        }
        return jdbcClient.sql(sql).query().listOfRows();
    }
}
