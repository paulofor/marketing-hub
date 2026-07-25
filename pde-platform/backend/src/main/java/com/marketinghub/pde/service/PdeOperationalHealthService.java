package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.DeployOperationalAlertResponse;
import com.marketinghub.pde.dto.DeploySchemaStatusResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Registra falhas técnicas do PDE e converte o histórico em alertas comerciais. */
@Service
public class PdeOperationalHealthService {

    private static final int AI_GUIDANCE_ACCESS_TOKEN_MIN_LENGTH = 120;
    private static final String FUNNEL_EVENT_TABLE = "pde_funnel_event";
    private static final String AI_GUIDANCE_TABLE = "pde_ai_guidance_request";
    private static final String ACCESS_TOKEN_COLUMN = "access_token";
    private static final String FAILURE_TABLE = "pde_operational_endpoint_failure";
    private static final List<String> FUNNEL_ANALYTICS_COLUMNS = List.of(
            "session_id",
            "visitor_id",
            "utm_source",
            "utm_medium",
            "utm_campaign",
            "utm_content",
            "utm_term",
            "device_type",
            "screen_width",
            "screen_height",
            "viewport_width",
            "viewport_height",
            "visible_ms",
            "section_id",
            "action_name");

    private final String jdbcUrl;
    private final String jdbcUsername;
    private final String jdbcPassword;
    private final JdbcConnectionProvider connectionProvider;

    /** Recebe a configuração JDBC usada para persistir e ler saúde operacional. */
    public PdeOperationalHealthService(
            @Value("${pde.access.jdbc-url:}") String jdbcUrl,
            @Value("${pde.access.jdbc-username:}") String jdbcUsername,
            @Value("${pde.access.jdbc-password:}") String jdbcPassword) {
        this(jdbcUrl, jdbcUsername, jdbcPassword, DriverManager::getConnection);
    }

    /** Recebe dependências controladas para validar saúde operacional em testes. */
    PdeOperationalHealthService(
            String jdbcUrl, String jdbcUsername, String jdbcPassword, JdbcConnectionProvider connectionProvider) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
        this.connectionProvider = connectionProvider;
    }

    /** Registra uma falha HTTP que pode distorcer métricas do funil PDE. */
    public void recordEndpointFailure(HttpServletRequest request, int httpStatus, Exception exception) {
        if (!usesJdbcStorage()) {
            return;
        }
        String endpoint = request == null ? "unknown" : normalizeEndpoint(request.getRequestURI());
        String method = request == null ? "UNKNOWN" : nullToDefault(request.getMethod(), "UNKNOWN");
        String sql = """
                INSERT INTO pde_operational_endpoint_failure (
                  endpoint, http_method, http_status, funnel_stage, error_class, error_message, occurred_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, endpoint);
            statement.setString(2, method);
            statement.setInt(3, httpStatus);
            statement.setString(4, resolveFunnelStage(endpoint));
            statement.setString(5, exception.getClass().getName());
            statement.setString(6, truncate(exception.getMessage(), 1000));
            statement.setTimestamp(7, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        } catch (SQLException ex) {
            org.slf4j.LoggerFactory.getLogger(PdeOperationalHealthService.class)
                    .error("Falha ao registrar erro operacional PDE; endpoint={}, httpStatus={}", endpoint, httpStatus, ex);
        }
    }

    /** Retorna o schema real usado para saber se o diagnóstico público está pronto para tráfego. */
    public DeploySchemaStatusResponse schemaStatus() {
        if (!usesJdbcStorage()) {
            return new DeploySchemaStatusResponse(false, false, false, false, null, false, false);
        }
        try (Connection connection = openConnection()) {
            boolean funnelEventTableExists = tableExists(connection, FUNNEL_EVENT_TABLE);
            boolean aiGuidanceTableExists = tableExists(connection, AI_GUIDANCE_TABLE);
            Integer accessTokenLength = aiGuidanceTableExists
                    ? columnMaxLength(connection, AI_GUIDANCE_TABLE, ACCESS_TOKEN_COLUMN)
                    : null;
            return new DeploySchemaStatusResponse(
                    true,
                    funnelEventTableExists,
                    funnelEventTableExists && columnsExist(connection, FUNNEL_EVENT_TABLE, FUNNEL_ANALYTICS_COLUMNS),
                    aiGuidanceTableExists,
                    accessTokenLength,
                    accessTokenLength != null && accessTokenLength >= AI_GUIDANCE_ACCESS_TOKEN_MIN_LENGTH,
                    tableExists(connection, FAILURE_TABLE));
        } catch (SQLException ex) {
            return new DeploySchemaStatusResponse(true, false, false, false, null, false, false);
        }
    }

    /** Consolida alertas que devem aparecer junto das métricas pós-deploy. */
    public List<DeployOperationalAlertResponse> operationalAlerts() {
        List<DeployOperationalAlertResponse> alerts = new ArrayList<>();
        DeploySchemaStatusResponse schema = schemaStatus();
        if (schema.jdbcConfigured() && !schema.aiGuidanceAccessTokenReady()) {
            alerts.add(new DeployOperationalAlertResponse(
                    "CRITICAL",
                    "SCHEMA_MISMATCH",
                    "DIAGNOSTICO_PUBLICO_IA",
                    "/api/pde/public/presence-diagnostic",
                    "Schema real não suporta o token público usado pelo diagnóstico da v3.",
                    "pde_ai_guidance_request.access_token="
                            + (schema.aiGuidanceAccessTokenLength() == null ? "ausente" : schema.aiGuidanceAccessTokenLength()),
                    0,
                    null,
                    "Rodar deploy com migração do backend PDE e validar envio completo do diagnóstico."));
        }
        if (schema.jdbcConfigured() && !schema.funnelAnalyticsFieldsReady()) {
            alerts.add(new DeployOperationalAlertResponse(
                    "CRITICAL",
                    "SCHEMA_MISMATCH",
                    "TRACKING_FUNIL",
                    "/api/pde/access/analytics/{productSlug}/summary",
                    "Schema real não confirma todos os campos necessários para métricas de dispositivo e tela.",
                    "pde_funnel_event.analyticsFieldsReady=" + schema.funnelAnalyticsFieldsReady(),
                    0,
                    null,
                    "Publicar o backend PDE com migração/contrato de analytics e validar deviceBreakdown e screenSizeBreakdown no resumo."));
        }
        if (schema.operationalFailureTableExists()) {
            alerts.addAll(loadRecentFailureAlerts());
        }
        return alerts;
    }

    /** Carrega falhas 500 recentes agrupadas por endpoint e etapa de funil. */
    private List<DeployOperationalAlertResponse> loadRecentFailureAlerts() {
        String sql = """
                SELECT endpoint, http_method, http_status, funnel_stage, error_class,
                       COUNT(*) AS failures, MAX(occurred_at) AS last_seen_at
                FROM pde_operational_endpoint_failure
                WHERE occurred_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
                GROUP BY endpoint, http_method, http_status, funnel_stage, error_class
                ORDER BY failures DESC, last_seen_at DESC
                LIMIT 20
                """;
        List<DeployOperationalAlertResponse> alerts = new ArrayList<>();
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Timestamp lastSeenAt = resultSet.getTimestamp("last_seen_at");
                String endpoint = resultSet.getString("endpoint");
                alerts.add(new DeployOperationalAlertResponse(
                        "CRITICAL",
                        "HTTP_5XX",
                        resultSet.getString("funnel_stage"),
                        endpoint,
                        "Erro 500 recente pode tornar a métrica de abandono desta etapa inválida.",
                        resultSet.getString("http_method") + " " + endpoint + " -> "
                                + resultSet.getInt("http_status") + " / " + resultSet.getString("error_class"),
                        resultSet.getLong("failures"),
                        lastSeenAt == null ? null : lastSeenAt.toInstant().toString(),
                        "Corrigir a causa técnica antes de interpretar ou escalar mídia desta etapa."));
            }
        } catch (SQLException ex) {
            org.slf4j.LoggerFactory.getLogger(PdeOperationalHealthService.class)
                    .error("Falha ao consolidar alertas operacionais PDE", ex);
        }
        return alerts;
    }

    /** Normaliza endpoint removendo identificadores variáveis para agregar falhas equivalentes. */
    private String normalizeEndpoint(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return "unknown";
        }
        if (requestUri.startsWith("/api/pde/public/presence-diagnostic/")) {
            return "/api/pde/public/presence-diagnostic/{requestId}";
        }
        if (requestUri.matches("^/api/pde/access/[^/]+/ai-guidance/[^/]+$")) {
            return "/api/pde/access/{token}/ai-guidance/{requestId}";
        }
        if (requestUri.matches("^/api/pde/access/[^/]+/missions/[^/]+/ai-guidance$")) {
            return "/api/pde/access/{token}/missions/{missionId}/ai-guidance";
        }
        return requestUri;
    }

    /** Mapeia endpoint técnico para etapa comercial do funil PDE. */
    private String resolveFunnelStage(String endpoint) {
        if (endpoint.startsWith("/api/pde/public/presence-diagnostic")) {
            return "DIAGNOSTICO_PUBLICO_IA";
        }
        if (endpoint.contains("/ai-guidance")) {
            return "ORIENTACAO_IA_AREA_MUSA";
        }
        if (endpoint.equals("/api/pde/access/magic-link") || endpoint.equals("/api/pde/access/login-link")) {
            return "LOGIN_EMAIL";
        }
        if (endpoint.equals("/api/pde/access/events")) {
            return "TRACKING_FUNIL";
        }
        return "PDE_BACKEND";
    }

    /** Verifica se uma tabela existe no schema atual. */
    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
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

    /** Verifica se todas as colunas exigidas existem no schema real. */
    private boolean columnsExist(Connection connection, String tableName, List<String> columnNames) throws SQLException {
        for (String columnName : columnNames) {
            if (!columnExists(connection, tableName, columnName)) {
                return false;
            }
        }
        return true;
    }

    /** Verifica se uma coluna existe na tabela informada. */
    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong(1) > 0;
            }
        }
    }

    /** Informa se existe banco JDBC configurado para saúde operacional. */
    private boolean usesJdbcStorage() {
        return jdbcUrl != null && !jdbcUrl.isBlank();
    }

    /** Abre conexão JDBC com o banco PDE. */
    private Connection openConnection() throws SQLException {
        return connectionProvider.open(jdbcUrl, jdbcUsername, jdbcPassword);
    }

    /** Retorna valor padrão quando a entrada é nula ou vazia. */
    private String nullToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** Limita textos técnicos antes da persistência operacional. */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /** Abre uma conexão JDBC para permitir substituição controlada em testes. */
    interface JdbcConnectionProvider {

        /** Abre uma conexão JDBC com as credenciais informadas. */
        Connection open(String url, String username, String password) throws SQLException;
    }
}
