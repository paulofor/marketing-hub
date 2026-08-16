package com.marketinghub.planning.imagestudio.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioJob;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioOperation;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import com.marketinghub.storage.AssetStorageService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: proteger a produção visual governada entre backend e Têmis. */
class CommercialPlanImageStudioServiceTest {
  private CommercialPlanService planService;
  private CommercialPlanImageStudioJobRepository jobRepository;
  private CommercialPlanVisualAssetRepository visualAssetRepository;
  private CommercialPlanImageStudioService service;
  private CreativeService creativeService;

  /** Inicializa mocks isolados sem storage ou banco reais. */
  @BeforeEach
  void setUp() {
    planService = mock(CommercialPlanService.class);
    jobRepository = mock(CommercialPlanImageStudioJobRepository.class);
    visualAssetRepository = mock(CommercialPlanVisualAssetRepository.class);
    creativeService = mock(CreativeService.class);
    service =
        new CommercialPlanImageStudioService(
            planService,
            jobRepository,
            visualAssetRepository,
            mock(AssetStorageService.class),
            mock(AssetRepository.class),
            new ObjectMapper(),
            creativeService);
    when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  /** Exige que toda produção seja um entregável antes de poder servir landing ou anúncio. */
  @Test
  void requiresDeliveryPurpose() {
    when(planService.getPlan(2L)).thenReturn(plan(2L));

    assertThatThrownBy(
            () ->
                service.create(
                    2L,
                    new CreateCommercialPlanImageStudioJobRequest(
                        CommercialPlanImageStudioOperation.CREATE,
                        null,
                        List.of(),
                        "Produzir post premium",
                        "Post principal",
                        List.of("ADS"),
                        "1024x1536",
                        "high")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("DELIVERY");
  }

  /** Preserva origem e referências do próprio plano numa edição não destrutiva. */
  @Test
  void createsEditWithSourceAndReusablePurposes() {
    CommercialPlan plan = plan(2L);
    CommercialPlanVisualAsset source = asset(10L, plan, "/assets/source.png");
    when(planService.getPlan(2L)).thenReturn(plan);
    when(visualAssetRepository.findById(10L)).thenReturn(Optional.of(source));

    CommercialPlanImageStudioJobDto result =
        service.create(
            2L,
            new CreateCommercialPlanImageStudioJobRequest(
                CommercialPlanImageStudioOperation.EDIT,
                10L,
                List.of(10L),
                "Ajustar contraste sem redesenhar o produto",
                "Post principal premium",
                List.of("DELIVERY", "LANDING", "ADS", "SOCIAL"),
                "2048x2048",
                "high"));

    assertThat(result.operation()).isEqualTo(CommercialPlanImageStudioOperation.EDIT);
    assertThat(result.sourceAssetId()).isEqualTo(10L);
    assertThat(result.purposes()).containsExactly("DELIVERY", "LANDING", "ADS", "SOCIAL");
    ArgumentCaptor<CommercialPlanImageStudioJob> captor =
        ArgumentCaptor.forClass(CommercialPlanImageStudioJob.class);
    verify(jobRepository).save(captor.capture());
    assertThat(captor.getValue().getReferenceAssetIdsJson()).isEqualTo("[10]");
  }

  /** Bloqueia referência auxiliar ainda não aprovada para preservar a fonte premium do produto. */
  @Test
  void rejectsDraftCompositionReference() {
    CommercialPlan plan = plan(2L);
    CommercialPlanVisualAsset draftReference = asset(11L, plan, "/assets/draft.png");
    when(planService.getPlan(2L)).thenReturn(plan);
    when(visualAssetRepository.findById(11L)).thenReturn(Optional.of(draftReference));

    assertThatThrownBy(
            () ->
                service.create(
                    2L,
                    new CreateCommercialPlanImageStudioJobRequest(
                        CommercialPlanImageStudioOperation.CREATE,
                        null,
                        List.of(11L),
                        "Compor material premium",
                        "Post premium",
                        List.of("DELIVERY", "ADS"),
                        "1024x1536",
                        "high")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Referência visual inválida");
  }

  /** Recupera uma lease vencida e cria novo identificador para invalidar callback antigo. */
  @Test
  void reclaimsExpiredProductionLease() {
    CommercialPlanImageStudioJob job = job(21L, plan(2L), "producer-old");
    job.setStatus(CommercialPlanImageStudioStatus.PROCESSING);
    job.setStartedAt(Instant.now().minusSeconds(4_000));
    when(jobRepository.findClaimable(any(), any(), any())).thenReturn(List.of(job));

    CommercialPlanImageStudioPendingDto claimed = service.claimPending(1).getFirst();

    assertThat(claimed.producerExecutionId()).isNotBlank().isNotEqualTo("producer-old");
    assertThat(job.getStartedAt()).isAfter(Instant.now().minusSeconds(10));
  }

  /** Falha uma edição cuja origem foi aposentada antes do consumo sem chamar o modelo. */
  @Test
  void failsQueuedEditWhenSourceWasRetired() {
    CommercialPlan plan = plan(2L);
    CommercialPlanVisualAsset source = asset(12L, plan, "/assets/retired.png");
    source.setStatus(CommercialPlanVisualAssetStatus.RETIRED);
    CommercialPlanImageStudioJob job = job(22L, plan, null);
    job.setOperation(CommercialPlanImageStudioOperation.EDIT);
    job.setSourceVisualAsset(source);
    job.setReferenceAssetIdsJson("[12]");
    when(jobRepository.findClaimable(any(), any(), any())).thenReturn(List.of(job));
    when(visualAssetRepository.findById(12L)).thenReturn(Optional.of(source));

    assertThat(service.claimPending(1)).isEmpty();
    assertThat(job.getStatus()).isEqualTo(CommercialPlanImageStudioStatus.FAILED);
    assertThat(job.getError()).contains("sem aprovação vigente");
  }

  /** Impede autoaprovação e permite que uma segunda execução aprove o entregável. */
  @Test
  void requiresIndependentReviewExecution() {
    CommercialPlan plan = plan(2L);
    CommercialPlanVisualAsset asset = asset(31L, plan, "/assets/result.png");
    asset.setPurposesJson("[\"DELIVERY\",\"ADS\"]");
    asset.setAgentReviewStatus(CommercialPlanVisualAssetReviewStatus.PROCESSING);
    CommercialPlanImageStudioJob job = job(41L, plan, "producer-1");
    job.setResultVisualAsset(asset);
    when(visualAssetRepository.findById(31L)).thenReturn(Optional.of(asset));
    when(jobRepository.findByResultVisualAssetId(31L)).thenReturn(Optional.of(job));

    assertThatThrownBy(
            () ->
                service.review(
                    31L,
                    new CommercialPlanVisualAssetReviewResultRequest(
                        CommercialPlanVisualAssetReviewStatus.APPROVED,
                        "producer-1",
                        "Premium",
                        "{}",
                        "{}",
                        null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("diferente");

    service.review(
        31L,
        new CommercialPlanVisualAssetReviewResultRequest(
            CommercialPlanVisualAssetReviewStatus.APPROVED,
            "reviewer-2",
            "Produto fiel, premium e reutilizável",
            "{}",
            "{}",
            null));

    assertThat(asset.getStatus()).isEqualTo(CommercialPlanVisualAssetStatus.APPROVED);
    assertThat(asset.getReviewerExecutionId()).isEqualTo("reviewer-2");
  }

  /** Reabre o retrabalho do criativo com a causa objetiva quando a Biblioteca pede ajuste. */
  @Test
  void requeuesCreativeAfterIndependentAdjustment() {
    CommercialPlan plan = plan(2L);
    CommercialPlanVisualAsset asset = asset(32L, plan, "/assets/result-adjust.png");
    asset.setPurposesJson("[\"DELIVERY\",\"ADS\"]");
    asset.setAgentReviewStatus(CommercialPlanVisualAssetReviewStatus.PROCESSING);
    CommercialPlanImageStudioJob job = job(42L, plan, "producer-1");
    Creative creative = new Creative();
    creative.setId(55L);
    job.setSourceCreative(creative);
    job.setResultVisualAsset(asset);
    when(visualAssetRepository.findById(32L)).thenReturn(Optional.of(asset));
    when(jobRepository.findByResultVisualAssetId(32L)).thenReturn(Optional.of(job));

    service.review(
        32L,
        new CommercialPlanVisualAssetReviewResultRequest(
            CommercialPlanVisualAssetReviewStatus.ADJUST,
            "reviewer-2",
            "Aumentar legibilidade mobile sem alterar o produto",
            "{}",
            "{}",
            null));

    verify(creativeService)
        .requeueLibraryImprovement(55L, "Aumentar legibilidade mobile sem alterar o produto");
    assertThat(asset.getStatus()).isEqualTo(CommercialPlanVisualAssetStatus.DRAFT);
  }

  /** Impede retrabalho sem causa objetiva, evitando consumir nova geração às cegas. */
  @Test
  void rejectsAdjustmentWithoutFunctionalSummary() {
    CommercialPlan plan = plan(2L);
    CommercialPlanVisualAsset asset = asset(33L, plan, "/assets/result-adjust.png");
    CommercialPlanImageStudioJob job = job(43L, plan, "producer-1");
    job.setResultVisualAsset(asset);
    when(visualAssetRepository.findById(33L)).thenReturn(Optional.of(asset));
    when(jobRepository.findByResultVisualAssetId(33L)).thenReturn(Optional.of(job));

    assertThatThrownBy(
            () ->
                service.review(
                    33L,
                    new CommercialPlanVisualAssetReviewResultRequest(
                        CommercialPlanVisualAssetReviewStatus.ADJUST,
                        "reviewer-2",
                        " ",
                        "{}",
                        "{}",
                        null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("parecer funcional");
  }

  /** Cria um plano mínimo segregado para os contratos do estúdio. */
  private CommercialPlan plan(Long id) {
    CommercialPlan plan = new CommercialPlan();
    plan.setId(id);
    plan.setName("Agenda Cheia");
    plan.setMainOffer("Kit digital premium");
    plan.setTargetAudience("Nail designers");
    return plan;
  }

  /** Cria um asset de imagem do plano informado. */
  private CommercialPlanVisualAsset asset(Long id, CommercialPlan plan, String url) {
    CommercialPlanVisualAsset asset = new CommercialPlanVisualAsset();
    asset.setId(id);
    asset.setCommercialPlan(plan);
    asset.setAssetUrl(url);
    asset.setMediaType("IMAGE");
    asset.setStatus(CommercialPlanVisualAssetStatus.DRAFT);
    asset.setVersionNumber(1);
    return asset;
  }

  /** Cria um job reservado com contrato mínimo para revisão ou retomada. */
  private CommercialPlanImageStudioJob job(Long id, CommercialPlan plan, String producer) {
    CommercialPlanImageStudioJob job = new CommercialPlanImageStudioJob();
    job.setId(id);
    job.setCommercialPlan(plan);
    job.setOperation(CommercialPlanImageStudioOperation.CREATE);
    job.setStatus(CommercialPlanImageStudioStatus.PROCESSING);
    job.setPrompt("Produzir entrega premium");
    job.setLabel("Entrega premium");
    job.setPurposesJson("[\"DELIVERY\"]");
    job.setReferenceAssetIdsJson("[]");
    job.setSize("1024x1536");
    job.setQuality("high");
    job.setProducerExecutionId(producer);
    return job;
  }
}
