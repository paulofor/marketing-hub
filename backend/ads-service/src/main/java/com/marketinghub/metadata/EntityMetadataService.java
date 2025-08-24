package com.marketinghub.metadata;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntityMetadataService {
    private final JdbcTemplate jdbcTemplate;

    public EntityMetadataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> listEntities() {
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public List<String> listAttributes(String entityName) {
        String sql = "SELECT column_name FROM information_schema.columns WHERE table_name = ?";
        return jdbcTemplate.queryForList(sql, String.class, entityName);
    }
}
