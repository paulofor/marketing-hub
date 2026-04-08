package com.marketinghub.experiment.frameworkimage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJob;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStage;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStatus;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobCompletionRequest;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobDto;
import com.marketinghub.experiment.frameworkimage.repository.FrameworkImageGenerationJobRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FrameworkImageGenerationServiceTest {

    @Mock
    private FrameworkImageGenerationJobRepository jobRepository;

    @Mock
    private ExperimentRepository experimentRepository;

    @Captor
    private ArgumentCaptor<FrameworkImageGenerationJob> jobCaptor;

    private FrameworkImageGenerationService service;

    @BeforeEach
    void setUp() {
        service = new FrameworkImageGenerationService(jobRepository, experimentRepository);
    }

    @Test
    void enqueueJobReturnsExistingActiveJobForSamePlanningItem() {
        FrameworkImageGenerationJob existing = FrameworkImageGenerationJob.builder()
                .id(UUID.randomUUID())
                .experiment(Experiment.builder().id(10L).build())
                .planningItemKey("hero-1")
                .status(FrameworkImageGenerationJobStatus.PENDING)
                .stage(FrameworkImageGenerationJobStage.WAITING_AI_WORKER)
                .build();

        when(jobRepository.findFirstByExperimentIdAndPlanningItemKeyAndStatusInOrderByCreatedAtDesc(
                eq(10L), eq("hero-1"), any(Set.class))).thenReturn(Optional.of(existing));

        FrameworkImageGenerationJobDto dto = service.enqueueJob(10L, "hero-1", "gpt-image-1", "prompt");

        assertThat(dto.id()).isEqualTo(existing.getId());
        verify(jobRepository, never()).save(any(FrameworkImageGenerationJob.class));
    }

    @Test
    void enqueueJobCreatesPendingJobWhenNoActiveJobExists() {
        Experiment experiment = Experiment.builder().id(15L).build();

        when(jobRepository.findFirstByExperimentIdAndPlanningItemKeyAndStatusInOrderByCreatedAtDesc(
                eq(15L), eq("hero-1"), any(Set.class))).thenReturn(Optional.empty());
        when(experimentRepository.findById(15L)).thenReturn(Optional.of(experiment));
        when(jobRepository.save(any(FrameworkImageGenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FrameworkImageGenerationJobDto dto = service.enqueueJob(15L, " hero-1 ", " gpt-image-1 ", " prompt ");

        verify(jobRepository).save(jobCaptor.capture());
        FrameworkImageGenerationJob saved = jobCaptor.getValue();
        assertThat(saved.getExperiment()).isEqualTo(experiment);
        assertThat(saved.getPlanningItemKey()).isEqualTo("hero-1");
        assertThat(saved.getStatus()).isEqualTo(FrameworkImageGenerationJobStatus.PENDING);
        assertThat(saved.getStage()).isEqualTo(FrameworkImageGenerationJobStage.WAITING_AI_WORKER);
        assertThat(saved.getModel()).isEqualTo("gpt-image-1");
        assertThat(saved.getPrompt()).isEqualTo("prompt");
        assertThat(dto.planningItemKey()).isEqualTo("hero-1");
    }

    @Test
    void completeJobFinalizesAndStoresMetadata() {
        UUID jobId = UUID.randomUUID();
        FrameworkImageGenerationJob job = FrameworkImageGenerationJob.builder()
                .id(jobId)
                .status(FrameworkImageGenerationJobStatus.PROCESSING)
                .stage(FrameworkImageGenerationJobStage.WAITING_OPENAI_BATCH)
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        service.completeJob(jobId, new FrameworkImageGenerationJobCompletionRequest(
                FrameworkImageGenerationJobStage.UPLOADED_TO_CLOUDFLARE,
                "gpt-image-1",
                "prompt final",
                "batch_1",
                99L,
                "https://cdn/source.jpg",
                "https://cdn/web.jpg"));

        assertThat(job.getStatus()).isEqualTo(FrameworkImageGenerationJobStatus.COMPLETED);
        assertThat(job.getStage()).isEqualTo(FrameworkImageGenerationJobStage.UPLOADED_TO_CLOUDFLARE);
        assertThat(job.getAssetId()).isEqualTo(99L);
        assertThat(job.getFinishedAt()).isNotNull();
        assertThat(job.getErrorMessage()).isNull();
    }

    @Test
    void claimJobRejectsNonPendingJob() {
        UUID jobId = UUID.randomUUID();
        FrameworkImageGenerationJob job = FrameworkImageGenerationJob.builder()
                .id(jobId)
                .status(FrameworkImageGenerationJobStatus.PROCESSING)
                .build();
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.claimJob(jobId, "worker-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void listPendingJobsClampsLimit() {
        FrameworkImageGenerationJob pending = FrameworkImageGenerationJob.builder()
                .id(UUID.randomUUID())
                .experiment(Experiment.builder().id(10L).build())
                .planningItemKey("hero-1")
                .status(FrameworkImageGenerationJobStatus.PENDING)
                .stage(FrameworkImageGenerationJobStage.WAITING_AI_WORKER)
                .build();
        when(jobRepository.findByStatusOrderByCreatedAtAsc(eq(FrameworkImageGenerationJobStatus.PENDING), any()))
                .thenReturn(List.of(pending));

        List<FrameworkImageGenerationJobDto> jobs = service.listPendingJobs(0);

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).planningItemKey()).isEqualTo("hero-1");
    }
}
