package com.marketinghub.mcpserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

/** Valida os diagnósticos consolidados expostos pelo MCP. */
class DatabaseDiagnosticsServiceTest {

    /** Consolida parecer, heartbeat vencido e memória sem promover candidatos. */
    @Test
    void shouldExposeBlockedMetaReviewWithGovernedMemory() {
        JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
        DatabaseDiagnosticsService service = new DatabaseDiagnosticsService(jdbc);
        Map<String, Object> review = new LinkedHashMap<>();
        review.put("EXPERIMENT_ID", 88L);
        review.put("AGENT_REVIEW_STATUS", "PROCESSING");
        Map<String, Object> telemetry = new LinkedHashMap<>();
        telemetry.put("STALE", 1L);

        when(jdbc.queryForList(anyString(), eq(330L))).thenReturn(List.of(review));
        when(jdbc.queryForList(anyString(), eq("META_AD_APPROVER"), eq(330L)))
                .thenReturn(List.of(telemetry));
        when(jdbc.queryForList(anyString(), eq("88")))
                .thenReturn(List.of(
                        Map.of("STATUS", "CONFIRMED", "TOTAL", 2L, "RETRIEVALS", 5L),
                        Map.of("STATUS", "CANDIDATE", "TOTAL", 3L, "RETRIEVALS", 4L)));

        Map<String, Object> result = service.metaAdApproverTelemetry(330L);

        assertThat(result.get("blocked")).isEqualTo(true);
        assertThat(result.get("experimentId")).isEqualTo(88L);
        Map<?, ?> memory = (Map<?, ?>) result.get("memory");
        assertThat(memory.get("confirmed")).isEqualTo(2);
        assertThat(memory.get("candidates")).isEqualTo(3);
    }
}
