package com.marketinghub.videomanagement.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import com.marketinghub.videomanagement.client.payload.ProviderPreflightResultPayload;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.RunwayProviderPreflightService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar que o executor reporta uma pendência por polling sem orquestrar etapas. */
@ExtendWith(MockitoExtension.class)
class VideoProviderPreflightPollerTest {
    @Mock private BackendVideoClient backend;
    @Mock private RunwayProviderPreflightService runway;

    /** Executa e reporta exatamente um preflight quando o polling estiver habilitado. */
    @Test
    void shouldProcessOnePendingPreflight() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getJobs().setPollingEnabled(true);
        ProviderPreflightJob job = job();
        ProviderPreflightResultPayload result = result();
        when(backend.fetchPendingProviderPreflight()).thenReturn(job);
        when(runway.execute(job)).thenReturn(result);

        new VideoProviderPreflightPoller(properties, backend, runway).pollPreflight();

        verify(runway).execute(job);
        verify(backend).reportProviderPreflight(11L, result);
    }

    /** Não consulta fila nem provedor quando a rotina local estiver pausada. */
    @Test
    void shouldStayIdleWhenPollingIsDisabled() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getJobs().setPollingEnabled(false);

        new VideoProviderPreflightPoller(properties, backend, runway).pollPreflight();

        verify(backend, never()).fetchPendingProviderPreflight();
        verify(runway, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    /** Cria a pendência mínima usada pelo executor de teste. */
    private ProviderPreflightJob job() {
        return new ProviderPreflightJob(
                31L, 11L, "Runway", "RUNWAY_PRIMARY", "FINAL_CAMPAIGN",
                new BigDecimal("100"), 10, 10, 1, "9:16", "720p", false,
                "Vega", null, null, null, null, null, null, null, null, null, null);
    }

    /** Cria o retorno mínimo já produzido pelo adapter simulado. */
    private ProviderPreflightResultPayload result() {
        return new ProviderPreflightResultPayload(
                "BLOCKED", "RUNWAY_PRIMARY", null, null, null, null, null, null,
                null, null, null, null, null, "PROVIDER_AUTH_ERROR", "Sem credencial",
                "https://api.dev.runwayml.com/v1/organization", Instant.now());
    }
}
