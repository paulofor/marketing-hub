package com.marketinghub.mcpserver.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Responsabilidade: executar diagnósticos e consultas somente leitura em um datasource MySQL.
 */
@Service
public class DatabaseDiagnosticsService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Inicializa o serviço com o datasource que será consultado pelas ferramentas MCP.
     */
    public DatabaseDiagnosticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Verifica se o datasource responde e informa o schema ativo.
     */
    public Map<String, Object> checkConnection() {
        Map<String, Object> response = new LinkedHashMap<>();
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        response.put("status", one != null && one == 1 ? "ok" : "unexpected");
        response.put("database", databaseName);
        return response;
    }

    /**
     * Lista as tabelas base disponíveis no schema ativo.
     */
    public Map<String, Object> listTables() {
        String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        List<String> tables = jdbcTemplate.queryForList(
                """
                        SELECT TABLE_NAME
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE UPPER(TABLE_SCHEMA) = UPPER(SCHEMA())
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

    /**
     * Lê linhas paginadas de uma tabela validada do schema ativo.
     */
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

    /**
     * Executa uma query de leitura com limite máximo aplicado quando necessário.
     */
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

    /**
     * Consulta a telemetria persistida de uma execução Codex sem acessar logs do worker.
     */
    public Map<String, Object> codexAgentTelemetry(String agentType, long executionId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT agent_type, execution_id, status, process_id, process_alive,
                               event_count, output_bytes, input_tokens, output_tokens,
                               last_event_type, last_activity_at, started_at, finished_at,
                               CASE WHEN status = 'RUNNING'
                                     AND last_activity_at < DATE_SUB(UTC_TIMESTAMP(), INTERVAL 2 MINUTE)
                                    THEN 1 ELSE 0 END AS stale
                        FROM codex_agent_execution_telemetry
                        WHERE agent_type = ? AND execution_id = ?
                        """,
                agentType,
                executionId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("agentType", agentType);
        response.put("executionId", executionId);
        response.put("found", !rows.isEmpty());
        response.put("telemetry", rows.isEmpty() ? null : rows.get(0));
        return response;
    }

    /**
     * Consolida o parecer canônico de Têmis com a atividade Codex e a memória governada usada para
     * acelerar consenso sem transformar lembranças candidatas em aprovação.
     */
    public Map<String, Object> metaAdApproverTelemetry(long creativeId) {
        List<Map<String, Object>> reviews = jdbcTemplate.queryForList(
                """
                        SELECT c.id AS creative_id, c.experiment_id, c.agent_review_status,
                               c.agent_review_started_at, c.agent_reviewed_at,
                               c.agent_review_recovery_count, c.agent_review_response_json,
                               c.agent_review_model
                        FROM creative c
                        WHERE c.id = ?
                        """,
                creativeId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("creativeId", creativeId);
        response.put("found", !reviews.isEmpty());
        if (reviews.isEmpty()) {
            response.put("review", null);
            response.put("telemetry", null);
            response.put("memory", Map.of("confirmed", 0, "candidates", 0));
            return response;
        }

        Map<String, Object> review = reviews.getFirst();
        long experimentId = ((Number) review.get("EXPERIMENT_ID")).longValue();
        Map<String, Object> telemetry = codexAgentTelemetry("META_AD_APPROVER", creativeId);
        List<Map<String, Object>> memoryRows = jdbcTemplate.queryForList(
                """
                        SELECT status, COUNT(*) AS total, COALESCE(SUM(retrieval_count), 0) AS retrievals
                        FROM premium_agent_memory
                        WHERE agent_key = 'meta-ad-approver'
                          AND scope_type = 'EXPERIMENT'
                          AND scope_id = ?
                          AND (valid_until IS NULL OR valid_until > UTC_TIMESTAMP())
                          AND status <> 'REJECTED'
                        GROUP BY status
                        """,
                String.valueOf(experimentId));
        int confirmed = memoryCount(memoryRows, "CONFIRMED");
        int candidates = memoryCount(memoryRows, "CANDIDATE");
        boolean processing = "PROCESSING".equals(String.valueOf(review.get("AGENT_REVIEW_STATUS")));
        Object telemetryValue = telemetry.get("telemetry");
        boolean stale = telemetryValue instanceof Map<?, ?> row
                && number(row.get("STALE")) != null
                && number(row.get("STALE")).intValue() == 1;

        response.put("experimentId", experimentId);
        response.put("review", review);
        response.put("telemetry", telemetryValue);
        response.put("blocked", processing && (telemetryValue == null || stale));
        response.put("memory", Map.of(
                "confirmed", confirmed,
                "candidates", candidates,
                "policy", "Candidatas orientam investigação; somente feedback independente confirma memória."));
        return response;
    }

    /**
     * Lista os experimentos de Apolo com memória e decisão, sem consultar logs ou recomputar notas.
     */
    public Map<String, Object> apolloLearningExperiments() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        SELECT e.id, e.scope_type, e.scope_id, e.baseline_version,
                               e.candidate_version, e.status, e.baseline_result_json,
                               e.candidate_result_json, e.decision_evidence,
                               e.regression_passed, e.local_validation_passed,
                               e.created_at, e.evaluated_at, e.promoted_at,
                               e.memory_id, m.status AS memory_status,
                               m.specialty AS memory_specialty, m.content AS memory_content
                        FROM governed_agent_learning_experiment e
                        JOIN premium_agent_memory m ON m.id = e.memory_id
                        WHERE e.agent_key = 'apollo'
                        ORDER BY e.id DESC
                        LIMIT 20
                        """);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("agentKey", "apollo");
        response.put("count", rows.size());
        response.put("experiments", rows);
        response.put("policy", "Memória candidata não autoriza provider, gasto, publicação ou promoção automática.");
        return response;
    }

    /** Soma memórias de um estado sem depender da capitalização do driver JDBC. */
    private int memoryCount(List<Map<String, Object>> rows, String status) {
        return rows.stream()
                .filter(row -> status.equalsIgnoreCase(String.valueOf(row.get("STATUS"))))
                .map(row -> number(row.get("TOTAL")))
                .filter(java.util.Objects::nonNull)
                .mapToInt(Number::intValue)
                .sum();
    }

    /** Converte um valor numérico retornado pelo driver. */
    private Number number(Object value) {
        return value instanceof Number number ? number : null;
    }

    /**
     * Valida o nome de tabela para impedir injeção em leitura dinâmica.
     */
    private void validateTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("table is required");
        }

        if (!tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("table contains invalid characters");
        }
    }

    /**
     * Normaliza e bloqueia qualquer SQL que não seja uma única consulta SELECT/WITH.
     */
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

    /**
     * Adiciona LIMIT quando a consulta não define limite próprio.
     */
    private String ensureLimit(String sql, int limit) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (lower.contains(" limit ")) {
            return sql;
        }
        return sql + " LIMIT " + limit;
    }
}
