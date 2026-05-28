package com.marketinghub.worker.geralanding.copy.monitor;

import com.marketinghub.worker.geralanding.copy.GeraLandingExecutionService;
import com.marketinghub.worker.geralanding.copy.dto.GeraLandingStageExecutionDetailDto;
import java.util.List;
import org.springframework.stereotype.Service;

/** Processa os jobs pendentes da etapa copy usando o executor compartilhado da etapa copy. */
@Service
public class CopyExecutionProcessor {
    private final GeraLandingExecutionService executionService;

    public CopyExecutionProcessor(GeraLandingExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa a lista de jobs pendentes retornada pelo polling da etapa copy. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) {
        executionService.processExecutions(jobs);
    }
}
