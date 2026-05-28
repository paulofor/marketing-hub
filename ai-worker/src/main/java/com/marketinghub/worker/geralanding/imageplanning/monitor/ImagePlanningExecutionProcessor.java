package com.marketinghub.worker.geralanding.imageplanning.monitor;

import com.marketinghub.worker.geralanding.imageplanning.dto.GeraLandingStageExecutionDetailDto;
import com.marketinghub.worker.geralanding.imageplanning.request.GeraLandingImagePlanningOpenAiExecutionService;
import java.util.List;
import org.springframework.stereotype.Service;

/** Processa os jobs pendentes da etapa image-planning com o executor OpenAI da própria etapa. */
@Service
public class ImagePlanningExecutionProcessor {
    private final GeraLandingImagePlanningOpenAiExecutionService executionService;

    public ImagePlanningExecutionProcessor(GeraLandingImagePlanningOpenAiExecutionService executionService) {
        this.executionService = executionService;
    }

    /** Processa a lista de jobs pendentes retornada pelo polling da etapa image-planning. */
    public void processExecutions(List<GeraLandingStageExecutionDetailDto> jobs) {
        executionService.processExecutions(jobs);
    }
}
