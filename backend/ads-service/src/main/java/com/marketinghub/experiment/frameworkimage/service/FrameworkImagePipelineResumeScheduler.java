package com.marketinghub.experiment.frameworkimage.service;

import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobDto;
import com.marketinghub.experiment.pipeline.service.ExperimentPipelineGenerationService;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FrameworkImagePipelineResumeScheduler {

    private final FrameworkImageGenerationService frameworkImageGenerationService;
    private final ExperimentPipelineGenerationService experimentPipelineGenerationService;
    private final boolean enabled;
    private final int batchSize;

    public FrameworkImagePipelineResumeScheduler(FrameworkImageGenerationService frameworkImageGenerationService,
                                                 ExperimentPipelineGenerationService experimentPipelineGenerationService,
                                                 @Value("${framework-image.pipeline-resume.enabled:true}") boolean enabled,
                                                 @Value("${framework-image.pipeline-resume.batch-size:50}") int batchSize) {
        this.frameworkImageGenerationService = frameworkImageGenerationService;
        this.experimentPipelineGenerationService = experimentPipelineGenerationService;
        this.enabled = enabled;
        this.batchSize = Math.max(1, batchSize);
    }

    @Scheduled(fixedDelayString = "${framework-image.pipeline-resume.fixed-delay-ms:60000}")
    public void run() {
        if (!enabled) {
            return;
        }

        Set<Long> experimentIds = new LinkedHashSet<>();
        for (FrameworkImageGenerationJobDto job : frameworkImageGenerationService.listPendingJobs(batchSize)) {
            if (job.experimentId() != null) {
                experimentIds.add(job.experimentId());
            }
        }

        for (Long experimentId : experimentIds) {
            experimentPipelineGenerationService.resumeFlowAfterImagePlanningIfReady(experimentId);
        }
    }
}
