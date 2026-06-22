package com.marketinghub.leadportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

/**
 * Valida as regras comerciais das métricas consolidadas do portal do lead.
 */
class LeadPortalMetricsServiceTest {

    /**
     * Garante que acessos técnicos da Meta não sejam contados como acesso humano no funil.
     */
    @Test
    void listExperimentMetricsFiltersMetaCrawlerAccessesFromCommercialFunnel() {
        JdbcTemplate jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
        List<String> executedSql = new ArrayList<>();
        doAnswer(invocation -> {
                    executedSql.add(invocation.getArgument(0));
                    return null;
                })
                .when(jdbcTemplate)
                .query(anyString(), any(RowCallbackHandler.class));

        LeadPortalMetricsService service = new LeadPortalMetricsService(jdbcTemplate);
        service.listExperimentMetrics();

        String accessSql = executedSql.getFirst();
        assertThat(accessSql)
                .contains("unique_accesses")
                .contains("technical_accesses_filtered")
                .contains("CASE")
                .contains("facebookexternalhit")
                .contains("meta-externalads")
                .contains("facebot");
    }
}
