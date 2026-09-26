package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.media.Asset;
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
import com.marketinghub.salesvideo.SalesVideoScript;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.dto.JobClaimRequest;
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

  /** Retoma só o filho falho da fonte atual e rejeita fontes de tentativas posteriores. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"FAILED_SAME", "FAILED_OTHER", "READY_SAME"})
  void shouldCorrelateFinalizationRecoveryWithoutOverwritingNewerWork(String scenario) {
    var source = VideoFinalDeliveryContractTest.source();
    source.setJobType(SalesVideoJobType.RENDER);
    source.setProviderName("RUNWAY_ROUTER");
    source.setMetadataJson(
        "{\"videoProductionCycleId\":91022,\"videoProjectId\":91004,\"experimentId\":91001,\"targetDurationSeconds\":15}");
    var current =
        SalesVideoJob.builder()
            .id(91010L)
            .tenantId("default")
            .profile(source.getProfile())
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .status(
                scenario.equals("READY_SAME")
                    ? SalesVideoStatus.VIDEO_READY
                    : SalesVideoStatus.VIDEO_FAILED)
            .retryOfJob(
                scenario.equals("FAILED_OTHER")
                    ? SalesVideoJob.builder().id(91999L).build()
                    : source)
            .build();
    var cycle = new com.marketinghub.salesvideo.VideoProductionCycle();
    cycle.setId(91022L);
    cycle.setVideoProjectId(91004L);
    cycle.setExperimentId(91001L);
    cycle.setSalesVideoJobId(current.getId());
    cycle.setStatus("APOLLO_BLOCKED");
    var cycles =
        org.mockito.Mockito.mock(
            com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository.class);
    service.setProductionCycleRepository(cycles);
    given(cycles.findById(91022L)).willReturn(Optional.of(cycle));
    given(jobRepository.findById(source.getId())).willReturn(Optional.of(source));
    given(jobRepository.findByIdForUpdate(source.getId())).willReturn(Optional.of(source));
    given(jobRepository.findById(current.getId())).willReturn(Optional.of(current));
    var request = new RequestSalesVideoPostProductionRequest();
    request.setRequestedBy("fixture@sandbox.local");
    request.setCaptionText("Copy menor");
    request.setVoiceOverScript("Copy menor");
    if (scenario.equals("FAILED_SAME")) {
      given(jobRepository.save(any()))
          .willAnswer(
              i -> {
                SalesVideoJob j = i.getArgument(0);
                if (j.getId() == null) j.setId(91011L);
                return j;
              });
      var result = service.requestPostProduction(source.getId(), request);
      assertThat(result.getId()).isEqualTo(91011L);
      assertThat(result.getExecutionMode()).isEqualTo(SalesVideoExecutionMode.TEST);
      assertThat(result.getMetadataJson()).contains("91022", "91004", "91001", "Copy menor");
      assertThat(cycle.getSalesVideoJobId()).isEqualTo(91011L);
      assertThat(cycle.getStatus()).isEqualTo("QUEUED_FOR_APOLLO");
      assertThat(current.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_FAILED);
      verify(cycles).save(cycle);
    } else {
      assertThrows(
          VideoModuleException.class, () -> service.requestPostProduction(source.getId(), request));
      assertThat(cycle.getSalesVideoJobId()).isEqualTo(current.getId());
      verify(jobRepository, never()).save(any());
      verify(cycles, never()).save(any());
    }
  }

  /** Reutiliza a chamada ativa igual e bloqueia edição concorrente sem enfileirar outro consumo. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void shouldPreventConcurrentFinalizationRequests(boolean sameText) {
    var source = VideoFinalDeliveryContractTest.source();
    var child =
        SalesVideoJob.builder()
            .id(91010L)
            .tenantId("default")
            .profile(source.getProfile())
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .status(SalesVideoStatus.VIDEO_PROCESSING)
            .metadataJson(
                "{\"captionText\":\"Copy menor\",\"voiceOverScript\":\"Copy menor\",\"sourceVideoUrl\":\"https://cdn.test/final.mp4\"}")
            .build();
    given(jobRepository.findById(source.getId())).willReturn(Optional.of(source));
    given(jobRepository.findByIdForUpdate(source.getId())).willReturn(Optional.of(source));
    given(jobRepository.findFirstByRetryOfJob_IdOrderByRequestedAtDescIdDesc(source.getId()))
        .willReturn(Optional.of(child));
    var request = new RequestSalesVideoPostProductionRequest();
    request.setCaptionText(sameText ? "Copy menor" : "Outra copy");
    request.setVoiceOverScript("Copy menor");
    if (sameText)
      assertThat(service.requestPostProduction(source.getId(), request).getId())
          .isEqualTo(child.getId());
    else
      assertThrows(
          VideoModuleException.class, () -> service.requestPostProduction(source.getId(), request));
    verify(jobRepository, never()).save(any());
  }

  /** Expõe disponibilidade e bloqueia nova preparação quando já existe filho em execução. */
  @Test
  void shouldExposeBackendDeliveryAvailability() {
    var source = VideoFinalDeliveryContractTest.source();
    given(jobRepository.findById(source.getId())).willReturn(Optional.of(source));
    var available = service.getJob(source.getId()).getDeliveryPreparation();
    assertThat(available.status()).isEqualTo("AVAILABLE");
    assertThat(available.captionText()).isEqualTo("Copy preservada");
    var child =
        SalesVideoJob.builder()
            .id(91010L)
            .status(SalesVideoStatus.VIDEO_REQUESTED)
            .metadataJson("{\"deliveryOnly\":true}")
            .build();
    given(jobRepository.findFirstByRetryOfJob_IdOrderByRequestedAtDesc(source.getId()))
        .willReturn(Optional.of(child));
    var pending = service.getJob(source.getId()).getDeliveryPreparation();
    assertThat(pending.status()).isEqualTo("PROCESSING");
    assertThat(pending.jobId()).isEqualTo(91010L);
    assertThat(pending.captionText()).isNull();
    verify(jobRepository, never()).save(any());
  }

  /** Arquivo sem identidade não pode disponibilizar recuperação apenas por estar VIDEO_READY. */
  @Test
  void shouldRejectDeliveryAvailabilityWithoutPersistedHash() {
    var source = VideoFinalDeliveryContractTest.source();
    source.getAsset().setPayload("{}");
    given(jobRepository.findById(source.getId())).willReturn(Optional.of(source));
    var state = service.getJob(source.getId()).getDeliveryPreparation();
    assertThat(state.status()).isEqualTo("UNAVAILABLE");
    assertThat(state.reason()).contains("hashes");
    verify(jobRepository, never()).save(any());
  }

  /** Serializa a recuperação e devolve o filho existente sem duplicar processamento. */
  @Test
  void shouldReuseExistingDeliveryJob() {
    var source = VideoFinalDeliveryContractTest.source();
    var child =
        SalesVideoJob.builder()
            .id(91010L)
            .tenantId("default")
            .profile(source.getProfile())
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .status(SalesVideoStatus.VIDEO_REQUESTED)
            .metadataJson("{\"deliveryOnly\":true}")
            .build();
    given(jobRepository.findById(source.getId())).willReturn(Optional.of(source));
    given(jobRepository.findByIdForUpdate(source.getId())).willReturn(Optional.of(source));
    given(jobRepository.findFirstByRetryOfJob_IdOrderByRequestedAtDescIdDesc(source.getId()))
        .willReturn(Optional.of(child));
    assertThat(
            service
                .requestPostProduction(source.getId(), VideoFinalDeliveryContractTest.request())
                .getId())
        .isEqualTo(child.getId());
    verify(jobRepository, never()).save(any());
  }

  /** Bloqueia recuperação de outro tenant antes de reservar ou criar um filho. */
  @Test
  void shouldRejectCrossTenantDelivery() {
    var source = VideoFinalDeliveryContractTest.source();
    given(jobRepository.findById(source.getId())).willReturn(Optional.of(source));
    TenantContextHolder.set(new TenantContext("tenant-b", "fixture@sandbox.local", false));
    try {
      assertThrows(
          VideoModuleException.class,
          () ->
              service.requestPostProduction(
                  source.getId(), VideoFinalDeliveryContractTest.request()));
      verify(jobRepository, never()).findByIdForUpdate(any());
      verify(jobRepository, never()).save(any());
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** Cria um filho de entrega e persiste o HLS do callback mantendo modo de teste e custo zero. */
  @Test
  void shouldCreateAndCompleteDeliveryWithHls() {
    var source = VideoFinalDeliveryContractTest.source();
    given(jobRepository.findById(source.getId())).willReturn(Optional.of(source));
    given(jobRepository.findByIdForUpdate(source.getId())).willReturn(Optional.of(source));
    given(jobRepository.save(any()))
        .willAnswer(
            invocation -> {
              SalesVideoJob value = invocation.getArgument(0);
              if (value.getId() == null) value.setId(91010L);
              return value;
            });
    var result =
        service.requestPostProduction(source.getId(), VideoFinalDeliveryContractTest.request());
    assertThat(result.getId()).isEqualTo(91010L);
    assertThat(result.getExecutionMode()).isEqualTo(SalesVideoExecutionMode.TEST);
    assertThat(result.getMetadataJson()).contains("VIDEO_FINAL_REUSE_V1", "deliveryOnly");
    var captor = org.mockito.ArgumentCaptor.forClass(SalesVideoJob.class);
    verify(jobRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
    var child = captor.getValue();
    given(jobRepository.findById(91010L)).willReturn(Optional.of(child));
    given(assetRepository.findById(92001L)).willReturn(Optional.of(source.getAsset()));
    given(assetRepository.findById(92002L)).willReturn(Optional.of(source.getVttAsset()));
    var callback = new JobCompletionRequest();
    callback.setAssetId(92001L);
    callback.setVttAssetId(92002L);
    callback.setStatus(SalesVideoStatus.VIDEO_READY);
    callback.setStreamPlaybackUrl("https://cdn.test/final.m3u8");
    callback.setCostUsd(BigDecimal.ZERO);
    callback.setMetadataJson(
        "{\"duration_seconds\":15,\"deliveryOnly\":true,\"hls_delivery\":{\"status\":\"READY\"}}");
    var completed = service.complete(91010L, callback);
    assertThat(completed.getStreamPlaybackUrl()).isEqualTo("https://cdn.test/final.m3u8");
    assertThat(completed.getMetadataJson()).contains("hls_delivery", "VIDEO_FINAL_REUSE_V1");
    assertThat(completed.getCommercialReadinessBlockers())
        .doesNotContain("A playlist HLS publicável não foi registrada.");
    assertThat(source.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_READY);
  }

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

  /** Reserva o job uma única vez e publica a transição canônica para processamento. */
  @Test
  void shouldClaimJobAtomically() {
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(21234L)
            .tenantId("default")
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .status(SalesVideoStatus.VIDEO_REQUESTED)
            .build();
    JobClaimRequest request = new JobClaimRequest();
    request.setWorkerId("video-worker-a");
    given(jobRepository.findById(21234L)).willReturn(Optional.of(job));
    given(
            jobRepository.claimIfAvailable(
                eq(21234L),
                eq(SalesVideoStatus.VIDEO_REQUESTED),
                eq(SalesVideoStatus.VIDEO_PROCESSING),
                any(Instant.class),
                any(Instant.class)))
        .willAnswer(
            invocation -> {
              job.setStatus(SalesVideoStatus.VIDEO_PROCESSING);
              job.setStartedAt(invocation.getArgument(3));
              return 1;
            });

    SalesVideoJobDto result = service.claimJob(21234L, request);

    assertThat(result.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_PROCESSING);
    assertThat(result.getStartedAt()).isNotNull();
    verify(eventRepository).save(any());
  }

  /** Recusa um segundo worker enquanto a lease do primeiro ainda está vigente. */
  @Test
  void shouldRejectDuplicateClaim() {
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(21234L)
            .tenantId("default")
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .status(SalesVideoStatus.VIDEO_PROCESSING)
            .build();
    JobClaimRequest request = new JobClaimRequest();
    request.setWorkerId("video-worker-b");
    given(jobRepository.findById(21234L)).willReturn(Optional.of(job));

    VideoModuleException error =
        assertThrows(VideoModuleException.class, () -> service.claimJob(21234L, request));

    assertThat(error.getStatus().value()).isEqualTo(409);
    assertThat(error.getErrorCode().name()).isEqualTo("JOB_CLAIM_CONFLICT");
    verify(eventRepository, never()).save(any());
  }

  /** Preserva o vídeo pronto quando uma execução concorrente reporta falha atrasada. */
  @Test
  void shouldIgnoreLateFailureAfterVideoReady() {
    Asset finalAsset = Asset.builder().id(2773L).build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(21234L)
            .tenantId("default")
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("MUSA_POST_PRODUCTION")
            .status(SalesVideoStatus.VIDEO_READY)
            .asset(finalAsset)
            .build();
    JobFailureRequest request = new JobFailureRequest();
    request.setStatus(SalesVideoStatus.VIDEO_FAILED);
    request.setFailureCode("APOLLO_VIDEO_STABILITY_REJECTED");
    request.setFailureDetail("Callback atrasado do worker antigo.");
    given(jobRepository.findById(21234L)).willReturn(Optional.of(job));

    SalesVideoJobDto result = service.fail(21234L, request);

    assertThat(result.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_READY);
    assertThat(result.getAssetId()).isEqualTo(2773L);
    assertThat(result.getFailureCode()).isNull();
    verify(jobRepository, never()).save(any());
    verify(eventRepository, never()).save(any());
  }

  /** Aceita como idempotente apenas a repetição identificável da mesma conclusão de vídeo. */
  @Test
  void shouldAcceptIdenticalCompletionAfterVideoReady() {
    Asset finalAsset = Asset.builder().id(2780L).build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(21234L)
            .tenantId("default")
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("MUSA_POST_PRODUCTION")
            .providerJobId("post-production-21234-approved-reference")
            .status(SalesVideoStatus.VIDEO_READY)
            .asset(finalAsset)
            .build();
    JobCompletionRequest request = new JobCompletionRequest();
    request.setStatus(SalesVideoStatus.VIDEO_READY);
    request.setAssetId(2780L);
    request.setProviderJobId("post-production-21234-approved-reference");
    given(jobRepository.findById(21234L)).willReturn(Optional.of(job));

    SalesVideoJobDto result = service.complete(21234L, request);

    assertThat(result.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_READY);
    assertThat(result.getAssetId()).isEqualTo(2780L);
    verify(jobRepository, never()).save(any());
    verify(eventRepository, never()).save(any());
  }

  /** Recusa conclusão tardia divergente em qualquer estado final de sucesso. */
  @Test
  void shouldRejectDivergentCompletionAcrossSuccessfulTerminalStates() {
    List<SalesVideoStatus> terminalStatuses =
        List.of(
            SalesVideoStatus.SCRIPT_READY,
            SalesVideoStatus.STORYBOARD_READY,
            SalesVideoStatus.VIDEO_READY,
            SalesVideoStatus.PUBLISHED,
            SalesVideoStatus.ARCHIVED);

    for (int index = 0; index < terminalStatuses.size(); index++) {
      long jobId = 22000L + index;
      SalesVideoJob job =
          SalesVideoJob.builder()
              .id(jobId)
              .tenantId("default")
              .jobType(SalesVideoJobType.POST_PRODUCTION)
              .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
              .providerJobId("provider-original-" + index)
              .status(terminalStatuses.get(index))
              .asset(Asset.builder().id(2800L + index).build())
              .build();
      JobCompletionRequest request = new JobCompletionRequest();
      request.setAssetId(2900L + index);
      request.setProviderJobId("provider-atrasado-" + index);
      given(jobRepository.findById(jobId)).willReturn(Optional.of(job));

      VideoModuleException error =
          assertThrows(VideoModuleException.class, () -> service.complete(jobId, request));

      assertThat(error.getStatus().value()).isEqualTo(409);
      assertThat(error.getErrorCode().name()).isEqualTo("JOB_CLAIM_CONFLICT");
    }
    verify(jobRepository, never()).save(any());
    verify(eventRepository, never()).save(any());
  }

  /** Recusa repetição sem asset ou identificador do provider, pois não há prova de idempotência. */
  @Test
  void shouldRejectUnidentifiableCompletionAfterScriptReady() {
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(22010L)
            .tenantId("default")
            .jobType(SalesVideoJobType.SCRIPT)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .status(SalesVideoStatus.SCRIPT_READY)
            .build();
    JobCompletionRequest request = new JobCompletionRequest();
    request.setStatus(SalesVideoStatus.SCRIPT_READY);
    given(jobRepository.findById(22010L)).willReturn(Optional.of(job));

    VideoModuleException error =
        assertThrows(VideoModuleException.class, () -> service.complete(22010L, request));

    assertThat(error.getStatus().value()).isEqualTo(409);
    assertThat(error.getErrorCode().name()).isEqualTo("JOB_CLAIM_CONFLICT");
    verify(jobRepository, never()).save(any());
    verify(eventRepository, never()).save(any());
  }

  /** Bloqueia clipe bruto silencioso mesmo quando o provider concluiu o render. */
  @Test
  void shouldExposeCommercialBlockersForRawReadyClip() {
    long profileId = 10L;
    SalesVideoProfile profile = SalesVideoProfile.builder().id(profileId).build();
    given(profileRepository.findById(profileId)).willReturn(Optional.of(profile));
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(20519L)
            .profile(profile)
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("KLING_3_0")
            .status(SalesVideoStatus.VIDEO_READY)
            .streamPlaybackUrl("https://cdn.example.com/scene.mp4")
            .build();
    given(jobRepository.findByProfileIdOrderByRequestedAtDesc(profileId)).willReturn(List.of(job));

    SalesVideoJobDto result = service.listJobsByProfile(profileId).get(0);

    assertThat(result.getCommercialReadinessStatus()).isEqualTo("BLOCKED");
    assertThat(result.getCommercialReadinessBlockers())
        .contains(
            "O ativo ainda é um clipe bruto ou uma montagem sem pós-produção final.",
            "A narração em português do Brasil não foi incorporada.",
            "A peça não deriva de uma fonte visual narrativa auditável.");
  }

  /** Aprova somente a peça final com montagem, áudio, legenda, CTA, HLS e revisão humana. */
  @Test
  void shouldApproveCommerciallyCompletePostProduction() {
    long profileId = 10L;
    SalesVideoProfile profile =
        SalesVideoProfile.builder()
            .id(profileId)
            .humanReviewApprovedAt(Instant.parse("2026-08-05T12:00:00Z"))
            .build();
    given(profileRepository.findById(profileId)).willReturn(Optional.of(profile));
    SalesVideoScript script = SalesVideoScript.builder().ctaText("Ver meu plano MUSA").build();
    SalesVideoJob montage =
        SalesVideoJob.builder().id(20520L).providerName("MUSA_VIDEO_MONTAGE").build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(20521L)
            .profile(profile)
            .script(script)
            .retryOfJob(montage)
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("MUSA_POST_PRODUCTION")
            .status(SalesVideoStatus.VIDEO_READY)
            .streamPlaybackUrl("https://cdn.example.com/musa-v7.m3u8")
            .vttAsset(Asset.builder().id(902L).build())
            .metadataJson(
                "{\"audio\":{\"voice_over\":true,\"language\":\"pt-BR\"},"
                    + "\"captions\":{\"burned_in\":true,\"vtt_asset\":true}}")
            .build();
    given(jobRepository.findByProfileIdOrderByRequestedAtDesc(profileId)).willReturn(List.of(job));

    SalesVideoJobDto result = service.listJobsByProfile(profileId).get(0);

    assertThat(result.getCommercialReadinessStatus()).isEqualTo("READY");
    assertThat(result.getCommercialReadinessBlockers()).isEmpty();
  }

  /** Exige prova íntegra, voz, sincronismo e decisão humana também na rota genérica. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "RUNWAY_ROUTER,complete",
    "EDITORIAL_MOTION,complete",
    "RUNWAY_ROUTER,hash",
    "RUNWAY_ROUTER,audio",
    "RUNWAY_ROUTER,human"
  })
  void shouldGatePrivatePdePostProduction(String sourceProvider, String scenario) throws Exception {
    SalesVideoProfile profile = SalesVideoProfile.builder().id(10L).build();
    if (!"human".equals(scenario))
      profile.setHumanReviewApprovedAt(Instant.parse("2026-09-13T12:00:00Z"));
    SalesVideoJob source =
        SalesVideoJob.builder()
            .id(91010L)
            .providerName(sourceProvider)
            .metadataJson(
                "{\"post_production\":{\"product_proof\":{\"contractVersion\":\"PDE_PRIVATE_VIDEO_PROOF_V1\",\"sha256\":\"fixture-hash\"}}}")
            .build();
    var metadata =
        new ObjectMapper()
            .readTree(
                """
        {"audio":{"voice_over":true,"language":"pt-BR","review":{"status":"APPROVED_FOR_TEST"}},
         "captions":{"burned_in":true,"vtt_asset":true},
         "caption_narration_sync":{"status":"APPROVED","timing_status":"APPROVED"},
         "product_reference_overlay":{"status":"APPLIED","sha256":"fixture-hash","commercialEvidenceClaimed":false}}
        """);
    if ("hash".equals(scenario))
      ((com.fasterxml.jackson.databind.node.ObjectNode) metadata.path("product_reference_overlay"))
          .put("sha256", "different");
    if ("audio".equals(scenario))
      ((com.fasterxml.jackson.databind.node.ObjectNode) metadata.at("/audio/review"))
          .put("status", "PENDING");
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(91011L)
            .profile(profile)
            .retryOfJob(source)
            .script(SalesVideoScript.builder().ctaText("Ver primeiro ajuste").build())
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("MUSA_POST_PRODUCTION")
            .status(SalesVideoStatus.VIDEO_READY)
            .streamPlaybackUrl("https://fixture.invalid/video.m3u8")
            .vttAsset(Asset.builder().id(91012L).build())
            .metadataJson(metadata.toString())
            .build();
    given(profileRepository.findById(10L)).willReturn(Optional.of(profile));
    given(jobRepository.findByProfileIdOrderByRequestedAtDesc(10L)).willReturn(List.of(job));
    var result = service.listJobsByProfile(10L).get(0);
    assertThat(result.getCommercialReadinessStatus())
        .isEqualTo("complete".equals(scenario) ? "READY" : "BLOCKED");
    if (!"complete".equals(scenario))
      assertThat(result.getCommercialReadinessBlockers()).hasSize(1);
  }

  /** Encadeia fontes governadas na pós-produção preservando os gates técnicos de Apolo. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"RUNWAY_PRODUCT_UGC", "RUNWAY_ROUTER", "EDITORIAL_MOTION"})
  void shouldEnqueuePremiumFinalizationForGovernedSource(String sourceProvider) {
    TenantContextHolder.set(new TenantContext("tenant-a", "operator@tenant.io", false));
    SalesVideoProfile profile =
        SalesVideoProfile.builder()
            .id(10L)
            .tenantId("tenant-a")
            .targetDurationSeconds(15)
            .status(SalesVideoStatus.VIDEO_PROCESSING)
            .build();
    SalesVideoJob source =
        SalesVideoJob.builder()
            .id(20522L)
            .tenantId("tenant-a")
            .profile(profile)
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName(sourceProvider)
            .status(SalesVideoStatus.VIDEO_PROCESSING)
            .executionMode(SalesVideoExecutionMode.TEST)
            .requestedBy("Apolo")
            .requestedAt(Instant.parse("2026-09-04T10:00:00Z"))
            .metadataJson(
                "{\"videoProductionCycleId\":91,\"videoProjectId\":91001,\"experimentId\":91,"
                    + "\"technicalQualityGate\":{\"continuousTakeRequired\":true,"
                    + "\"captionMustMatchNarration\":true},"
                    + "\"referenceGovernance\":{\"productIsDigitalExperience\":true},"
                    + "\"premiumFinalization\":{\"enabled\":true,"
                    + "\"captionText\":\"Você se arruma | Faça o diagnóstico gratuito\","
                    + "\"voiceOverScript\":\"Você se arruma | Faça o diagnóstico gratuito\"}}")
            .build();
    var cycles =
        org.mockito.Mockito.mock(
            com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository.class);
    var cycle = new com.marketinghub.salesvideo.VideoProductionCycle();
    cycle.setId(91L);
    cycle.setVideoProjectId(91001L);
    cycle.setExperimentId(91L);
    cycle.setSalesVideoJobId(source.getId());
    given(cycles.findById(91L)).willReturn(Optional.of(cycle));
    service.setProductionCycleRepository(cycles);
    Asset sourceAsset =
        Asset.builder().id(903L).url("https://cdn.example.com/vega-91-ugc.mp4").build();
    given(jobRepository.findById(20522L)).willReturn(Optional.of(source));
    given(jobRepository.findByIdForUpdate(20522L)).willReturn(Optional.of(source));
    given(assetRepository.findById(903L)).willReturn(Optional.of(sourceAsset));
    given(jobRepository.save(any(SalesVideoJob.class)))
        .willAnswer(
            invocation -> {
              SalesVideoJob saved = invocation.getArgument(0);
              if (saved.getId() == null) saved.setId(20523L);
              return saved;
            });
    given(eventRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
    JobCompletionRequest request = new JobCompletionRequest();
    request.setStatus(SalesVideoStatus.VIDEO_READY);
    request.setAssetId(903L);
    request.setStreamPlaybackUrl("https://cdn.example.com/vega-91-ugc.mp4");
    request.setMetadataJson(
        "{\"duration_seconds\":15,\"cost_usd\":6.48,"
            + "\"technicalQualityGate\":{\"captionMustMatchNarration\":false},"
            + "\"apollo_technical_quality\":{\"stability_status\":\"APPROVED\"}}");

    try {
      service.complete(20522L, request);

      org.mockito.ArgumentCaptor<SalesVideoJob> jobs =
          org.mockito.ArgumentCaptor.forClass(SalesVideoJob.class);
      verify(jobRepository, org.mockito.Mockito.atLeastOnce()).save(jobs.capture());
      SalesVideoJob postProduction =
          jobs.getAllValues().stream()
              .filter(job -> "MUSA_POST_PRODUCTION".equals(job.getProviderName()))
              .findFirst()
              .orElseThrow();
      assertThat(postProduction.getRetryOfJob()).isSameAs(source);
      assertThat(cycle.getSalesVideoJobId()).isEqualTo(postProduction.getId());
      assertThat(cycle.getStatus()).isEqualTo("QUEUED_FOR_APOLLO");
      verify(cycles).save(cycle);
      assertThat(source.getMetadataJson()).contains("\"captionMustMatchNarration\":true");
      assertThat(postProduction.getMetadataJson())
          .contains(
              "\"technicalQualityGate\"",
              "\"captionMustMatchNarration\":true",
              "\"referenceGovernance\"",
              "\"apollo_technical_quality\"",
              "Você se arruma | Faça o diagnóstico gratuito");
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** Reconhece pós-produção completa cuja fonte auditável é uma tomada Product UGC. */
  @Test
  void shouldApproveCommerciallyCompleteProductUgcPostProduction() {
    long profileId = 10L;
    SalesVideoProfile profile =
        SalesVideoProfile.builder()
            .id(profileId)
            .humanReviewApprovedAt(Instant.parse("2026-09-04T12:00:00Z"))
            .build();
    SalesVideoScript script = SalesVideoScript.builder().ctaText("Faça o diagnóstico").build();
    SalesVideoJob ugc =
        SalesVideoJob.builder().id(20524L).providerName("RUNWAY_PRODUCT_UGC").build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(20525L)
            .profile(profile)
            .script(script)
            .retryOfJob(ugc)
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("MUSA_POST_PRODUCTION")
            .status(SalesVideoStatus.VIDEO_READY)
            .streamPlaybackUrl("https://cdn.example.com/vega-91.m3u8")
            .vttAsset(Asset.builder().id(904L).build())
            .metadataJson(
                "{\"audio\":{\"voice_over\":true,\"language\":\"pt-BR\","
                    + "\"review\":{\"status\":\"APPROVED_FOR_TEST\"}},"
                    + "\"captions\":{\"burned_in\":true,\"vtt_asset\":true},"
                    + "\"caption_narration_sync\":{\"status\":\"APPROVED\","
                    + "\"timing_status\":\"APPROVED\"},"
                    + "\"apollo_technical_quality\":{\"stability_status\":\"APPROVED\"}}")
            .build();
    given(profileRepository.findById(profileId)).willReturn(Optional.of(profile));
    given(jobRepository.findByProfileIdOrderByRequestedAtDesc(profileId)).willReturn(List.of(job));

    SalesVideoJobDto result = service.listJobsByProfile(profileId).get(0);

    assertThat(result.getCommercialReadinessStatus()).isEqualTo("READY");
    assertThat(result.getCommercialReadinessBlockers()).isEmpty();
  }

  /** Bloqueia Product UGC finalizado quando as três causas de rejeição não foram medidas. */
  @Test
  void shouldBlockProductUgcWithoutPremiumQualityEvidence() {
    long profileId = 10L;
    SalesVideoProfile profile =
        SalesVideoProfile.builder()
            .id(profileId)
            .humanReviewApprovedAt(Instant.parse("2026-09-04T12:00:00Z"))
            .build();
    SalesVideoScript script = SalesVideoScript.builder().ctaText("Faça o diagnóstico").build();
    SalesVideoJob ugc =
        SalesVideoJob.builder().id(20526L).providerName("RUNWAY_PRODUCT_UGC").build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(20527L)
            .profile(profile)
            .script(script)
            .retryOfJob(ugc)
            .jobType(SalesVideoJobType.POST_PRODUCTION)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("MUSA_POST_PRODUCTION")
            .status(SalesVideoStatus.VIDEO_READY)
            .streamPlaybackUrl("https://cdn.example.com/vega-91.m3u8")
            .vttAsset(Asset.builder().id(905L).build())
            .metadataJson(
                "{\"audio\":{\"voice_over\":true,\"language\":\"pt-BR\"},"
                    + "\"captions\":{\"burned_in\":true,\"vtt_asset\":true}}")
            .build();
    given(profileRepository.findById(profileId)).willReturn(Optional.of(profile));
    given(jobRepository.findByProfileIdOrderByRequestedAtDesc(profileId)).willReturn(List.of(job));

    SalesVideoJobDto result = service.listJobsByProfile(profileId).get(0);

    assertThat(result.getCommercialReadinessStatus()).isEqualTo("BLOCKED");
    assertThat(result.getCommercialReadinessBlockers())
        .containsExactly(
            "Apolo ainda não aprovou a estabilidade da tomada contínua.",
            "Texto, locução e tempo de exibição ainda não possuem sincronismo aprovado.",
            "O áudio premium ainda não passou no gate técnico para teste comercial.");
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
    assertThat(costCalculator.estimateUsd("RUNWAY_GEN_4_TURBO", "gen4_turbo", 10, "720p"))
        .isEqualByComparingTo(new BigDecimal("0.5000"));
    assertThat(costCalculator.estimateUsd("RUNWAY_VEO_3_1_FAST", "veo3.1_fast", 8, "720p"))
        .isEqualByComparingTo(new BigDecimal("1.2000"));
    assertThat(costCalculator.estimateUsd("RUNWAY_VEO_3_1", "veo3.1", 8, "720p"))
        .isEqualByComparingTo(new BigDecimal("3.2000"));
    assertThat(costCalculator.estimateUsd("RUNWAY_SEEDANCE_2", "seedance2", 10, "480p"))
        .isEqualByComparingTo(new BigDecimal("2.0000"));
    assertThat(costCalculator.estimateUsd("RUNWAY_SEEDANCE_2", "seedance2", 10, "720p"))
        .isEqualByComparingTo(new BigDecimal("3.0000"));
    assertThat(costCalculator.estimateUsd("RUNWAY_HAILUO_3", "hailuo3", 10, "768p"))
        .isEqualByComparingTo(new BigDecimal("1.0000"));
    assertThat(costCalculator.estimateUsd("RUNWAY_HAILUO_3", "hailuo3", 10, "2K"))
        .isEqualByComparingTo(new BigDecimal("1.5000"));
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

  /** Usa a duração contratada da receita Product UGC em vez do alvo legado do perfil. */
  @Test
  void shouldAcceptProductUgcUsingPinnedRecipeDuration() {
    SalesVideoProfile profile =
        SalesVideoProfile.builder()
            .id(57L)
            .targetDurationSeconds(22)
            .status(SalesVideoStatus.VIDEO_PROCESSING)
            .build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(21232L)
            .profile(profile)
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("RUNWAY_PRODUCT_UGC")
            .status(SalesVideoStatus.VIDEO_PROCESSING)
            .failureCode("RENDER_DURATION_SHORT")
            .failureDetail("Contrato legado comparou o render com 22 segundos.")
            .metadataJson(
                "{\"generation_strategy\":\"RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION\","
                    + "\"targetDurationSeconds\":15}")
            .build();
    JobCompletionRequest request = new JobCompletionRequest();
    request.setStatus(SalesVideoStatus.VIDEO_READY);
    request.setMetadataJson("{\"duration_seconds\":15,\"resolution\":\"1080x1920\"}");
    given(jobRepository.findById(21232L)).willReturn(Optional.of(job));
    given(jobRepository.save(job)).willReturn(job);

    SalesVideoJobDto result = service.complete(21232L, request);

    assertThat(result.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_READY);
    assertThat(job.getFailureCode()).isNull();
    assertThat(job.getFailureDetail()).isNull();
    verify(completedRenderAssetSync)
        .syncCompletedRender(
            org.mockito.Mockito.eq(job),
            org.mockito.Mockito.eq(request),
            org.mockito.Mockito.eq(15),
            org.mockito.Mockito.eq("1080x1920"));
  }

  /** Restaura no job filho a duração e a política de cortes já auditadas no Product UGC. */
  @Test
  void shouldPreserveAuditedProductUgcContractForPostProduction() throws Exception {
    TenantContextHolder.set(new TenantContext("tenant-a", "seller@example.com", false));
    SalesVideoProfile profile =
        SalesVideoProfile.builder()
            .id(57L)
            .tenantId("tenant-a")
            .status(SalesVideoStatus.VIDEO_READY)
            .build();
    SalesVideoJob sourceJob =
        SalesVideoJob.builder()
            .id(21232L)
            .profile(profile)
            .tenantId("tenant-a")
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .providerName("RUNWAY_PRODUCT_UGC")
            .executionMode(SalesVideoExecutionMode.TEST)
            .status(SalesVideoStatus.VIDEO_READY)
            .streamPlaybackUrl("https://cdn.example.com/vega-91-raw.mp4")
            .metadataJson(
                """
                {
                  "experimentId":91,
                  "generation_strategy":"RUNWAY_PRODUCT_UGC_WITH_DETERMINISTIC_POST_PRODUCTION",
                  "targetDurationSeconds":15,
                  "sceneCount":1,
                  "assemblyRequired":false,
                  "technicalQualityGate":{
                    "continuousTakeRequired":true,
                    "maximumMeanMotionDelta":1.25,
                    "maximumPeakMotionDelta":12.0
                  },
                  "apollo_technical_quality":{
                    "stability_status":"APPROVED",
                    "continuous_take":false,
                    "intentional_scene_cuts_allowed":true,
                    "maximum_scene_cuts":4,
                    "method":"FFMPEG_SCENE_AWARE_VIDSTAB_GLOBAL_MOTION_DELTA"
                  }
                }
                """)
            .build();
    RequestSalesVideoPostProductionRequest request = new RequestSalesVideoPostProductionRequest();
    request.setRequestedBy("operator@tenant.io");
    request.setCaptionText("Faça o diagnóstico gratuito.");
    given(jobRepository.findById(21232L)).willReturn(Optional.of(sourceJob));
    given(jobRepository.findByIdForUpdate(21232L)).willReturn(Optional.of(sourceJob));
    given(jobRepository.save(any(SalesVideoJob.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(eventRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    try {
      SalesVideoJobDto result = service.requestPostProduction(21232L, request);
      JsonNode metadata = new ObjectMapper().readTree(result.getMetadataJson());

      assertThat(metadata.path("targetDurationSeconds").asInt()).isEqualTo(15);
      assertThat(metadata.path("technicalQualityGate").path("continuousTakeRequired").asBoolean())
          .isFalse();
      assertThat(
              metadata.path("technicalQualityGate").path("intentionalSceneCutsAllowed").asBoolean())
          .isTrue();
      assertThat(metadata.path("technicalQualityGate").path("maximumSceneCuts").asInt())
          .isEqualTo(4);
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** Aceita clipe isolado quando atende à duração da cena, sem compará-lo ao vídeo final. */
  @Test
  void shouldAcceptIsolatedSceneUsingSceneDurationTarget() {
    SalesVideoProfile profile =
        SalesVideoProfile.builder()
            .id(6L)
            .targetDurationSeconds(30)
            .status(SalesVideoStatus.VIDEO_REQUESTED)
            .build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(20501L)
            .profile(profile)
            .jobType(SalesVideoJobType.RENDER)
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .status(SalesVideoStatus.VIDEO_PROCESSING)
            .metadataJson(
                """
                {
                  "generation_strategy":"SCENE_BY_SCENE_MONTAGE",
                  "scene":{"role":"MECANISMO","duration_seconds":10},
                  "provider_strategy":{"expected_clip_duration_seconds":10}
                }
                """)
            .build();
    JobCompletionRequest request = new JobCompletionRequest();
    request.setStatus(SalesVideoStatus.VIDEO_READY);
    request.setMetadataJson(
        "{\"duration_seconds\":10,\"resolution\":\"720p\",\"scene\":{\"role\":\"PROVIDER_RESULT\"}}");
    request.setMessage("Cena isolada concluída");
    given(jobRepository.findById(20501L)).willReturn(Optional.of(job));
    given(jobRepository.save(job)).willReturn(job);

    SalesVideoJobDto result = service.complete(20501L, request);

    assertThat(result.getStatus()).isEqualTo(SalesVideoStatus.VIDEO_READY);
    assertThat(job.getFailureCode()).isNull();
    assertThat(result.getMetadataJson())
        .contains("\"generation_strategy\":\"SCENE_BY_SCENE_MONTAGE\"")
        .contains("\"role\":\"MECANISMO\"")
        .contains("\"duration_seconds\":10")
        .contains("\"resolution\":\"720p\"");
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

  /** Preserva origem e copy, mas corrige disclosure editorial que inventava apresentadora. */
  @Test
  void shouldRequestPostProductionFromReadyVideoJob() throws Exception {
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
            .metadataJson(
                "{\"videoProductionCycleId\":7,\"videoProjectId\":3,\"experimentId\":91,"
                    + "\"generation_strategy\":\"DETERMINISTIC_EDITORIAL_MOTION_FROM_APPROVED_PRODUCT_PROOF\","
                    + "\"cut_plan\":[{\"role\":\"HOOK_DOR\"}],"
                    + "\"referenceGovernance\":{\"presenterIsSynthetic\":true,\"productIsDigitalExperience\":true},"
                    + "\"post_production\":{\"cta_text\":\"Ver meu plano\"}}")
            .requestedAt(Instant.parse("2026-07-24T10:00:00Z"))
            .build();
    RequestSalesVideoPostProductionRequest request = new RequestSalesVideoPostProductionRequest();
    request.setRequestedBy("operator@tenant.io");
    request.setVoiceOverScript("Você se arruma e sente que falta presença.");
    request.setCaptionText("Veja seu plano MUSA de 7 dias.");

    given(jobRepository.findById(20432L)).willReturn(Optional.of(sourceJob));
    given(jobRepository.findByIdForUpdate(20432L)).willReturn(Optional.of(sourceJob));
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
      assertThat(result.getMetadataJson())
          .contains(
              "\"videoProductionCycleId\":7",
              "\"videoProjectId\":3",
              "\"experimentId\":91",
              "\"cut_plan\"");
      assertThat(
              new ObjectMapper()
                  .readTree(result.getMetadataJson())
                  .at("/referenceGovernance/presenterIsSynthetic")
                  .asBoolean())
          .isFalse();
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

  /** Bloqueia variações repetidas da mesma cena na montagem narrativa do Estúdio. */
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

    assertThat(ex.getMessage()).contains("planos consecutivos do mesmo projeto");
  }

  /** Usa o quadro final persistido do plano anterior como abertura auditável do próximo. */
  @Test
  void shouldEnrichNextSceneWithPreviousFinalFrame() {
    TenantContextHolder.set(new TenantContext("tenant-a", "editor@tenant.io", false));
    SalesVideoProfile profile = SalesVideoProfile.builder().id(6L).tenantId("tenant-a").build();
    Asset continuityFrame =
        Asset.builder().id(901L).url("https://cdn.example.com/scene-1-final-frame.png").build();
    SalesVideoJob source =
        SalesVideoJob.builder()
            .id(501L)
            .profile(profile)
            .tenantId("tenant-a")
            .status(SalesVideoStatus.VIDEO_READY)
            .posterAsset(continuityFrame)
            .build();
    given(jobRepository.findById(501L)).willReturn(Optional.of(source));

    try {
      String enriched =
          service.enrichContinuityBridge("{\"image_to_video\":{\"enabled\":true}}", 501L, 6L);

      assertThat(enriched)
          .contains("PREVIOUS_SCENE_FINAL_FRAME")
          .contains("scene-1-final-frame.png")
          .contains("LAST_FRAME_TO_FIRST_FRAME")
          .contains("\"source_job_id\":501");
    } finally {
      TenantContextHolder.clear();
    }
  }
}
