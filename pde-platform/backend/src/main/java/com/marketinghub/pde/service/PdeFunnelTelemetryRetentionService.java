package com.marketinghub.pde.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Anonimiza o contexto detalhado da telemetria PDE sem apagar os fatos agregáveis do funil. */
final class PdeFunnelTelemetryRetentionService {
    private static final Logger log = LoggerFactory.getLogger(PdeFunnelTelemetryRetentionService.class);
    private static final long DETAILED_TELEMETRY_RETENTION_DAYS = 180L;

    private final String jdbcUrl;
    private final String jdbcUsername;
    private final String jdbcPassword;

    /** Recebe somente a conexão necessária para aplicar a política no banco canônico. */
    PdeFunnelTelemetryRetentionService(String jdbcUrl, String jdbcUsername, String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
    }

    /** Remove identificadores e contexto de eventos com mais de 180 dias de forma idempotente. */
    int anonymizeExpiredDetailedTelemetry(Instant reference) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return 0;
        }
        String sql = """
                UPDATE pde_funnel_event
                SET access_token = NULL,
                    email = NULL,
                    normalized_email = NULL,
                    page_url = NULL,
                    client_ip = NULL,
                    user_agent = NULL,
                    referrer_url = NULL,
                    session_id = NULL,
                    visitor_id = NULL,
                    utm_source = NULL,
                    utm_medium = NULL,
                    utm_campaign = NULL,
                    utm_content = NULL,
                    utm_term = NULL,
                    device_type = NULL,
                    screen_width = NULL,
                    screen_height = NULL,
                    viewport_width = NULL,
                    viewport_height = NULL,
                    visible_ms = NULL,
                    section_id = NULL,
                    action_name = NULL,
                    metadata_json = NULL
                WHERE occurred_at < ?
                  AND (access_token IS NOT NULL
                    OR email IS NOT NULL
                    OR normalized_email IS NOT NULL
                    OR page_url IS NOT NULL
                    OR client_ip IS NOT NULL
                    OR user_agent IS NOT NULL
                    OR referrer_url IS NOT NULL
                    OR session_id IS NOT NULL
                    OR visitor_id IS NOT NULL
                    OR utm_source IS NOT NULL
                    OR utm_medium IS NOT NULL
                    OR utm_campaign IS NOT NULL
                    OR utm_content IS NOT NULL
                    OR utm_term IS NOT NULL
                    OR device_type IS NOT NULL
                    OR screen_width IS NOT NULL
                    OR screen_height IS NOT NULL
                    OR viewport_width IS NOT NULL
                    OR viewport_height IS NOT NULL
                    OR visible_ms IS NOT NULL
                    OR section_id IS NOT NULL
                    OR action_name IS NOT NULL
                    OR metadata_json IS NOT NULL)
                """;
        Instant cutoff = reference.minusSeconds(DETAILED_TELEMETRY_RETENTION_DAYS * 24L * 60L * 60L);
        try (Connection connection = DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(cutoff));
            return statement.executeUpdate();
        } catch (SQLException ex) {
            log.error(
                    "Falha ao anonimizar telemetria PDE vencida; operation=telemetry-retention, cutoff={}",
                    cutoff,
                    ex);
            throw new IllegalStateException("Não foi possível aplicar a retenção da telemetria PDE", ex);
        }
    }
}
