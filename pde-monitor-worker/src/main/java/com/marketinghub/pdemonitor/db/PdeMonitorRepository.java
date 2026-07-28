package com.marketinghub.pdemonitor.db;

import com.marketinghub.pdemonitor.health.PdeHealthResult;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Acessa diretamente as tabelas operacionais de monitoramento dos PDEs críticos. */
@Repository
public class PdeMonitorRepository {
    private final JdbcTemplate jdbcTemplate;

    /** Recebe o cliente JDBC usado para leitura e gravação direta no MySQL. */
    public PdeMonitorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Lista PDEs críticos habilitados para monitoramento 24/7. */
    public List<PdeMonitoredModule> findCriticalPdes() {
        return jdbcTemplate.query(
                """
                SELECT id, code, name, base_url, health_path, monitoring_url, offline_threshold_seconds
                FROM ops_monitored_module
                WHERE enabled = 1
                  AND type = 'PDE'
                  AND criticality = 'CRITICAL'
                ORDER BY code
                """,
                (rs, rowNum) ->
                        new PdeMonitoredModule(
                                rs.getLong("id"),
                                rs.getString("code"),
                                rs.getString("name"),
                                rs.getString("base_url"),
                                rs.getString("health_path"),
                                rs.getString("monitoring_url"),
                                rs.getInt("offline_threshold_seconds")));
    }

    /** Grava o resultado bruto da verificação diretamente no histórico operacional. */
    public void insertHealthCheck(PdeMonitoredModule module, PdeHealthResult result) {
        jdbcTemplate.update(
                """
                INSERT INTO ops_module_health_check
                  (module_id, checked_at, status, http_status, response_time_ms, error_message, raw_payload)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                module.id(),
                Timestamp.from(result.checkedAt()),
                result.status(),
                result.httpStatus(),
                result.responseTimeMs(),
                result.errorMessage(),
                result.rawPayload());
    }

    /** Busca o incidente aberto mais recente do PDE, quando existir. */
    public Optional<Long> findOpenIncidentId(long moduleId) {
        List<Long> ids =
                jdbcTemplate.query(
                        """
                        SELECT id
                        FROM ops_module_incident
                        WHERE module_id = ?
                          AND status = 'OPEN'
                        ORDER BY started_at DESC
                        LIMIT 1
                        """,
                        (rs, rowNum) -> rs.getLong("id"),
                        moduleId);
        return ids.stream().findFirst();
    }

    /** Abre um incidente crítico direto no banco para um PDE indisponível ou degradado. */
    public void openIncident(PdeMonitoredModule module, PdeHealthResult result, String severity) {
        jdbcTemplate.update(
                """
                INSERT INTO ops_module_incident
                  (module_id, status, severity, started_at, summary, root_signal, last_error)
                VALUES (?, 'OPEN', ?, ?, ?, ?, ?)
                """,
                module.id(),
                severity,
                Timestamp.from(result.checkedAt()),
                summary(module, result),
                rootSignal(result),
                result.errorMessage());
    }

    /** Encerra o incidente aberto quando o PDE volta a responder normalmente. */
    public void closeIncident(long incidentId, Instant endedAt, Instant startedAt) {
        jdbcTemplate.update(
                """
                UPDATE ops_module_incident
                SET status = 'RESOLVED',
                    ended_at = ?,
                    duration_seconds = ?,
                    updated_at = ?
                WHERE id = ?
                  AND status = 'OPEN'
                """,
                Timestamp.from(endedAt),
                Duration.between(startedAt, endedAt).toSeconds(),
                Timestamp.from(endedAt),
                incidentId);
    }

    /** Busca a data de abertura do incidente para calcular sua duração. */
    public Optional<Instant> findIncidentStartedAt(long incidentId) {
        List<Instant> dates =
                jdbcTemplate.query(
                        "SELECT started_at FROM ops_module_incident WHERE id = ?",
                        (rs, rowNum) -> rs.getTimestamp("started_at").toInstant(),
                        incidentId);
        return dates.stream().findFirst();
    }

    /** Monta um resumo operacional curto para o incidente. */
    private String summary(PdeMonitoredModule module, PdeHealthResult result) {
        return "PDE crítico " + module.code() + " está " + result.status();
    }

    /** Resolve o sinal raiz salvo para orientar a correção operacional. */
    private String rootSignal(PdeHealthResult result) {
        if ("DEGRADED".equals(result.status())) {
            return "PDE_HTTP_DEGRADED";
        }
        return "PDE_HTTP_OFFLINE";
    }
}
