package com.marketinghub.mcpserver.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Responsabilidade: configurar o datasource opcional do schema efetivo do PDE em produção.
 */
@Configuration
public class PdeDatabaseConfig {

    /**
     * Cria o JdbcTemplate usado pelas ferramentas `pde_db_*`.
     */
    @Bean
    @ConditionalOnProperty(name = "mcp.pde.datasource.url")
    public JdbcTemplate pdeJdbcTemplate(Environment environment) {
        DriverManagerDataSource pdeDataSource = new DriverManagerDataSource();
        pdeDataSource.setUrl(environment.getRequiredProperty("mcp.pde.datasource.url"));
        pdeDataSource.setUsername(environment.getRequiredProperty("mcp.pde.datasource.username"));
        pdeDataSource.setPassword(environment.getProperty("mcp.pde.datasource.password", ""));
        pdeDataSource.setDriverClassName(environment.getProperty(
                "mcp.pde.datasource.driver-class-name",
                "com.mysql.cj.jdbc.Driver"));
        return new JdbcTemplate(pdeDataSource);
    }
}
