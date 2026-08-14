package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.financialagent.service.CommercialSceneEvaluationRequest;
import com.marketinghub.financialagent.service.ProviderTaskConsumptionView;
import com.marketinghub.financialagent.service.StudioProviderTaskConsumptionQueryService;
import com.marketinghub.media.Asset;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.VideoProject;
import com.marketinghub.salesvideo.dto.storyboard.VideoStoryboardResponse;
import com.marketinghub.salesvideo.tenant.TenantContext;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida a composição auditável do storyboard do Estúdio. */
@ExtendWith(MockitoExtension.class)
class VideoStoryboardServiceTest {
  @Mock private VideoProjectRepository projectRepository;
  @Mock private SalesVideoJobRepository jobRepository;
  @Mock private StudioProviderTaskConsumptionQueryService taskConsumptionQueryService;

  private VideoStoryboardService service;

  /** Configura tenant e fontes canônicas isoladas. */
  @BeforeEach
  void setUp() {
    TenantContextHolder.set(new TenantContext("tenant-musa", "editor@marketinghub.io", false));
    service =
        new VideoStoryboardService(
            projectRepository, jobRepository, taskConsumptionQueryService, new ObjectMapper());
  }

  /** Limpa o tenant após cada cenário. */
  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  /** Cruza task, arquivo e montagem sem tratar cena gerada como aproveitada automaticamente. */
  @Test
  void shouldExposeCreditsAndActualEditorialUtilizationPerScene() {
    VideoProject project =
        VideoProject.builder()
            .id(7L)
            .tenantId("tenant-musa")
            .salesVideoProfileId(55L)
            .scenePlan("Dor visível\nResultado concreto")
            .build();
    SalesVideoProfile profile = SalesVideoProfile.builder().id(55L).build();
    SalesVideoJob source =
        SalesVideoJob.builder()
            .id(101L)
            .profile(profile)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .jobType(SalesVideoJobType.RENDER)
            .status(SalesVideoStatus.VIDEO_READY)
            .metadataJson("{\"studio_project_id\":7,\"scene\":{\"order\":1,\"role\":\"DOR\"}}")
            .asset(Asset.builder().id(90L).url("https://assets.example/scene-1.mp4").build())
            .build();
    SalesVideoJob montage =
        SalesVideoJob.builder()
            .id(102L)
            .profile(profile)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .jobType(SalesVideoJobType.RENDER)
            .status(SalesVideoStatus.VIDEO_READY)
            .metadataJson("{\"sourceJobIds\":[101]}")
            .build();
    ProviderTaskConsumptionView task =
        new ProviderTaskConsumptionView(
            501L,
            101L,
            "runway-task-1",
            1,
            2,
            10,
            300,
            300,
            "PARTIAL",
            30,
            "Plano de apoio sem texto.",
            "Estúdio",
            Instant.parse("2026-08-13T12:05:00Z"),
            Instant.parse("2026-08-13T12:00:00Z"));

    given(projectRepository.findById(7L)).willReturn(Optional.of(project));
    given(jobRepository.findByProfileIdOrderByRequestedAtDesc(55L))
        .willReturn(List.of(montage, source));
    given(taskConsumptionQueryService.findBySalesVideoJobIds(anyCollection()))
        .willReturn(List.of(task));

    VideoStoryboardResponse storyboard = service.getStoryboard(7L);

    assertThat(storyboard.expectedCredits()).isEqualTo(300);
    assertThat(storyboard.consumedCredits()).isEqualTo(300);
    assertThat(storyboard.utilizationPercent()).isEqualTo(30);
    assertThat(storyboard.scenes()).hasSize(2);
    assertThat(storyboard.scenes().get(0).producedFileUrl())
        .isEqualTo("https://assets.example/scene-1.mp4");
    assertThat(storyboard.scenes().get(0).utilizationEvidence()).isEqualTo("USED_IN_READY_MONTAGE");
    assertThat(storyboard.scenes().get(0).commercialEvaluationStatus()).isEqualTo("PARTIAL");
    assertThat(storyboard.scenes().get(1).jobStatus()).isEqualTo("NOT_REQUESTED");
  }

  /** Persiste a avaliação apenas quando a task pertence ao projeto acessível. */
  @Test
  void shouldPersistCommercialSceneEvaluation() {
    VideoProject project =
        VideoProject.builder().id(7L).tenantId("tenant-musa").salesVideoProfileId(55L).build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(101L)
            .profile(SalesVideoProfile.builder().id(55L).build())
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .jobType(SalesVideoJobType.RENDER)
            .status(SalesVideoStatus.VIDEO_READY)
            .metadataJson("{\"studio_project_id\":7}")
            .build();
    ProviderTaskConsumptionView task =
        new ProviderTaskConsumptionView(
            501L, 101L, "task", 1, 1, 10, 300, 300, null, null, null, null, null, Instant.now());
    given(projectRepository.findById(7L)).willReturn(Optional.of(project));
    given(jobRepository.findByProfileIdOrderByRequestedAtDesc(55L)).willReturn(List.of(job));
    given(taskConsumptionQueryService.findBySalesVideoJobIds(anyCollection()))
        .willReturn(List.of(task));

    CommercialSceneEvaluationRequest request =
        new CommercialSceneEvaluationRequest("PARTIAL", 30, "Plano de apoio", "Estúdio");
    service.evaluateScene(7L, 501L, request);

    verify(taskConsumptionQueryService).evaluate(501L, request);
  }
}
