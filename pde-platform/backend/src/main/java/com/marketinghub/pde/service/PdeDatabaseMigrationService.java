package com.marketinghub.pde.service;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static final String CLIENT_IP_COLUMN = "client_ip";
    private static final String USER_AGENT_COLUMN = "user_agent";
    private static final String TRAFFIC_QUALITY_COLUMN = "traffic_quality";
    private static final String TRAFFIC_QUALITY_REASON_COLUMN = "traffic_quality_reason";
    private static final String TRAFFIC_PROVIDER_COLUMN = "traffic_provider";
    private static final String TRAFFIC_QUALITY_INDEX = "idx_pde_funnel_product_quality_time";
    private static final String TRAFFIC_SOURCE_INDEX = "idx_pde_funnel_product_quality_utm_session";
    private static final String JOURNEY_SESSION_INDEX = "idx_pde_funnel_product_quality_session_time";
    private static final String JOURNEY_RECENT_INDEX = "idx_pde_funnel_product_quality_time_id";
    private static final String AI_GUIDANCE_TABLE = "pde_ai_guidance_request";
    private static final String ACCESS_TOKEN_COLUMN = "access_token";
    private static final String AI_GUIDANCE_ACCESS_GRANT_FK = "fk_pde_ai_guidance_access_grant";
    private static final int ACCESS_TOKEN_MIN_LENGTH = 120;
    private static final String OPERATIONAL_FAILURE_TABLE = "pde_operational_endpoint_failure";

    private final String jdbcUrl;
    private final String jdbcUsername;
    private final String jdbcPassword;
    private final JdbcConnectionProvider connectionProvider;
    private final AtomicBoolean migrated = new AtomicBoolean(false);

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

    /** Aplica automaticamente as migrações do schema PDE ao iniciar o serviço. */
    @PostConstruct
    public void migrateOnStartup() {
        migrateIfNeeded();
    }

    /** Aplica as migrações necessárias antes do PDE ler ou gravar dados operacionais. */
    public void migrateIfNeeded() {
        if (!usesJdbcStorage()) {
            return;
        }
        if (!migrated.compareAndSet(false, true)) {
            return;
        }
        try (Connection connection = connectionProvider.open(jdbcUrl, jdbcUsername, jdbcPassword)) {
            migrateFunnelEventExperienceVersion(connection);
            migrateAiGuidanceStorageTable(connection);
            migrateOperationalFailureTable(connection);
        } catch (SQLException ex) {
            migrated.set(false);
            log.error("Falha ao migrar schema operacional do PDE", ex);
            throw new IllegalStateException("Não foi possível migrar schema operacional do PDE", ex);
        }
    }

    /** Garante sob demanda que a persistência de orientação por IA aceita o diagnóstico público. */
    public void ensureAiGuidanceStorageReady() {
        if (!usesJdbcStorage()) {
            return;
        }
        try (Connection connection = connectionProvider.open(jdbcUrl, jdbcUsername, jdbcPassword)) {
            migrateAiGuidanceStorageTable(connection);
        } catch (SQLException ex) {
            log.error("Falha ao preparar schema de orientação IA do PDE", ex);
            throw new IllegalStateException("Não foi possível preparar schema de orientação IA do PDE", ex);
        }
    }

    /** Garante a tabela que transforma erro técnico em alerta comercial pós-deploy. */
    private void migrateOperationalFailureTable(Connection connection) throws SQLException {
        if (objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                OPERATIONAL_FAILURE_TABLE)) {
            return;
        }
        executeSql(
                connection,
                """
                CREATE TABLE pde_operational_endpoint_failure (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  endpoint VARCHAR(255) NOT NULL,
                  http_method VARCHAR(12) NOT NULL,
                  http_status INT NOT NULL,
                  funnel_stage VARCHAR(80) NOT NULL,
                  error_class VARCHAR(191) NOT NULL,
                  error_message VARCHAR(1000) NULL,
                  occurred_at DATETIME NOT NULL,
                  PRIMARY KEY (id),
                  KEY idx_pde_operational_failure_endpoint_time (endpoint, occurred_at),
                  KEY idx_pde_operational_failure_stage_time (funnel_stage, occurred_at)
                )
                """);
        log.info("Tabela de falhas operacionais PDE criada; table={}", OPERATIONAL_FAILURE_TABLE);
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
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                FUNNEL_EVENT_TABLE,
                CLIENT_IP_COLUMN)) {
            executeSql(connection, "ALTER TABLE pde_funnel_event ADD COLUMN client_ip VARCHAR(45) NULL AFTER page_url");
            log.info("Coluna de IP do visitante criada no funil PDE; column={}", CLIENT_IP_COLUMN);
        }
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                FUNNEL_EVENT_TABLE,
                USER_AGENT_COLUMN)) {
            executeSql(connection, "ALTER TABLE pde_funnel_event ADD COLUMN user_agent VARCHAR(512) NULL AFTER client_ip");
            log.info("Coluna de user-agent criada no funil PDE; column={}", USER_AGENT_COLUMN);
        }
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                FUNNEL_EVENT_TABLE,
                TRAFFIC_QUALITY_COLUMN)) {
            executeSql(connection, "ALTER TABLE pde_funnel_event ADD COLUMN traffic_quality VARCHAR(40) NULL AFTER user_agent");
            log.info("Coluna de qualidade de tráfego criada no funil PDE; column={}", TRAFFIC_QUALITY_COLUMN);
        }
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                FUNNEL_EVENT_TABLE,
                TRAFFIC_QUALITY_REASON_COLUMN)) {
            executeSql(connection,
                    "ALTER TABLE pde_funnel_event ADD COLUMN traffic_quality_reason VARCHAR(120) NULL AFTER traffic_quality");
            log.info("Coluna de motivo de qualidade de tráfego criada no funil PDE; column={}", TRAFFIC_QUALITY_REASON_COLUMN);
        }
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                FUNNEL_EVENT_TABLE,
                TRAFFIC_PROVIDER_COLUMN)) {
            executeSql(connection, "ALTER TABLE pde_funnel_event ADD COLUMN traffic_provider VARCHAR(80) NULL AFTER traffic_quality_reason");
            log.info("Coluna de provedor de tráfego criada no funil PDE; column={}", TRAFFIC_PROVIDER_COLUMN);
        }
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                FUNNEL_EVENT_TABLE,
                TRAFFIC_QUALITY_INDEX)) {
            executeSql(
                    connection,
                    "ALTER TABLE pde_funnel_event "
                            + "ADD KEY idx_pde_funnel_product_quality_time "
                            + "(product_slug(100), traffic_quality(40), occurred_at)");
            log.info("Índice de qualidade de tráfego criado no funil PDE; index={}", TRAFFIC_QUALITY_INDEX);
        }
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                FUNNEL_EVENT_TABLE,
                TRAFFIC_SOURCE_INDEX)) {
            executeSql(
                    connection,
                    "ALTER TABLE pde_funnel_event "
                            + "ADD KEY idx_pde_funnel_product_quality_utm_session "
                            + "(product_slug(80), traffic_quality(40), utm_source(60), utm_medium(60), "
                            + "utm_campaign(80), utm_content(80), session_id(64), occurred_at)");
            log.info("Índice de origem de tráfego criado no funil PDE; index={}", TRAFFIC_SOURCE_INDEX);
        }
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                FUNNEL_EVENT_TABLE,
                JOURNEY_SESSION_INDEX)) {
            executeSql(
                    connection,
                    "ALTER TABLE pde_funnel_event "
                            + "ADD KEY idx_pde_funnel_product_quality_session_time "
                            + "(product_slug(80), traffic_quality(40), session_id(64), occurred_at)");
            log.info("Índice de jornadas recentes criado no funil PDE; index={}", JOURNEY_SESSION_INDEX);
        }
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                FUNNEL_EVENT_TABLE,
                JOURNEY_RECENT_INDEX)) {
            executeSql(
                    connection,
                    "ALTER TABLE pde_funnel_event "
                            + "ADD KEY idx_pde_funnel_product_quality_time_id "
                            + "(product_slug(80), traffic_quality(40), occurred_at, id)");
            log.info("Índice de leitura recente criado no funil PDE; index={}", JOURNEY_RECENT_INDEX);
        }
    }

    /** Garante tabela e coluna para salvar orientação IA antes de existir acesso pago/logado. */
    private void migrateAiGuidanceStorageTable(Connection connection) throws SQLException {
        if (!objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                AI_GUIDANCE_TABLE)) {
            createAiGuidanceTable(connection);
        }
        if (objectExists(
                connection,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?",
                AI_GUIDANCE_TABLE,
                AI_GUIDANCE_ACCESS_GRANT_FK)) {
            executeSql(connection, "ALTER TABLE pde_ai_guidance_request DROP FOREIGN KEY fk_pde_ai_guidance_access_grant");
            log.info("Chave estrangeira incompatível com diagnóstico público removida; constraint={}", AI_GUIDANCE_ACCESS_GRANT_FK);
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

    /** Cria a tabela canônica de solicitações de orientação por IA do PDE. */
    private void createAiGuidanceTable(Connection connection) throws SQLException {
        executeSql(
                connection,
                """
                CREATE TABLE pde_ai_guidance_request (
                  request_id VARCHAR(36) NOT NULL,
                  access_token VARCHAR(120) NOT NULL,
                  product_slug VARCHAR(120) NOT NULL,
                  email VARCHAR(255) NOT NULL,
                  mission_id VARCHAR(120) NOT NULL,
                  guidance_type VARCHAR(120) NOT NULL,
                  stage_code VARCHAR(80) NOT NULL,
                  status VARCHAR(40) NOT NULL,
                  answers_json TEXT NOT NULL,
                  previous_answers_json TEXT NOT NULL,
                  headline VARCHAR(500) NULL,
                  summary TEXT NULL,
                  signals_json TEXT NOT NULL,
                  micro_actions_json TEXT NOT NULL,
                  caution TEXT NULL,
                  model VARCHAR(80) NULL,
                  service_tier VARCHAR(40) NULL,
                  raw_request_json MEDIUMTEXT NULL,
                  raw_response_json MEDIUMTEXT NULL,
                  input_tokens INT NULL,
                  output_tokens INT NULL,
                  cost_usd DECIMAL(12,6) NULL,
                  error_message TEXT NULL,
                  created_at DATETIME NOT NULL,
                  finished_at DATETIME NULL,
                  updated_at DATETIME NOT NULL,
                  PRIMARY KEY (request_id),
                  KEY idx_pde_ai_guidance_status_created (status, created_at),
                  KEY idx_pde_ai_guidance_access_token (access_token),
                  KEY idx_pde_ai_guidance_product_type (product_slug, guidance_type)
                )
                """);
        log.info("Tabela de orientação IA PDE criada; table={}", AI_GUIDANCE_TABLE);
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
