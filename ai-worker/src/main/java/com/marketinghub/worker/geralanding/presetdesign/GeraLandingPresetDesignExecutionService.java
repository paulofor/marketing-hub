package com.marketinghub.worker.geralanding.presetdesign;

import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa preset design usando o executor compartilhado. */
@Service
public class GeraLandingPresetDesignExecutionService {
    private final com.marketinghub.worker.geralanding.comum.GeraLandingExecutionService executionService;

    public GeraLandingPresetDesignExecutionService(com.marketinghub.worker.geralanding.comum.GeraLandingExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa os jobs pendentes da etapa preset design. */
    public void processExecutions(List<GeraLandingStageExecutionPresetDesignDto> jobs) {
        executionService.processExecutions(jobs.stream()
                .map(item -> new com.marketinghub.worker.geralanding.comum.GeraLandingStageExecutionRef(item.experimentId(), item.idJob(), item.stageCode()))
                .toList());
    }
}
