package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class MoisHotmartCollectionScheduler {

    private final MoisModuleGateway gateway;
    private final MoisHotmartCollectionProperties properties;

    public MoisHotmartCollectionScheduler(MoisModuleGateway gateway, MoisHotmartCollectionProperties properties) {
        this.gateway = gateway;
        this.properties = properties;
    }

    @Scheduled(cron = "0 0 22 * * *")
    public void scheduleCollection() {
        log.info("Iniciando execução do scheduler MOIS Hotmart (workspaceId={}, enabled={})",
                properties.getWorkspaceId(), properties.isEnabled());

        if (!properties.isEnabled()) {
            return;
        }

        List<String> sanitizedSources = properties.getSources().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();

        if (sanitizedSources.isEmpty()) {
            log.warn("MOIS Hotmart scheduler ignorado: sources vazio (workspaceId={})", properties.getWorkspaceId());
            return;
        }

        MoisWorkspaceDtos.CreateCollectionJobRequest request = new MoisWorkspaceDtos.CreateCollectionJobRequest(
                properties.getWorkspaceId(),
                properties.getNiche(),
                properties.getMarketTheme(),
                sanitizedSources,
                properties.getTimeWindow(),
                properties.getLimitPerSource(),
                properties.getLocale(),
                properties.getCountry(),
                properties.getMinSuccessScore()
        );

        try {
            MoisWorkspaceDtos.CollectionJobResponse response = gateway.createCollectionJob(request);
            if (response != null) {
                log.info("MOIS Hotmart scheduler criou job {} (workspaceId={}, sources={})",
                        response.jobId(), response.workspaceId(), response.sources());
            }
        } catch (Exception ex) {
            log.error("Falha ao criar job automático MOIS Hotmart (workspaceId={})", properties.getWorkspaceId(), ex);
        }
    }
}
