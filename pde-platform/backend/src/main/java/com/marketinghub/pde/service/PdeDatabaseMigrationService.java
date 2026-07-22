package com.marketinghub.pde.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Executa migrações idempotentes do schema operacional do backend PDE. */
@Service
public class PdeDatabaseMigrationService {
    private static final Logger log = LoggerFactory.getLogger(PdeDatabaseMigrationService.class);
    private static final String FUNNEL_EVENT_TABLE = "pde_funnel_event";
    private static final String EXPERIENCE_VERSION_COLUMN = "experience_version";
    private static final String EXPERIENCE_VERSION_INDEX = "idx_pde_funnel_product_version_time";
    private static final String AI_GUIDANCE_TABLE = "pde_ai_guidance_request";
    private static final String ACCESS_TOKEN_COLUMN = "access_token";
    private static final int ACCESS_TOKEN_MIN_LENGTH = 120;

    private final String jdbcUrl;
    private final String jdbcUsername;
    private final String jdbcPassword;
    private final JdbcConnectionProvider connectionProvider;

    /** Recebe a configuração JDBC usada pelo PDE em produção. */
    @Autowired
    public PdeDatabaseMigrationService(
            @Value("${pde.access.jdbc-url:}") String jdbcUrl,
            @Value("${pde.access.jdbc-username:}") String jdbcUsername,
            @Value("${pde.access.jdbc-password:}") String jdbcPassword) {
        this(jdbcUrl, jdbcUsername, jdbcPassword, DriverManager::getConnection);
    }

    /** Recebe dependências controladas para testes da migração. */
    PdeDatabaseMigrationService(
            String jdbcUrl, String jdbcUsername, String jdbcPassword, JdbcConnectionProvider connectionProvider) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
        this.connectionProvider = connectionProvider;
    }

    /** Aplica as migrações necessárias antes do PDE ler ou gravar analytics. */
    public void migrateIfNeeded() {
        if (!usesJdbcStorage()) {
            return;
        }
        try (Connection connection = connectionProvider.open(jdbcUrl, jdbcUsername, jdbcPassword)) {
            migrateFunnelEventExperienceVersion(connection);
            migrateAiGuidanceAccessTokenLength(connection);
        } catch (SQLException ex) {
            log.error("Falha ao migrar schema operacional do PDE", ex);
            throw new IllegalStateException("Não foi possível migrar schema operacional do PDE", ex);
        }
    }

    /** Garante coluna e índice usados para comparar métricas por versão comercial do PDE. */
    private void migrateFunnelEventExperienceVersion(Connection connection) throws SQLException {
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                FUNNEL_EVENT_TABLE)) {
            log.warn("Migração PDE ignorada porque a tabela de funil ainda não existe; table={}", FUNNEL_EVENT_TABLE);
            return;
        }
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                FUNNEL_EVENT_TABLE,
                EXPERIENCE_VERSION_COLUMN)) {
            executeSql(connection, "ALTER TABLE pde_funnel_event ADD COLUMN experience_version VARCHAR(80) NULL AFTER product_slug");
            log.info("Coluna de versão comercial criada no funil PDE; column={}", EXPERIENCE_VERSION_COLUMN);
        }
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                FUNNEL_EVENT_TABLE,
                EXPERIENCE_VERSION_INDEX)) {
            executeSql(
                    connection,
                    "ALTER TABLE pde_funnel_event "
                            + "ADD KEY idx_pde_funnel_product_version_time "
                            + "(product_slug(100), experience_version(80), occurred_at)");
            log.info("Índice de versão comercial criado no funil PDE; index={}", EXPERIENCE_VERSION_INDEX);
        }
    }

    /** Garante espaço suficiente para tokens públicos de diagnóstico com prefixo e UUID. */
    private void migrateAiGuidanceAccessTokenLength(Connection connection) throws SQLException {
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                AI_GUIDANCE_TABLE)) {
            log.warn("Migração PDE ignorada porque a tabela de orientação IA ainda não existe; table={}", AI_GUIDANCE_TABLE);
            return;
        }
        Integer currentLength = columnMaxLength(connection, AI_GUIDANCE_TABLE, ACCESS_TOKEN_COLUMN);
        if (currentLength != null && currentLength < ACCESS_TOKEN_MIN_LENGTH) {
            executeSql(connection, "ALTER TABLE pde_ai_guidance_request MODIFY COLUMN access_token VARCHAR(120) NOT NULL");
            log.info(
                    "Coluna de token da orientação IA ampliada; column={}, previousLength={}, newLength={}",
                    ACCESS_TOKEN_COLUMN,
                    currentLength,
                    ACCESS_TOKEN_MIN_LENGTH);
        }
    }

    /** Verifica existência de tabela, coluna ou índice no schema atual. */
    private boolean objectExists(Connection connection, String sql, String... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                statement.setString(index + 1, values[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong(1) > 0;
            }
        }
    }

    /** Lê o tamanho máximo de uma coluna textual no schema atual. */
    private Integer columnMaxLength(Connection connection, String tableName, String columnName) throws SQLException {
        String sql = "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                int length = resultSet.getInt(1);
                return resultSet.wasNull() ? null : length;
            }
        }
    }

    /** Executa uma alteração DDL simples no schema PDE. */
    private void executeSql(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    /** Informa se existe banco JDBC configurado para o PDE. */
    private boolean usesJdbcStorage() {
        return jdbcUrl != null && !jdbcUrl.isBlank();
    }

    /** Abre conexão JDBC para permitir teste isolado sem DriverManager real. */
    @FunctionalInterface
    interface JdbcConnectionProvider {
        /** Cria uma conexão JDBC com as credenciais informadas. */
        Connection open(String url, String username, String password) throws SQLException;
    }
}
