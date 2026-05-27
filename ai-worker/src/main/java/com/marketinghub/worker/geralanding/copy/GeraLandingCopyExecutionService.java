package com.marketinghub.worker.geralanding.copy;

import com.marketinghub.worker.geralanding.copy.GeraLandingExecutionService;
import com.marketinghub.worker.geralanding.copy.GeraLandingStageExecutionDto;
import java.util.List;
import org.springframework.stereotype.Service;

/** Centraliza a execução de jobs da etapa copy usando o executor compartilhado. */
@Service
public class GeraLandingCopyExecutionService {
    private final GeraLandingExecutionService executionService;

    public GeraLandingCopyExecutionService(GeraLandingExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa os jobs pendentes da etapa copy. */
    public void processExecutions(List<GeraLandingStageExecutionDto> jobs) {
        executionService.processExecutions(jobs);
    }
}
