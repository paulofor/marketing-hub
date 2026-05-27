package com.marketinghub.worker.geralanding.comum;

import com.marketinghub.worker.geralanding.GeraLandingExecutionService;
import com.marketinghub.worker.geralanding.GeraLandingStageExecutionDto;
import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa preset design usando o executor compartilhado. */
@Service
public class GeraLandingPresetDesignExecutionService {
    private final GeraLandingExecutionService executionService;

    public GeraLandingPresetDesignExecutionService(GeraLandingExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa os jobs pendentes da etapa preset design. */
    public void processExecutions(List<GeraLandingStageExecutionDto> jobs) {
        executionService.processExecutions(jobs);
    }
}
