package com.marketinghub.mcpserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Pattern;

@Component
public class DatabaseConnectionAttemptLogger {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionAttemptLogger.class);
    private static final Pattern JDBC_CREDENTIALS_PATTERN = Pattern.compile(
            "(?i)(jdbc:[^:]+://)([^:@/]+):([^@/]+)@"
    );

    private final String datasourceUrl;
    private final String datasourceUsername;

    public DatabaseConnectionAttemptLogger(
            @Value("${spring.datasource.url}") String datasourceUrl,
            @Value("${spring.datasource.username}") String datasourceUsername
    ) {
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void logConnectionTarget() {
        DatabaseTarget databaseTarget = parseDatabaseTarget(datasourceUrl);
        String sanitizedDatasourceUrl = sanitizeJdbcUrl(datasourceUrl);
        logger.info(
                "Tentando conexão com banco de dados jdbcUrl='{}' host='{}' port='{}' user='{}'",
                sanitizedDatasourceUrl,
                databaseTarget.host(),
                databaseTarget.port(),
                datasourceUsername
        );
    }

    private String sanitizeJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "unknown";
        }
        return JDBC_CREDENTIALS_PATTERN.matcher(jdbcUrl).replaceAll("$1$2:***@");
    }

    private DatabaseTarget parseDatabaseTarget(String jdbcUrl) {
        try {
            String withoutPrefix = jdbcUrl.replaceFirst("^jdbc:", "");
            URI uri = URI.create(withoutPrefix);
            String host = uri.getHost() == null ? "unknown" : uri.getHost();
            String port = uri.getPort() == -1 ? "3306" : String.valueOf(uri.getPort());
            return new DatabaseTarget(host, port);
        } catch (RuntimeException ex) {
            logger.warn("Não foi possível interpretar a URL do banco para log de diagnóstico: {}", jdbcUrl);
            return new DatabaseTarget("unknown", "unknown");
        }
    }

    private record DatabaseTarget(String host, String port) {
    }
}
