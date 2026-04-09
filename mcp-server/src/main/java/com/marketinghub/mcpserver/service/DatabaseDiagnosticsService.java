package com.marketinghub.mcpserver.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DatabaseDiagnosticsService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseDiagnosticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> checkConnection() {
        Map<String, Object> response = new LinkedHashMap<>();
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        response.put("status", one != null && one == 1 ? "ok" : "unexpected");
        response.put("database", databaseName);
        return response;
    }
}
