package com.marketinghub.experiment.frameworkimage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJob;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStage;
import com.marketinghub.experiment.frameworkimage.FrameworkImageGenerationJobStatus;
import com.marketinghub.experiment.frameworkimage.dto.FrameworkImageGenerationItemStatusDto;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobCompletionRequest;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageGenerationJobDto;
import com.marketinghub.experiment.frameworkimage.dto.internal.FrameworkImageWebnizationPendingAssetDto;
import com.marketinghub.experiment.frameworkimage.repository.FrameworkImageGenerationJobRepository;
import com.marketinghub.experiment.repository.ExperimentRepository;
import java.time.Instant;
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
        service = new FrameworkImageGenerationService(jobRepository, experimentRepository, new ObjectMapper(), true, 100);
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
                .experiment(Experiment.builder().id(55L).build())
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

    @Test
    void enqueueJobsForExperimentMapsPlanningAndIgnoresItemsWithoutPrompt() {
        Experiment experiment = Experiment.builder()
                .id(22L)
                .landingPageImagePlanning("""
                        {
                          "images": [
                            {"sectionId":"hero","sectionName":"Hero","imagePrompt":"Prompt 1"},
                            {"sectionId":"benefit","sectionName":"Benefícios","imagePrompt":"   "},
                            {"sectionId":"offer","sectionName":"Oferta","prompt":"Prompt 3"}
                          ]
                        }
                        """)
                .build();
        when(experimentRepository.findById(22L)).thenReturn(Optional.of(experiment));
        when(jobRepository.findFirstByExperimentIdAndPlanningItemKeyAndStatusInOrderByCreatedAtDesc(eq(22L), any(), any(Set.class)))
                .thenReturn(Optional.empty());
        when(jobRepository.save(any(FrameworkImageGenerationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<FrameworkImageGenerationJobDto> jobs = service.enqueueJobsForExperiment(22L);

        assertThat(jobs).hasSize(2);
        assertThat(jobs).extracting(FrameworkImageGenerationJobDto::planningItemKey)
                .containsExactly("hero", "offer");
    }

    @Test
    void listJobsByExperimentReturnsPlannedAndJobStatuses() {
        Experiment experiment = Experiment.builder()
                .id(30L)
                .landingPageImagePlanning("""
                        {
                          "landingPageImagePlanning": {
                            "images": [
                              {"sectionId":"hero","sectionName":"Hero","imagePrompt":"Prompt Hero"},
                              {"sectionId":"faq","sectionName":"FAQ","imagePrompt":"Prompt FAQ"}
                            ]
                          }
                        }
                        """)
                .build();
        FrameworkImageGenerationJob heroJob = FrameworkImageGenerationJob.builder()
                .id(UUID.randomUUID())
                .experiment(experiment)
                .planningItemKey("hero")
                .status(FrameworkImageGenerationJobStatus.PROCESSING)
                .stage(FrameworkImageGenerationJobStage.CLAIMED)
                .build();

        when(experimentRepository.findById(30L)).thenReturn(Optional.of(experiment));
        when(jobRepository.findByExperimentIdOrderByCreatedAtDesc(30L)).thenReturn(List.of(heroJob));

        List<FrameworkImageGenerationItemStatusDto> items = service.listJobsByExperiment(30L);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).planningItemKey()).isEqualTo("hero");
        assertThat(items.get(0).status()).isEqualTo("PROCESSING");
        assertThat(items.get(1).planningItemKey()).isEqualTo("faq");
        assertThat(items.get(1).status()).isEqualTo("PLANNED");
    }

    @Test
    void listJobsByExperimentParsesPlanningWrappedInMarkdownCodeFence() {
        Experiment experiment = Experiment.builder()
                .id(31L)
                .landingPageImagePlanning("""
                        ```json
                        {
                          "images": [
                            {"sectionId":"hero","sectionName":"Hero","imagePrompt":"Prompt Hero"},
                            {"sectionId":"faq","sectionName":"FAQ","imagePrompt":"Prompt FAQ"}
                          ]
                        }
                        ```
                        """)
                .build();
        when(experimentRepository.findById(31L)).thenReturn(Optional.of(experiment));
        when(jobRepository.findByExperimentIdOrderByCreatedAtDesc(31L)).thenReturn(List.of());

        List<FrameworkImageGenerationItemStatusDto> items = service.listJobsByExperiment(31L);

        assertThat(items).hasSize(2);
        assertThat(items).extracting(FrameworkImageGenerationItemStatusDto::planningItemKey)
                .containsExactly("hero", "faq");
        assertThat(items).extracting(FrameworkImageGenerationItemStatusDto::status)
                .containsOnly("PLANNED");
    }

    @Test
    void listJobsByExperimentParsesFirstJsonObjectWhenPayloadIsDuplicated() {
        String onePlan = """
                {
                  "images": [
                    {"sectionId":"hero","sectionName":"Hero","imagePrompt":"Prompt Hero"},
                    {"sectionId":"faq","sectionName":"FAQ","imagePrompt":"Prompt FAQ"}
                  ]
                }
                """;
        Experiment experiment = Experiment.builder()
                .id(32L)
                .landingPageImagePlanning(onePlan + onePlan)
                .build();
        when(experimentRepository.findById(32L)).thenReturn(Optional.of(experiment));
        when(jobRepository.findByExperimentIdOrderByCreatedAtDesc(32L)).thenReturn(List.of());

        List<FrameworkImageGenerationItemStatusDto> items = service.listJobsByExperiment(32L);

        assertThat(items).hasSize(2);
        assertThat(items).extracting(FrameworkImageGenerationItemStatusDto::planningItemKey)
                .containsExactly("hero", "faq");
    }

    @Test
    void listJobsByExperimentParsesEscapedAndDuplicatedImagePlanPayload() {
        String escapedDuplicatedPlan = "{\\n  \\\"imagePlan\\\": [\\n    {\\\"sectionId\\\":\\\"hero\\\",\\\"sectionName\\\":\\\"Hero\\\",\\\"imagePrompt\\\":\\\"Prompt Hero\\\"},\\n    {\\\"sectionId\\\":\\\"faq\\\",\\\"sectionName\\\":\\\"FAQ\\\",\\\"imagePrompt\\\":\\\"Prompt FAQ\\\"}\\n  ]\\n}"
                + "{\\n  \\\"imagePlan\\\": [\\n    {\\\"sectionId\\\":\\\"hero\\\",\\\"sectionName\\\":\\\"Hero\\\",\\\"imagePrompt\\\":\\\"Prompt Hero\\\"}\\n  ]\\n}";
        Experiment experiment = Experiment.builder()
                .id(33L)
                .landingPageImagePlanning(escapedDuplicatedPlan)
                .build();
        when(experimentRepository.findById(33L)).thenReturn(Optional.of(experiment));
        when(jobRepository.findByExperimentIdOrderByCreatedAtDesc(33L)).thenReturn(List.of());

        List<FrameworkImageGenerationItemStatusDto> items = service.listJobsByExperiment(33L);

        assertThat(items).hasSize(2);
        assertThat(items).extracting(FrameworkImageGenerationItemStatusDto::planningItemKey)
                .containsExactly("hero", "faq");
    }

    @Test
    void listPendingWebnizationAssetsReturnsCompletedAssetsWithoutWebUrl() {
        FrameworkImageGenerationJob pendingWebnization = FrameworkImageGenerationJob.builder()
                .id(UUID.randomUUID())
                .experiment(Experiment.builder().id(44L).build())
                .planningItemKey("hero")
                .status(FrameworkImageGenerationJobStatus.COMPLETED)
                .stage(FrameworkImageGenerationJobStage.NOTIFIED_BACKEND)
                .assetId(501L)
                .sourceUrl("https://cdn/source.jpg")
                .updatedAt(Instant.parse("2026-04-08T10:00:00Z"))
                .build();
        when(jobRepository.findByStatusAndStageInAndAssetIdIsNotNullAndSourceUrlIsNotNullAndWebUrlIsNullOrderByUpdatedAtAsc(
                eq(FrameworkImageGenerationJobStatus.COMPLETED), any(Set.class), any()))
                .thenReturn(List.of(pendingWebnization));

        List<FrameworkImageWebnizationPendingAssetDto> assets = service.listPendingWebnizationAssets(20);

        assertThat(assets).hasSize(1);
        assertThat(assets.get(0).assetId()).isEqualTo(501L);
        assertThat(assets.get(0).jobId()).isEqualTo(pendingWebnization.getId());
    }

    @Test
    void markAssetAsWebReadyUpdatesStageAndKeepsOperationIdempotent() {
        FrameworkImageGenerationJob job = FrameworkImageGenerationJob.builder()
                .id(UUID.randomUUID())
                .experiment(Experiment.builder().id(77L).build())
                .status(FrameworkImageGenerationJobStatus.COMPLETED)
                .stage(FrameworkImageGenerationJobStage.NOTIFIED_BACKEND)
                .assetId(901L)
                .sourceUrl("https://cdn/source.jpg")
                .build();
        when(jobRepository.findFirstByAssetIdOrderByCreatedAtDesc(901L)).thenReturn(Optional.of(job));

        service.markAssetAsWebReady(901L, " https://cdn/web.webp ");

        assertThat(job.getStage()).isEqualTo(FrameworkImageGenerationJobStage.WEB_READY);
        assertThat(job.getWebUrl()).isEqualTo("https://cdn/web.webp");
        assertThat(job.getStatus()).isEqualTo(FrameworkImageGenerationJobStatus.COMPLETED);

        service.markAssetAsWebReady(901L, "https://cdn/web.webp");
        assertThat(job.getWebUrl()).isEqualTo("https://cdn/web.webp");
    }

    @Test
    void enqueueJobsForExperimentReturnsEmptyWhenRolloutDisabled() {
        FrameworkImageGenerationService disabledRolloutService =
                new FrameworkImageGenerationService(jobRepository, experimentRepository, new ObjectMapper(), false, 100);

        List<FrameworkImageGenerationJobDto> jobs = disabledRolloutService.enqueueJobsForExperiment(99L);

        assertThat(jobs).isEmpty();
        verify(experimentRepository, never()).findById(99L);
    }

    @Test
    void failStaleProcessingJobsMarksProcessingJobsAsFailed() {
        FrameworkImageGenerationJob staleJob = FrameworkImageGenerationJob.builder()
                .id(UUID.randomUUID())
                .experiment(Experiment.builder().id(55L).build())
                .status(FrameworkImageGenerationJobStatus.PROCESSING)
                .stage(FrameworkImageGenerationJobStage.CLAIMED)
                .startedAt(Instant.parse("2026-04-08T08:00:00Z"))
                .build();
        when(jobRepository.findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
                eq(FrameworkImageGenerationJobStatus.PROCESSING), any(Instant.class), any()))
                .thenReturn(List.of(staleJob));

        int failed = service.failStaleProcessingJobs(Instant.parse("2026-04-08T09:00:00Z"), 10, "timed out");

        assertThat(failed).isEqualTo(1);
        assertThat(staleJob.getStatus()).isEqualTo(FrameworkImageGenerationJobStatus.FAILED);
        assertThat(staleJob.getStage()).isEqualTo(FrameworkImageGenerationJobStage.FAILED);
        assertThat(staleJob.getErrorMessage()).isEqualTo("timed out");
        assertThat(staleJob.getFinishedAt()).isNotNull();
    }
}
