package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobEventRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoScriptRepository;
import com.marketinghub.salesvideo.SalesVideoExecutionMode;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoJobType;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import com.marketinghub.salesvideo.SalesVideoRetryReason;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.dto.JobCompletionRequest;
import com.marketinghub.salesvideo.dto.JobFailureRequest;
import com.marketinghub.salesvideo.dto.RequestSalesVideoMontageRequest;
import com.marketinghub.salesvideo.dto.RequestSalesVideoPostProductionRequest;
import com.marketinghub.salesvideo.dto.RetrySalesVideoJobRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.tenant.TenantContext;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida as regras de negocio dos jobs do modulo de video. */
@ExtendWith(MockitoExtension.class)
class SalesVideoJobServiceTest {

  @Mock private SalesVideoJobRepository jobRepository;

  @Mock private SalesVideoJobEventRepository eventRepository;

  @Mock private SalesVideoProfileRepository profileRepository;

  @Mock private SalesVideoScriptRepository scriptRepository;

  @Mock private AssetRepository assetRepository;

  @Mock private SalesVideoReprocessPolicy reprocessPolicy;
  @Mock private SalesVideoCompletedRenderAssetSync completedRenderAssetSync;

  private SalesVideoJobService service;
  private SalesVideoProductionCostCalculator costCalculator;

  /** Inicializa o service com dependencias simuladas para cada teste. */
  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    costCalculator = new SalesVideoProductionCostCalculator();
    service =
        new SalesVideoJobService(
            jobRepository,
            eventRepository,
            profileRepository,
            scriptRepository,
            assetRepository,
            reprocessPolicy,
            completedRenderAssetSync,
            new SalesVideoJobCostMetadataService(objectMapper, costCalculator),
            objectMapper);
  }

  /** Garante que os jobs de um perfil sao retornados em contrato de leitura. */
  @Test
  void shouldListJobsByProfile() {
    long profileId = 10L;
    SalesVideoProfile profile = SalesVideoProfile.builder().id(profileId).build();
    given(profileRepository.findById(profileId)).willReturn(Optional.of(profile));
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(55L)
            .profile(profile)
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .status(SalesVideoStatus.VIDEO_REQUESTED)
            .requestedAt(Instant.parse("2024-01-01T10:15:30Z"))
            .build();
    given(jobRepository.findByProfileIdOrderByRequestedAtDesc(profileId)).willReturn(List.of(job));

    List<SalesVideoJobDto> result = service.listJobsByProfile(profileId);

    assertThat(result).hasSize(1).first().extracting(SalesVideoJobDto::getId).isEqualTo(55L);
    assertThat(result.get(0).getStatus()).isEqualTo(SalesVideoStatus.VIDEO_REQUESTED);
  }

  /** Garante que a tela de produto lista todos os jobs do produto no tenant atual. */
  @Test
  void shouldListJobsByProduct() {
    long productId = 4L;
    TenantContextHolder.set(new TenantContext("tenant-a", "seller@example.com", false));
    SalesVideoProfile profile = SalesVideoProfile.builder().id(10L).tenantId("tenant-a").build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(56L)
            .profile(profile)
            .tenantId("tenant-a")
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .status(SalesVideoStatus.VIDEO_READY)
            .requestedAt(Instant.parse("2024-01-01T10:15:30Z"))
            .metadataJson("{\"cost_usd\":12.34}")
            .build();
    given(
            jobRepository.findByProfileProductIdAndTenantIdOrderByRequestedAtDesc(
                productId, "tenant-a"))
        .willReturn(List.of(job));

    try {
      List<SalesVideoJobDto> result = service.listJobsByProduct(productId);

      assertThat(result).hasSize(1).first().extracting(SalesVideoJobDto::getId).isEqualTo(56L);
      assertThat(result.get(0).getMetadataJson()).contains("cost_usd");
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** Estima custo para jobs legados do produto quando o provider nao informou custo. */
  @Test
  void shouldEstimateMissingCostWhenListingJobsByProduct() {
    long productId = 4L;
    TenantContextHolder.set(new TenantContext("tenant-a", "seller@example.com", false));
    SalesVideoProfile profile =
        SalesVideoProfile.builder().id(10L).tenantId("tenant-a").targetDurationSeconds(30).build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(57L)
            .profile(profile)
            .tenantId("tenant-a")
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("LUMA_RAY_3_2")
            .status(SalesVideoStatus.VIDEO_READY)
            .requestedAt(Instant.parse("2024-01-01T10:15:30Z"))
            .metadataJson("{\"resolution\":\"720p\"}")
            .build();
    given(
            jobRepository.findByProfileProductIdAndTenantIdOrderByRequestedAtDesc(
                productId, "tenant-a"))
        .willReturn(List.of(job));

    try {
      List<SalesVideoJobDto> result = service.listJobsByProduct(productId);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getMetadataJson()).contains("\"cost_usd\":2.7");
      assertThat(result.get(0).getMetadataJson()).contains("PROVIDER_RATE_CARD_ESTIMATE");
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** Confirma o catalogo de custos dos providers de video integrados. */
  @Test
  void shouldEstimateKnownVideoProvidersFromRateCard() {
    assertThat(costCalculator.estimateUsd("LUMA_RAY_3_2", "ray-3.2", 30, "720p"))
        .isEqualByComparingTo(new BigDecimal("2.7000"));
    assertThat(costCalculator.estimateUsd("KLING_3_0", "kling-v3-0", 10, "720p"))
        .isEqualByComparingTo(new BigDecimal("1.1200"));
    assertThat(costCalculator.estimateUsd("RUNWAY", "gen4.5", 10, "720p"))
        .isEqualByComparingTo(new BigDecimal("1.2000"));
    assertThat(costCalculator.estimateUsd("HEYGEN", "avatar_iv", 30, "720p"))
        .isEqualByComparingTo(new BigDecimal("1.5000"));
    assertThat(costCalculator.estimateUsd("VEO", "veo-3.1-generate-preview", 8, "720p"))
        .isEqualByComparingTo(new BigDecimal("3.2000"));
  }

  /** Garante erro de negocio quando o perfil solicitado nao existe. */
  @Test
  void shouldRejectWhenProfileDoesNotExist() {
    long missingId = 404L;
    given(profileRepository.findById(missingId)).willReturn(Optional.empty());

    assertThrows(VideoModuleException.class, () -> service.listJobsByProfile(missingId));
  }

  /** Preserva metadata operacional ao reprocessar render com avatar e voz do provider. */
  @Test
  void shouldPreserveMetadataWhenRetryingVideoJob() {
    SalesVideoProfile profile = SalesVideoProfile.builder().id(52L).tenantId("default").build();
    com.marketinghub.salesvideo.SalesVideoScript script =
        com.marketinghub.salesvideo.SalesVideoScript.builder().id(554L).build();
    SalesVideoJob failedJob =
        SalesVideoJob.builder()
            .id(20486L)
            .profile(profile)
            .script(script)
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("HEYGEN")
            .executionMode(SalesVideoExecutionMode.TEST)
            .status(SalesVideoStatus.VIDEO_FAILED)
            .retryAttempt(1)
            .metadataJson("{\"heygen_avatar_id\":\"avatar\",\"heygen_voice_id\":\"voice\"}")
            .build();
    RetrySalesVideoJobRequest request = new RetrySalesVideoJobRequest();
    request.setRequestedBy("operator@example.com");
    request.setReason(SalesVideoRetryReason.QUALITY_ASSURANCE);
    request.setNotes("Reprocessar com contrato preservado.");
    given(jobRepository.findById(20486L)).willReturn(Optional.of(failedJob));
    given(scriptRepository.findById(554L)).willReturn(Optional.of(script));
    given(jobRepository.save(any(SalesVideoJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    SalesVideoJobDto result = service.retry(20486L, request);

    assertThat(result.getMetadataJson()).contains("heygen_avatar_id").contains("heygen_voice_id");
    assertThat(result.getRetryOfJobId()).isEqualTo(20486L);
    assertThat(result.getRetryAttempt()).isEqualTo(2);
  }

  /** Bloqueia render que terminou tecnicamente, mas ficou curto demais para o perfil comercial. */
  @Test
  void shouldFailRenderWhenAuditedDurationIsShorterThanCommercialTarget() {
    SalesVideoProfile profile =
        SalesVideoProfile.builder()
            .id(6L)
            .targetDurationSeconds(30)
            .status(SalesVideoStatus.VIDEO_REQUESTED)
            .build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(20430L)
            .profile(profile)
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .status(SalesVideoStatus.VIDEO_PROCESSING)
            .build();
    JobCompletionRequest request = new JobCompletionRequest();
    request.setStatus(SalesVideoStatus.VIDEO_READY);
    request.setMetadataJson("{\"duration_seconds\":8,\"resolution\":\"720p\"}");
    request.setMessage("Render concluido pelo provider");
    given(jobRepository.findById(20430L)).willReturn(Optional.of(job));
    given(jobRepository.save(job)).willReturn(job);

    SalesVideoJobDto result = service.complete(20430L, request);

    assertThat(result.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_FAILED);
    assertThat(job.getFailureCode()).isEqualTo("RENDER_DURATION_SHORT");
    assertThat(job.getFailureDetail()).contains("8s").contains("30s");
    assertThat(profile.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_FAILED);
    verify(completedRenderAssetSync, never())
        .syncCompletedRender(
            org.mockito.Mockito.any(),
            org.mockito.Mockito.any(),
            org.mockito.Mockito.any(),
            org.mockito.Mockito.any());
    verify(completedRenderAssetSync)
        .syncFailedRender(
            org.mockito.Mockito.eq(job), org.mockito.Mockito.any(JobFailureRequest.class));
  }

  /** Aceita render quando a duração auditada atende a tolerância comercial do perfil. */
  @Test
  void shouldAcceptRenderWhenAuditedDurationMatchesCommercialTargetTolerance() {
    SalesVideoProfile profile =
        SalesVideoProfile.builder()
            .id(6L)
            .targetDurationSeconds(30)
            .status(SalesVideoStatus.VIDEO_REQUESTED)
            .build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(20431L)
            .profile(profile)
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .status(SalesVideoStatus.VIDEO_PROCESSING)
            .build();
    JobCompletionRequest request = new JobCompletionRequest();
    request.setStatus(SalesVideoStatus.VIDEO_READY);
    request.setMetadataJson("{\"duration_seconds\":28,\"resolution\":\"720p\"}");
    request.setStreamPlaybackUrl(" https://stream.example.com/video/playlist.m3u8 ");
    given(jobRepository.findById(20431L)).willReturn(Optional.of(job));
    given(jobRepository.save(job)).willReturn(job);

    SalesVideoJobDto result = service.complete(20431L, request);

    assertThat(result.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_READY);
    assertThat(result.getStreamPlaybackUrl())
        .isEqualTo("https://stream.example.com/video/playlist.m3u8");
    assertThat(job.getFailureCode()).isNull();
    verify(completedRenderAssetSync)
        .syncCompletedRender(
            org.mockito.Mockito.eq(job),
            org.mockito.Mockito.eq(request),
            org.mockito.Mockito.eq(28),
            org.mockito.Mockito.eq("720p"));
  }

  /** Propaga falha de render para ativos comerciais vinculados ao job. */
  @Test
  void shouldSyncFailedRenderWithExperimentVideoAssets() {
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(20431L)
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .status(SalesVideoStatus.VIDEO_PROCESSING)
            .build();
    JobFailureRequest request = new JobFailureRequest();
    request.setStatus(SalesVideoStatus.VIDEO_FAILED);
    request.setFailureCode("VIDEO_MODULE_ERROR");
    request.setFailureDetail("404 Not Found from POST https://agents.lumalabs.ai/v1/generations");
    given(jobRepository.findById(20431L)).willReturn(Optional.of(job));
    given(jobRepository.save(job)).willReturn(job);

    SalesVideoJobDto result = service.fail(20431L, request);

    assertThat(result.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_FAILED);
    verify(completedRenderAssetSync).syncFailedRender(job, request);
  }

  /** Cria job de pós-produção preservando origem e textos comerciais do vídeo final. */
  @Test
  void shouldRequestPostProductionFromReadyVideoJob() {
    TenantContextHolder.set(new TenantContext("tenant-a", "seller@example.com", false));
    SalesVideoProfile profile =
        SalesVideoProfile.builder()
            .id(6L)
            .tenantId("tenant-a")
            .status(SalesVideoStatus.VIDEO_READY)
            .build();
    SalesVideoJob sourceJob =
        SalesVideoJob.builder()
            .id(20432L)
            .profile(profile)
            .tenantId("tenant-a")
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("LUMA_RAY_3_2")
            .executionMode(com.marketinghub.salesvideo.SalesVideoExecutionMode.TEST)
            .status(SalesVideoStatus.VIDEO_READY)
            .streamPlaybackUrl("https://cdn.example.com/source.mp4")
            .requestedAt(Instant.parse("2026-07-24T10:00:00Z"))
            .build();
    RequestSalesVideoPostProductionRequest request = new RequestSalesVideoPostProductionRequest();
    request.setRequestedBy("operator@tenant.io");
    request.setVoiceOverScript("Você se arruma e sente que falta presença.");
    request.setCaptionText("Veja seu plano MUSA de 7 dias.");

    given(jobRepository.findById(20432L)).willReturn(Optional.of(sourceJob));
    given(jobRepository.save(any(SalesVideoJob.class)))
        .willAnswer(
            invocation -> {
              SalesVideoJob saved = invocation.getArgument(0);
              if (saved.getId() == null) {
                saved.setId(20433L);
              }
              return saved;
            });
    given(eventRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    try {
      SalesVideoJobDto result = service.requestPostProduction(20432L, request);

      assertThat(result.getJobType()).isEqualTo(SalesVideoJobType.POST_PRODUCTION);
      assertThat(result.getProviderName()).isEqualTo("MUSA_POST_PRODUCTION");
      assertThat(result.getMetadataJson()).contains("sourceVideoUrl");
      assertThat(result.getMetadataJson()).contains("voiceOverScript");
      assertThat(result.getAuditSnapshotJson()).contains("\"sourceJobId\":20432");
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** Cria job de montagem preservando a ordem e as URLs dos clipes selecionados. */
  @Test
  void shouldRequestMontageFromReadyVideoJobs() {
    TenantContextHolder.set(new TenantContext("tenant-a", "seller@example.com", false));
    SalesVideoProfile profile =
        SalesVideoProfile.builder()
            .id(6L)
            .tenantId("tenant-a")
            .status(SalesVideoStatus.VIDEO_READY)
            .build();
    SalesVideoJob firstSource =
        SalesVideoJob.builder()
            .id(20440L)
            .profile(profile)
            .tenantId("tenant-a")
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("VEO")
            .executionMode(com.marketinghub.salesvideo.SalesVideoExecutionMode.TEST)
            .status(SalesVideoStatus.VIDEO_READY)
            .streamPlaybackUrl("https://cdn.example.com/scene-1.mp4")
            .requestedAt(Instant.parse("2026-07-24T10:00:00Z"))
            .build();
    SalesVideoJob secondSource =
        SalesVideoJob.builder()
            .id(20441L)
            .profile(profile)
            .tenantId("tenant-a")
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("VEO")
            .executionMode(com.marketinghub.salesvideo.SalesVideoExecutionMode.TEST)
            .status(SalesVideoStatus.VIDEO_READY)
            .streamPlaybackUrl("https://cdn.example.com/scene-2.mp4")
            .requestedAt(Instant.parse("2026-07-24T10:01:00Z"))
            .build();
    RequestSalesVideoMontageRequest request = new RequestSalesVideoMontageRequest();
    request.setRequestedBy("operator@tenant.io");
    request.setSourceJobIds(List.of(20440L, 20441L));

    given(jobRepository.findById(20440L)).willReturn(Optional.of(firstSource));
    given(jobRepository.findById(20441L)).willReturn(Optional.of(secondSource));
    given(jobRepository.save(any(SalesVideoJob.class)))
        .willAnswer(
            invocation -> {
              SalesVideoJob saved = invocation.getArgument(0);
              if (saved.getId() == null) {
                saved.setId(20442L);
              }
              return saved;
            });
    given(eventRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    try {
      SalesVideoJobDto result = service.requestMontage(request);

      assertThat(result.getJobType()).isEqualTo(SalesVideoJobType.POST_PRODUCTION);
      assertThat(result.getProviderName()).isEqualTo("MUSA_VIDEO_MONTAGE");
      assertThat(result.getMetadataJson()).contains("sourceVideos");
      assertThat(result.getMetadataJson()).contains("scene-1.mp4", "scene-2.mp4");
      assertThat(result.getAuditSnapshotJson()).contains("\"sourceJobIds\":[20440,20441]");
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** Bloqueia quatro variações da mesma cena na montagem narrativa do Estúdio. */
  @Test
  void shouldRejectSceneMontageWithoutFourDistinctNarrativeRoles() {
    SalesVideoProfile profile = SalesVideoProfile.builder().id(6L).tenantId("tenant-a").build();
    List<SalesVideoJob> sources =
        java.util.stream.LongStream.rangeClosed(1, 4)
            .mapToObj(
                id ->
                    SalesVideoJob.builder()
                        .id(id)
                        .profile(profile)
                        .tenantId("tenant-a")
                        .jobType(SalesVideoJobType.RENDER)
                        .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
                        .providerName("LUMA_RAY_3_2")
                        .executionMode(SalesVideoExecutionMode.TEST)
                        .status(SalesVideoStatus.VIDEO_READY)
                        .streamPlaybackUrl("https://cdn.example.com/scene-" + id + ".mp4")
                        .metadataJson(
                            "{\"generation_strategy\":\"SCENE_BY_SCENE_MONTAGE\","
                                + "\"studio_project_id\":1,\"scene\":{\"order\":1,\"role\":\"DOR\"}}")
                        .build())
            .toList();
    sources.forEach(job -> given(jobRepository.findById(job.getId())).willReturn(Optional.of(job)));
    RequestSalesVideoMontageRequest request = new RequestSalesVideoMontageRequest();
    request.setRequestedBy("reviewer@tenant.io");
    request.setSourceJobIds(sources.stream().map(SalesVideoJob::getId).toList());

    VideoModuleException ex =
        assertThrows(VideoModuleException.class, () -> service.requestMontage(request));

    assertThat(ex.getMessage()).contains("DOR, RESULTADO, MECANISMO e CTA");
  }
}
