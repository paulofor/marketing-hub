package com.marketinghub.mcpserver.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    public Map<String, Object> listTables() {
        String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        List<String> tables = jdbcTemplate.queryForList(
                """
                        SELECT TABLE_NAME
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE UPPER(TABLE_SCHEMA) = UPPER(DATABASE())
                          AND TABLE_TYPE = 'BASE TABLE'
                        ORDER BY TABLE_NAME
                        """,
                String.class
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("database", databaseName);
        response.put("tableCount", tables.size());
        response.put("tables", tables);
        return response;
    }

    public Map<String, Object> readTable(String tableName, int limit, int offset) {
        validateTableName(tableName);

        String safeTableName = "`" + tableName + "`";
        String countSql = "SELECT COUNT(*) FROM " + safeTableName;
        String rowsSql = "SELECT * FROM " + safeTableName + " LIMIT ? OFFSET ?";

        Integer totalRows = jdbcTemplate.queryForObject(countSql, Integer.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(rowsSql, limit, offset);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("table", tableName);
        response.put("limit", limit);
        response.put("offset", offset);
        response.put("returnedRows", rows.size());
        response.put("totalRows", totalRows == null ? 0 : totalRows);
        response.put("rows", rows);
        return response;
    }

    public Map<String, Object> query(String sql, int limit) {
        String normalizedSql = normalizeAndValidateReadOnlySql(sql);
        String sqlWithLimit = ensureLimit(normalizedSql, limit);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sqlWithLimit);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", normalizedSql);
        response.put("appliedLimit", limit);
        response.put("returnedRows", rows.size());
        response.put("rows", rows);
        return response;
    }

    private void validateTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("table is required");
        }

        if (!tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("table contains invalid characters");
        }
    }

    private String normalizeAndValidateReadOnlySql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }

        String normalized = sql.trim();
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }

        if (normalized.contains(";")) {
            throw new IllegalArgumentException("only one SQL statement is allowed");
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("select ") || lower.startsWith("with "))) {
            throw new IllegalArgumentException("only SELECT queries are allowed");
        }

        return normalized;
    }

    private String ensureLimit(String sql, int limit) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (lower.contains(" limit ")) {
            return sql;
        }
        return sql + " LIMIT " + limit;
    }
}
