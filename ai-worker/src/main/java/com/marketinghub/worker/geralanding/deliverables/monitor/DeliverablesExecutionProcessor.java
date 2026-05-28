package com.marketinghub.worker.geralanding.deliverables.monitor;

import com.marketinghub.worker.geralanding.deliverables.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.deliverables.request.GeraLandingDeliverablesOpenAiExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Processa os jobs pendentes da etapa deliverables com o executor OpenAI da própria etapa. */
@Service
public class DeliverablesExecutionProcessor {
    private final GeraLandingDeliverablesOpenAiExecutionService executionService;

    public DeliverablesExecutionProcessor(GeraLandingDeliverablesOpenAiExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa a lista de jobs pendentes retornada pelo polling da etapa deliverables. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) {
        executionService.processExecutions(jobs);
    }
}
