package com.marketinghub.experiment.frameworkimage.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobDto;
import com.marketinghub.experiment.pipeline.service.ExperimentPipelineGenerationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FrameworkImagePipelineResumeSchedulerTest {

    @Mock
    private FrameworkImageGenerationService frameworkImageGenerationService;

    @Mock
    private ExperimentPipelineGenerationService experimentPipelineGenerationService;

    @Test
    void runResumesFlowForDistinctExperimentIds() {
        FrameworkImagePipelineResumeScheduler scheduler = new FrameworkImagePipelineResumeScheduler(
                frameworkImageGenerationService,
                experimentPipelineGenerationService,
                true,
                50);

        when(frameworkImageGenerationService.listPendingJobs(50)).thenReturn(List.of(
                job(11L),
                job(11L),
                job(12L)));

        scheduler.run();

        verify(experimentPipelineGenerationService, times(1)).resumeFlowAfterImagePlanningIfReady(11L);
        verify(experimentPipelineGenerationService, times(1)).resumeFlowAfterImagePlanningIfReady(12L);
    }

    @Test
    void runDoesNothingWhenDisabled() {
        FrameworkImagePipelineResumeScheduler scheduler = new FrameworkImagePipelineResumeScheduler(
                frameworkImageGenerationService,
                experimentPipelineGenerationService,
                false,
                50);

        scheduler.run();

        verify(frameworkImageGenerationService, never()).listPendingJobs(50);
        verify(experimentPipelineGenerationService, never()).resumeFlowAfterImagePlanningIfReady(11L);
    }

    private FrameworkImageGenerationJobDto job(Long experimentId) {
        return FrameworkImageGenerationJobDto.builder()
                .id(UUID.randomUUID())
                .experimentId(experimentId)
                .planningItemKey("key")
                .status("PENDING")
                .build();
    }
}
