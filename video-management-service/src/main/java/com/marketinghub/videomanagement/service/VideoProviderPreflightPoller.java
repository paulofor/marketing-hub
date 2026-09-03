package com.marketinghub.videomanagement.service;

import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import com.marketinghub.videomanagement.client.payload.ProviderPreflightResultPayload;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.provider.RunwayProviderPreflightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: executar periodicamente um preflight de vídeo sem iniciar geração paga. */
@Component
public class VideoProviderPreflightPoller {
    private static final Logger log = LoggerFactory.getLogger(VideoProviderPreflightPoller.class);
    private final VideoManagementProperties properties;
    private final BackendVideoClient backend;
    private final RunwayProviderPreflightService runway;
    @Autowired private AutomaticExecutionControl automaticExecution;

    /** Configura fila interna, integração Runway e controle operacional local. */
    public VideoProviderPreflightPoller(
            VideoManagementProperties properties,
            BackendVideoClient backend,
            RunwayProviderPreflightService runway) {
        this.properties = properties;
        this.backend = backend;
        this.runway = runway;
    }

    /** Processa no máximo um preflight por ciclo de polling para limitar carga e auditoria. */
    @Scheduled(initialDelay = 4000,
            fixedDelayString = "#{@videoManagementProperties.jobs.pollInterval.toMillis()}")
    public void pollPreflight() {
        if (automaticExecution != null && !automaticExecution.allowsAutomaticExecution()) return;
        if (!properties.getJobs().isPollingEnabled()) return;
        ProviderPreflightJob job = null;
        try {
            job = backend.fetchPendingProviderPreflight();
            if (job == null) return;
            ProviderPreflightResultPayload result = runway.execute(job);
            backend.reportProviderPreflight(job.cycleId(), result);
        } catch (Exception ex) {
            log.error("Falha no polling de preflight de vídeo; cycleId={}", job == null ? null : job.cycleId(), ex);
        }
    }
}
