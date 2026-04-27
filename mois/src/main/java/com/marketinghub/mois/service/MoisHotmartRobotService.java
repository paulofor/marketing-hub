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
import org.springframework.stereotype.Service;

@Service
public class MoisHotmartRobotService {

    private static final String SOURCE_HOTMART = "HOTMART";
    private static final int MAX_HISTORY_SIZE = 300;

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

        try {
            MoisWorkspaceDtos.CollectionJobResponse job = domainService.createCollectionJob(new MoisWorkspaceDtos.CreateCollectionJobRequest(
                    properties.getWorkspaceId(),
                    properties.getNiche(),
                    properties.getMarketTheme(),
                    resolveSources(),
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
