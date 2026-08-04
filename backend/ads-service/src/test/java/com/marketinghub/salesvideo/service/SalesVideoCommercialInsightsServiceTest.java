package com.marketinghub.salesvideo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetProviderReviewProjection;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoCommercialPlaybookRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoConversionEventRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoScriptRepository;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.CreateSalesVideoCommercialPlaybookRequest;
import com.marketinghub.salesvideo.dto.CreateSalesVideoConversionEventRequest;
import com.marketinghub.salesvideo.dto.SalesVideoPerformanceSummaryDto;
import com.marketinghub.salesvideo.dto.SalesVideoProviderScoreDto;
import com.marketinghub.salesvideo.tenant.TenantContext;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Valida os aprendizados comerciais e a pontuação de providers do módulo SalesVideo. */
@ExtendWith(MockitoExtension.class)
class SalesVideoCommercialInsightsServiceTest {

  @Mock private SalesVideoProfileRepository profileRepository;
  @Mock private SalesVideoJobRepository jobRepository;
  @Mock private SalesVideoScriptRepository scriptRepository;
  @Mock private SalesVideoCommercialPlaybookRepository playbookRepository;
  @Mock private SalesVideoConversionEventRepository conversionEventRepository;
  @Mock private ExperimentVideoAssetRepository experimentVideoAssetRepository;

  private SalesVideoCommercialInsightsService service;

  /** Inicializa o service com repositórios simulados para cada cenário de teste. */
  @BeforeEach
  void setUp() {
    service =
        new SalesVideoCommercialInsightsService(
            profileRepository,
            jobRepository,
            scriptRepository,
            playbookRepository,
            conversionEventRepository,
            experimentVideoAssetRepository);
  }

  /** Deve persistir o Brief Cinematico PDE junto do playbook comercial do perfil. */
  @Test
  void shouldCreatePlaybookWithCinematicBriefFields() {
    SalesVideoProfile profile = profile();
    CreateSalesVideoCommercialPlaybookRequest request =
        new CreateSalesVideoCommercialPlaybookRequest();
    request.setNicheKey("moda");
    request.setVariantKey("hero-pde");
    request.setObjectionText("não tenho tempo");
    request.setCtaText("começar diagnóstico");
    request.setFunnelRole("landing");
    request.setPromiseToVisualize("ver uma missão personalizada no celular");
    request.setVisualPain("olhar o guarda-roupa e travar");
    request.setMainScene("cliente abre o PDE antes de sair");
    request.setSubjectDescription("mulher com celular e interface do produto");
    request.setMotionDescription("toca na missão do dia e separa uma peça");
    request.setCameraFraming("close no celular e plano médio da reação");
    request.setLightingStyle("luz natural suave");
    request.setExpectedEmotion("alívio");
    request.setTransitionOrCta("ir para a primeira missão");
    request.setQualityConstraints("sem texto pequeno ilegível");
    request.setCinematicPrompt("mobile-first commercial PDE scene");

    given(profileRepository.findById(7L)).willReturn(Optional.of(profile));
    given(playbookRepository.save(any(SalesVideoCommercialPlaybook.class)))
        .willAnswer(
            invocation -> {
              SalesVideoCommercialPlaybook playbook = invocation.getArgument(0);
              playbook.setId(88L);
              return playbook;
            });

    var response = service.createPlaybook(7L, request);

    ArgumentCaptor<SalesVideoCommercialPlaybook> captor =
        ArgumentCaptor.forClass(SalesVideoCommercialPlaybook.class);
    verify(playbookRepository).save(captor.capture());
    SalesVideoCommercialPlaybook saved = captor.getValue();
    assertThat(response.getId()).isEqualTo(88L);
    assertThat(response.getFunnelRole()).isEqualTo("landing");
    assertThat(response.getPromiseToVisualize()).contains("missão personalizada");
    assertThat(saved.getMainScene()).isEqualTo("cliente abre o PDE antes de sair");
    assertThat(saved.getCameraFraming()).contains("close no celular");
    assertThat(saved.getCinematicPrompt()).isEqualTo("mobile-first commercial PDE scene");
  }

  /** Deve consolidar conversão usando o script associado ao job informado. */
  @Test
  void shouldCreateConversionEventBindingScriptFromJob() {
    SalesVideoProfile profile = profile();
    SalesVideoScript script = SalesVideoScript.builder().id(77L).profile(profile).build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(11L)
            .profile(profile)
            .tenantId("tenant-a")
            .script(script)
            .providerName("video-management-service")
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .jobType(SalesVideoJobType.RENDER)
            .status(SalesVideoStatus.VIDEO_READY)
            .build();
    CreateSalesVideoConversionEventRequest request = new CreateSalesVideoConversionEventRequest();
    request.setJobId(11L);
    request.setEventType(SalesVideoConversionEventType.PURCHASE);
    request.setEventValue(new BigDecimal("197.90"));

    given(profileRepository.findById(7L)).willReturn(Optional.of(profile));
    given(jobRepository.findById(11L)).willReturn(Optional.of(job));
    given(conversionEventRepository.save(any(SalesVideoConversionEvent.class)))
        .willAnswer(
            invocation -> {
              SalesVideoConversionEvent event = invocation.getArgument(0);
              event.setId(900L);
              return event;
            });

    var response = service.createConversionEvent(7L, request);

    assertThat(response.getId()).isEqualTo(900L);
    assertThat(response.getScriptId()).isEqualTo(77L);
    assertThat(response.getEventType()).isEqualTo(SalesVideoConversionEventType.PURCHASE);
    assertThat(response.getEventValue()).isEqualByComparingTo("197.90");
  }

  /** Deve resumir receita, conversões e reputação do provider por script. */
  @Test
  void shouldSummarizePerformanceByScriptAndProvider() {
    SalesVideoProfile profile = profile();
    SalesVideoScript script = SalesVideoScript.builder().id(3L).profile(profile).build();
    SalesVideoJob job =
        SalesVideoJob.builder()
            .id(21L)
            .profile(profile)
            .script(script)
            .providerName("provider-real")
            .tenantId("tenant-a")
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .jobType(SalesVideoJobType.RENDER)
            .status(SalesVideoStatus.VIDEO_READY)
            .build();
    SalesVideoConversionEvent lead =
        SalesVideoConversionEvent.builder()
            .id(1L)
            .profile(profile)
            .job(job)
            .script(script)
            .tenantId("tenant-a")
            .eventType(SalesVideoConversionEventType.LEAD)
            .occurredAt(Instant.parse("2026-04-17T10:00:00Z"))
            .build();
    SalesVideoConversionEvent purchase =
        SalesVideoConversionEvent.builder()
            .id(2L)
            .profile(profile)
            .job(job)
            .script(script)
            .tenantId("tenant-a")
            .eventType(SalesVideoConversionEventType.PURCHASE)
            .eventValue(new BigDecimal("399.00"))
            .occurredAt(Instant.parse("2026-04-17T10:30:00Z"))
            .build();

    given(profileRepository.findById(7L)).willReturn(Optional.of(profile));
    given(conversionEventRepository.findByProfileIdAndTenantIdOrderByOccurredAtDesc(7L, "tenant-a"))
        .willReturn(List.of(purchase, lead));
    given(playbookRepository.findByProfileIdAndTenantIdOrderByCreatedAtDesc(7L, "tenant-a"))
        .willReturn(List.of());
    given(jobRepository.findByProfileIdOrderByRequestedAtDesc(7L)).willReturn(List.of(job));
    given(experimentVideoAssetRepository.findProviderReviewsBySalesVideoProfileId(7L))
        .willReturn(List.of(providerReview("provider-real", "READY", "APPROVED")));

    SalesVideoPerformanceSummaryDto response = service.summarizePerformance(7L, null, null);

    assertThat(response.getTotalEvents()).isEqualTo(2);
    assertThat(response.getTotalLeads()).isEqualTo(1);
    assertThat(response.getTotalPurchases()).isEqualTo(1);
    assertThat(response.getTotalRevenue()).isEqualByComparingTo("399.00");
    assertThat(response.getVariants()).hasSize(1);
    assertThat(response.getVariants().get(0).getScriptId()).isEqualTo(3L);
    assertThat(response.getVariants().get(0).getProviderName()).isEqualTo("provider-real");
    assertThat(response.getProviderScores()).hasSize(1);
    assertThat(response.getProviderScores().get(0).getProviderName()).isEqualTo("provider-real");
    assertThat(response.getProviderScores().get(0).getScore()).isEqualTo(100);
    assertThat(response.getProviderScores().get(0).getRecommendation()).isEqualTo("priorizar");
  }

  /**
   * Deve reduzir a pontuação sem bloquear provider quando ainda precisamos acumular aprendizado.
   */
  @Test
  void shouldPenalizeRejectedProviderAssets() {
    SalesVideoProfile profile = profile();
    SalesVideoJob failedJob =
        SalesVideoJob.builder()
            .id(22L)
            .profile(profile)
            .providerName("LUMA_RAY_3_2")
            .tenantId("tenant-a")
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .jobType(SalesVideoJobType.RENDER)
            .status(SalesVideoStatus.VIDEO_FAILED)
            .build();

    given(profileRepository.findById(7L)).willReturn(Optional.of(profile));
    given(conversionEventRepository.findByProfileIdAndTenantIdOrderByOccurredAtDesc(7L, "tenant-a"))
        .willReturn(List.of());
    given(playbookRepository.findByProfileIdAndTenantIdOrderByCreatedAtDesc(7L, "tenant-a"))
        .willReturn(List.of());
    given(jobRepository.findByProfileIdOrderByRequestedAtDesc(7L)).willReturn(List.of(failedJob));
    given(experimentVideoAssetRepository.findProviderReviewsBySalesVideoProfileId(7L))
        .willReturn(List.of(providerReview("LUMA_RAY_3_2", "READY", "REJECTED")));

    SalesVideoPerformanceSummaryDto response = service.summarizePerformance(7L, null, null);

    assertThat(response.getProviderScores()).hasSize(1);
    assertThat(response.getProviderScores().get(0).getScore()).isEqualTo(13);
    assertThat(response.getProviderScores().get(0).getRejectedAssets()).isEqualTo(1);
    assertThat(response.getProviderScores().get(0).getRiskCategory())
        .isEqualTo("REPROVACAO_CRIATIVA");
    assertThat(response.getProviderScores().get(0).getRecommendation())
        .isEqualTo("usar_com_cautela");
    assertThat(response.getProviderScores().get(0).getRiskMessage())
        .contains("manter teste controlado");
  }

  /** Deve separar falha operacional de configuração da reprovação criativa do provider. */
  @Test
  void shouldAllowControlledTestAfterOnlyOperationalProviderConfigurationFailure() {
    SalesVideoProfile profile = profile();
    SalesVideoJob failedJob =
        SalesVideoJob.builder()
            .id(20473L)
            .profile(profile)
            .providerName("RUNWAY")
            .tenantId("tenant-a")
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .jobType(SalesVideoJobType.RENDER)
            .status(SalesVideoStatus.VIDEO_FAILED)
            .failureCode("VIDEO_PROVIDER_ERROR")
            .failureDetail(
                "retryable=false;code=VIDEO_PROVIDER_ERROR;message=Nenhum provider configurado para o job")
            .build();

    given(jobRepository.findByTenantIdOrderByRequestedAtDesc("tenant-a"))
        .willReturn(List.of(failedJob));
    given(experimentVideoAssetRepository.findProviderReviewsByTenantId("tenant-a"))
        .willReturn(List.of());
    given(conversionEventRepository.findByTenantIdOrderByOccurredAtDesc("tenant-a"))
        .willReturn(List.of());

    TenantContextHolder.set(new TenantContext("tenant-a", "seller@example.com", false));
    try {
      List<SalesVideoProviderScoreDto> response = service.summarizeProviderScores();

      assertThat(response).hasSize(1);
      assertThat(response.get(0).getProviderName()).isEqualTo("RUNWAY");
      assertThat(response.get(0).getFailedJobs()).isEqualTo(1);
      assertThat(response.get(0).getOperationalFailedJobs()).isEqualTo(1);
      assertThat(response.get(0).getRejectedAssets()).isZero();
      assertThat(response.get(0).getRecommendation()).isEqualTo("testar_controlado");
      assertThat(response.get(0).getRiskCategory()).isEqualTo("FALHA_OPERACIONAL_CONFIGURACAO");
      assertThat(response.get(0).getRiskMessage()).contains("liberar teste controlado");
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** Deve resumir score global dos providers usando o tenant corrente. */
  @Test
  void shouldSummarizeGlobalProviderScoresForTenant() {
    SalesVideoProfile profile = profile();
    SalesVideoJob readyJob =
        SalesVideoJob.builder()
            .id(31L)
            .profile(profile)
            .providerName("HEYGEN")
            .tenantId("tenant-a")
            .providerFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE)
            .jobType(SalesVideoJobType.RENDER)
            .status(SalesVideoStatus.VIDEO_READY)
            .build();
    SalesVideoConversionEvent purchase =
        SalesVideoConversionEvent.builder()
            .id(40L)
            .profile(profile)
            .job(readyJob)
            .tenantId("tenant-a")
            .eventType(SalesVideoConversionEventType.PURCHASE)
            .eventValue(new BigDecimal("67.00"))
            .occurredAt(Instant.parse("2026-07-25T10:00:00Z"))
            .build();

    given(jobRepository.findByTenantIdOrderByRequestedAtDesc("tenant-a"))
        .willReturn(List.of(readyJob));
    given(experimentVideoAssetRepository.findProviderReviewsByTenantId("tenant-a"))
        .willReturn(List.of(providerReview("HEYGEN", "READY", "APPROVED")));
    given(conversionEventRepository.findByTenantIdOrderByOccurredAtDesc("tenant-a"))
        .willReturn(List.of(purchase));

    TenantContextHolder.set(new TenantContext("tenant-a", "seller@example.com", false));
    try {
      var response = service.summarizeProviderScores();

      assertThat(response).hasSize(1);
      assertThat(response.get(0).getProviderName()).isEqualTo("HEYGEN");
      assertThat(response.get(0).getScore()).isEqualTo(98);
      assertThat(response.get(0).getPurchases()).isEqualTo(1);
      assertThat(response.get(0).getRevenue()).isEqualByComparingTo("67.00");
      assertThat(response.get(0).getRecommendation()).isEqualTo("priorizar");
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** Cria uma projeção mínima de avaliação de provider para os testes de reputação. */
  private static ProviderReview providerReview(
      String provider, String status, String reviewStatus) {
    return new ProviderReview(provider, status, reviewStatus);
  }

  /** Cria um perfil base de SalesVideo para os testes comerciais. */
  private static SalesVideoProfile profile() {
    return SalesVideoProfile.builder()
        .id(7L)
        .tenantId("tenant-a")
        .title("Avatar Hero")
        .videoKind(SalesVideoKind.HERO)
        .status(SalesVideoStatus.SCRIPT_READY)
        .product(Product.builder().id(99L).build())
        .build();
  }

  /** Implementa a projeção mínima retornada pelo repositório de vídeos de experimento. */
  private record ProviderReview(String provider, String status, String reviewStatus)
      implements ExperimentVideoAssetProviderReviewProjection {
    /** Retorna o provider projetado para o teste. */
    @Override
    public String getProvider() {
      return provider;
    }

    /** Retorna o status funcional projetado para o teste. */
    @Override
    public String getStatus() {
      return status;
    }

    /** Retorna o status de revisão projetado para o teste. */
    @Override
    public String getReviewStatus() {
      return reviewStatus;
    }
  }
}
