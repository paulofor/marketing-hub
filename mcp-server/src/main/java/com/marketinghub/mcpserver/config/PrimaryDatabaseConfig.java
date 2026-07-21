package com.marketinghub.mcpserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Responsabilidade: declarar explicitamente o JdbcTemplate principal do MCP.
 */
@Configuration
public class PrimaryDatabaseConfig {

    /**
     * Cria o JdbcTemplate primário usado pelas ferramentas `db_*`.
     */
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
