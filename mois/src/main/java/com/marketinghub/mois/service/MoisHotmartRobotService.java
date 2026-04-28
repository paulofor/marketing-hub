package com.marketinghub.mois.service;

import com.marketinghub.mois.dto.MoisAutomationDtos;
import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MoisHotmartRobotService {

    private static final String SOURCE_HOTMART = "HOTMART";
    private static final int MAX_HISTORY_SIZE = 300;
    private static final Logger log = LoggerFactory.getLogger(MoisHotmartRobotService.class);

    private final MoisDomainService domainService;
    private final MoisHotmartRobotProperties properties;
    private final Deque<MoisAutomationDtos.HotmartRobotRunResponse> history = new ConcurrentLinkedDeque<>();

    public MoisHotmartRobotService(MoisDomainService domainService, MoisHotmartRobotProperties properties) {
        this.domainService = domainService;
        this.properties = properties;
    }

    public MoisAutomationDtos.HotmartRobotRunResponse triggerManualRun() {
        return trigger("MANUAL");
    }

    public MoisAutomationDtos.HotmartRobotRunResponse triggerScheduledRun() {
        return trigger("SCHEDULER");
    }

    public MoisAutomationDtos.HotmartRobotRunListResponse listRuns(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        List<MoisAutomationDtos.HotmartRobotRunResponse> items = history.stream()
                .limit(safeLimit)
                .toList();
        return new MoisAutomationDtos.HotmartRobotRunListResponse(new ArrayList<>(items));
    }

    private MoisAutomationDtos.HotmartRobotRunResponse trigger(String triggerType) {
        String runId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        List<String> sources = resolveSources();
        log.info("MOIS Hotmart run iniciado (runId={}, triggerType={}, workspaceId={}, niche={}, marketTheme={}, sources={}, timeWindow={}, minSuccessScore={}, limitPerSource={})",
                runId,
                triggerType,
                properties.getWorkspaceId(),
                properties.getNiche(),
                properties.getMarketTheme(),
                sources,
                properties.getTimeWindow(),
                properties.getMinSuccessScore(),
                properties.getLimitPerSource());

        try {
            MoisWorkspaceDtos.CollectionJobResponse job = domainService.createCollectionJob(new MoisWorkspaceDtos.CreateCollectionJobRequest(
                    properties.getWorkspaceId(),
                    properties.getNiche(),
                    properties.getMarketTheme(),
                    sources,
                    properties.getTimeWindow(),
                    properties.getLimitPerSource(),
                    properties.getLocale(),
                    properties.getCountry(),
                    properties.getMinSuccessScore()
            ));
            MoisAutomationDtos.HotmartRobotRunResponse response = new MoisAutomationDtos.HotmartRobotRunResponse(
                    runId,
                    "SUCCESS",
                    triggerType,
                    properties.getWorkspaceId(),
                    properties.getNiche(),
                    properties.getMarketTheme(),
                    job.jobId(),
                    properties.getMinSuccessScore(),
                    properties.getLimitPerSource(),
                    now,
                    null
            );
            remember(response);
            log.info("MOIS Hotmart run finalizado com sucesso (runId={}, triggerType={}, jobId={})",
                    runId, triggerType, job.jobId());
            return response;
        } catch (RuntimeException ex) {
            MoisAutomationDtos.HotmartRobotRunResponse response = new MoisAutomationDtos.HotmartRobotRunResponse(
                    runId,
                    "FAILED",
                    triggerType,
                    properties.getWorkspaceId(),
                    properties.getNiche(),
                    properties.getMarketTheme(),
                    null,
                    properties.getMinSuccessScore(),
                    properties.getLimitPerSource(),
                    now,
                    ex.getMessage()
            );
            remember(response);
            log.error("MOIS Hotmart run falhou (runId={}, triggerType={})", runId, triggerType, ex);
            throw ex;
        }
    }

    private void remember(MoisAutomationDtos.HotmartRobotRunResponse run) {
        history.addFirst(run);
        while (history.size() > MAX_HISTORY_SIZE) {
            history.removeLast();
        }
    }

    private List<String> resolveSources() {
        if (properties.getSources() == null || properties.getSources().isEmpty()) {
            return List.of(SOURCE_HOTMART);
        }
        return properties.getSources().stream()
                .filter(Objects::nonNull)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
