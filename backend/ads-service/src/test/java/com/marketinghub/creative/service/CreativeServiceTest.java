package com.marketinghub.creative.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marketinghub.FixtureUtils;
import com.marketinghub.ads.AdsServiceApplication;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.creative.CreativeVideoReviewSourceType;
import com.marketinghub.creative.convergence.v1.ConvergenceCycleStatus;
import com.marketinghub.creative.convergence.v1.ConvergenceTaskStatus;
import com.marketinghub.creative.dto.AssetUploadResponse;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.dto.CreativeAgentReviewResultRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanStatus;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.creative.convergence.CreativeConvergenceCycleRepository;
import com.marketinghub.repository.jpa.creative.convergence.CreativeConvergenceTaskRepository;
import com.marketinghub.repository.jpa.creative.label.AngleRepository;
import com.marketinghub.repository.jpa.creative.label.EmotionalTriggerRepository;
import com.marketinghub.repository.jpa.creative.label.VisualProofRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadCategory;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest(classes = AdsServiceApplication.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
      "spring.datasource.driverClassName=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create",
      "spring.liquibase.enabled=false"
    })
class CreativeServiceTest {

  @Autowired CreativeRepository repository;
  @Autowired ExperimentRepository experimentRepository;
  @Autowired AngleRepository angleRepository;
  @Autowired VisualProofRepository visualProofRepository;
  @Autowired EmotionalTriggerRepository emotionalTriggerRepository;
  @Autowired FixtureUtils fixtures;
  @Autowired AssetRepository assetRepository;
  @Autowired ExperimentVideoAssetRepository experimentVideoAssetRepository;
  @Autowired CreativeConvergenceCycleRepository convergenceCycleRepository;
  @Autowired CreativeConvergenceTaskRepository convergenceTaskRepository;
  @Autowired CommercialPlanRepository commercialPlanRepository;
  @Autowired CommercialPlanVisualAssetRepository commercialPlanVisualAssetRepository;

  @Autowired CreativeService service;

  @MockBean HttpClient httpClient;

  @MockBean AssetStorageService assetStorageService;

  @BeforeEach
  void setup() {
    experimentVideoAssetRepository.deleteAll();
    assetRepository.deleteAll();
    commercialPlanVisualAssetRepository.deleteAll();
    commercialPlanRepository.deleteAll();
  }

  @Test
  void uploadImageReturnsPath() throws Exception {
    MultipartFile file =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "test.png", "image/png", new byte[] {1, 2});
    AssetStorageService.StoredObject stored =
        new AssetStorageService.StoredObject(
            "experiments/exp-1/test.png",
            "https://cdn.test/assets/test.png",
            file.getSize(),
            "image/png",
            true);
    when(assetStorageService.store(any(), any())).thenReturn(stored);

    AssetUploadResponse response =
        service.uploadImage(
            file,
            "dall-e-3",
            "prompt text",
            "intermediate prompt",
            AssetUploadCategory.EXPERIMENT_CREATIVE,
            1L,
            null,
            "slug-test");

    assertThat(response.url()).isEqualTo("https://cdn.test/assets/test.png");
    Asset saved = assetRepository.findAll().stream().findFirst().orElseThrow();
    assertThat(saved.getUrl()).isEqualTo("https://cdn.test/assets/test.png");
    assertThat(saved.getExternalId()).isEqualTo("experiments/exp-1/test.png");
    assertThat(saved.getType()).isEqualTo(AssetType.IMAGE);
    assertThat(saved.getStatus()).isEqualTo(AssetStatus.READY);
    assertThat(saved.getModel()).isEqualTo("dall-e-3");
    assertThat(saved.getPrompt()).isEqualTo("prompt text");
    assertThat(saved.getPromptIntermediate()).isEqualTo("intermediate prompt");
    assertThat(saved.getPayload()).contains("EXPERIMENT_CREATIVE");
  }

  /** Recupera revisão órfã, preserva auditoria e entrega um novo lease ao worker. */
  @Test
  void recoversExpiredAgentReviewLease() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);
    Creative creative = fixtures.createAndSaveCreative(exp);
    creative.setAgentReviewStatus(CreativeAgentReviewStatus.PROCESSING);
    creative.setAgentReviewStartedAt(Instant.now().minusSeconds(60 * 60));
    repository.saveAndFlush(creative);

    var claimed = service.claimAgentReviewQueue(100);

    assertThat(claimed).anyMatch(job -> job.creativeId().equals(creative.getId()));
    Creative recovered = repository.findById(creative.getId()).orElseThrow();
    assertThat(recovered.getAgentReviewStatus()).isEqualTo(CreativeAgentReviewStatus.PROCESSING);
    assertThat(recovered.getAgentReviewRecoveryCount()).isEqualTo(1);
    assertThat(recovered.getAgentReviewLastRecoveredAt()).isNotNull();
    assertThat(recovered.getAgentReviewStartedAt()).isAfter(Instant.now().minusSeconds(10));
  }

  /** Encerra com causa persistida quando o lease excede o limite seguro de recuperações. */
  @Test
  void failsAgentReviewAfterRecoveryLimit() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);
    Creative creative = fixtures.createAndSaveCreative(exp);
    creative.setAgentReviewStatus(CreativeAgentReviewStatus.PROCESSING);
    creative.setAgentReviewStartedAt(Instant.now().minusSeconds(60 * 60));
    creative.setAgentReviewRecoveryCount(2);
    repository.saveAndFlush(creative);

    var claimed = service.claimAgentReviewQueue(100);

    assertThat(claimed).noneMatch(job -> job.creativeId().equals(creative.getId()));
    Creative failed = repository.findById(creative.getId()).orElseThrow();
    assertThat(failed.getAgentReviewStatus()).isEqualTo(CreativeAgentReviewStatus.FAILED);
    assertThat(failed.getAgentReviewRecoveryCount()).isEqualTo(3);
    assertThat(failed.getAgentReviewResponseJson()).contains("limite de recuperações");
    assertThat(failed.getAgentReviewedAt()).isNotNull();
  }

  @Test
  void previewParsesHtml() throws Exception {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);
    fixtures.createAndSaveCreative(exp);

    HttpResponse<String> resp = mock(HttpResponse.class);
    when(resp.body()).thenReturn("{\"data\":[{\"body\":\"<div>ok</div>\"}]}");
    when(httpClient.send(any(), any())).thenReturn((HttpResponse) resp);
    System.setProperty("FB_ACCESS_TOKEN", "dummy");
    try {
      String html = service.preview(1L);
      assertThat(html).contains("ok");
    } finally {
      System.clearProperty("FB_ACCESS_TOKEN");
    }
  }

  @Test
  void approvingCreativeMarksExperimentAsReady() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);

    CreateCreativeRequest createRequest = new CreateCreativeRequest();
    createRequest.setHeadline("Headline");
    createRequest.setPrimaryText("Primary");
    createRequest.setImageUrl("/img.png");
    createRequest.setStatus(CreativeStatus.DRAFT);
    Creative creative = service.create(exp.getId(), createRequest);

    Experiment afterCreate = experimentRepository.findById(exp.getId()).orElseThrow();
    assertThat(afterCreate.isCreativeApproved()).isFalse();

    CreateCreativeRequest approveRequest = new CreateCreativeRequest();
    approveRequest.setHeadline("Headline");
    approveRequest.setPrimaryText("Primary");
    approveRequest.setImageUrl("/img.png");
    approveRequest.setStatus(CreativeStatus.DRAFT);
    service.update(creative.getId(), approveRequest);
    approveByAgent(creative.getId());
    service.updateStatus(creative.getId(), CreativeStatus.READY);

    Experiment afterApproval = experimentRepository.findById(exp.getId()).orElseThrow();
    assertThat(afterApproval.isCreativeApproved()).isTrue();
  }

  /** Garante que criativo de vídeo aprovado por status libera o experimento para campanha. */
  @Test
  void approvingVideoCreativeByStatusMarksExperimentAsReady() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);

    CreateCreativeRequest createRequest = new CreateCreativeRequest();
    createRequest.setFormat("VIDEO");
    createRequest.setHeadline("Video");
    createRequest.setPrimaryText("Primary");
    createRequest.setVideoUrl("https://cdn.test/video.mp4");
    createRequest.setStatus(CreativeStatus.DRAFT);
    Creative creative = service.create(exp.getId(), createRequest);

    approveByAgent(creative.getId());
    service.updateStatus(creative.getId(), CreativeStatus.READY);

    Experiment afterApproval = experimentRepository.findById(exp.getId()).orElseThrow();
    assertThat(afterApproval.isCreativeApproved()).isTrue();
  }

  /**
   * Garante que reprovação de vídeo exija motivo e mantenha o experimento bloqueado para campanha.
   */
  @Test
  void rejectingVideoCreativeByStatusRequiresReasonAndKeepsExperimentBlocked() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);

    CreateCreativeRequest createRequest = new CreateCreativeRequest();
    createRequest.setFormat("VIDEO");
    createRequest.setHeadline("Video");
    createRequest.setPrimaryText("Primary");
    createRequest.setVideoUrl("https://cdn.test/video.mp4");
    createRequest.setStatus(CreativeStatus.DRAFT);
    Creative creative = service.create(exp.getId(), createRequest);

    assertThatThrownBy(() -> service.updateStatus(creative.getId(), CreativeStatus.REJECTED, " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("motivo da reprovação");

    Creative rejected =
        service.updateStatus(
            creative.getId(),
            CreativeStatus.REJECTED,
            "Avatar não comunica sofisticação suficiente.");

    assertThat(rejected.getStatus()).isEqualTo(CreativeStatus.REJECTED);
    assertThat(rejected.getRejectionReason())
        .isEqualTo("Avatar não comunica sofisticação suficiente.");
    Experiment afterRejection = experimentRepository.findById(exp.getId()).orElseThrow();
    assertThat(afterRejection.isCreativeApproved()).isFalse();
  }

  /** Garante que a fila de revisão traga apenas criativos de vídeo com mídia publicável. */
  @Test
  void listVideoReviewQueueReturnsVideoCreativesWithCommercialContext() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);

    CreateCreativeRequest videoRequest = new CreateCreativeRequest();
    videoRequest.setFormat("VIDEO");
    videoRequest.setHeadline("Video");
    videoRequest.setPrimaryText("Primary");
    videoRequest.setVideoUrl("https://cdn.test/video.mp4");
    videoRequest.setStatus(CreativeStatus.DRAFT);
    Creative videoCreative = service.create(exp.getId(), videoRequest);

    CreateCreativeRequest imageRequest = new CreateCreativeRequest();
    imageRequest.setFormat("IMAGE");
    imageRequest.setHeadline("Image");
    imageRequest.setPrimaryText("Primary");
    imageRequest.setImageUrl("https://cdn.test/image.png");
    imageRequest.setStatus(CreativeStatus.DRAFT);
    service.create(exp.getId(), imageRequest);

    var queue = service.listVideoReviewQueue(CreativeStatus.DRAFT);

    assertThat(queue).hasSize(1);
    assertThat(queue.get(0).id()).isEqualTo(videoCreative.getId());
    assertThat(queue.get(0).experimentId()).isEqualTo(exp.getId());
    assertThat(queue.get(0).nicheName()).isEqualTo(niche.getName());
  }

  /** Garante que a fila humana bloqueia campanha e hero com a mesma origem visual sem exceção. */
  @Test
  void approvingExperimentVideoReviewBlocksRepeatedVisualSourceAcrossAdAndHero() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);
    ExperimentVideoAsset heroVideo =
        experimentVideoAssetRepository.save(
            ExperimentVideoAsset.builder()
                .experiment(exp)
                .slot(ExperimentVideoSlot.LANDING_HERO)
                .objective("Explicar PDE")
                .primaryMetric("diagnostico_iniciado")
                .script("Hero explicativo")
                .provider("HEYGEN")
                .model("avatar-iv")
                .status(ExperimentVideoStatus.READY)
                .assetUrl("https://cdn.test/hero.mp4")
                .hasAudio(true)
                .visualSourceKey("sofia-musa")
                .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
                .requiredForRelease(true)
                .build());
    ExperimentVideoAsset adVideo =
        experimentVideoAssetRepository.save(
            ExperimentVideoAsset.builder()
                .experiment(exp)
                .slot(ExperimentVideoSlot.AD)
                .objective("Validar criativo")
                .primaryMetric("ctr")
                .script("Anuncio curto")
                .provider("HEYGEN")
                .model("avatar-iv")
                .status(ExperimentVideoStatus.READY)
                .assetUrl("https://cdn.test/ad.mp4")
                .hasAudio(true)
                .visualSourceKey("sofia-musa")
                .reviewStatus(ExperimentVideoReviewStatus.PENDING)
                .requiredForRelease(true)
                .build());

    assertThat(heroVideo.getId()).isNotNull();
    assertThatThrownBy(
            () ->
                service.updateVideoReviewStatus(
                    CreativeVideoReviewSourceType.EXPERIMENT_VIDEO_ASSET,
                    adVideo.getId(),
                    CreativeStatus.READY,
                    null))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("mesma origem visual");
  }

  /** Garante que criativo de imagem não possa ser aprovado sem imagem gerada. */
  @Test
  void shouldRejectReadyImageCreativeWithoutImageUrl() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);

    CreateCreativeRequest createRequest = new CreateCreativeRequest();
    createRequest.setHeadline("Headline");
    createRequest.setPrimaryText("Primary");
    createRequest.setFormat("IMAGE");
    createRequest.setStatus(CreativeStatus.READY);

    assertThatThrownBy(() -> service.create(exp.getId(), createRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("precisa ter imagem");

    Experiment afterAttempt = experimentRepository.findById(exp.getId()).orElseThrow();
    assertThat(afterAttempt.isCreativeApproved()).isFalse();
  }

  /** Garante que CTAs livres longos não quebram o limite físico da coluna. */
  @Test
  void normalizesLongCallToActionBeforePersisting() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);

    CreateCreativeRequest createRequest = new CreateCreativeRequest();
    createRequest.setHeadline("Headline");
    createRequest.setPrimaryText("Primary");
    createRequest.setImageUrl("/img.png");
    createRequest.setCta("Gerar minha amostra personalizada");
    createRequest.setStatus(CreativeStatus.DRAFT);

    Creative creative = service.create(exp.getId(), createRequest);

    assertThat(creative.getCta()).isEqualTo("LEARN_MORE");
  }

  @Test
  void deletingLastApprovedCreativeResetsFlag() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);

    CreateCreativeRequest createRequest = new CreateCreativeRequest();
    createRequest.setHeadline("Headline");
    createRequest.setPrimaryText("Primary");
    createRequest.setImageUrl("/img.png");
    createRequest.setStatus(CreativeStatus.DRAFT);
    Creative creative = service.create(exp.getId(), createRequest);
    approveByAgent(creative.getId());
    service.updateStatus(creative.getId(), CreativeStatus.READY);

    Experiment afterApproval = experimentRepository.findById(exp.getId()).orElseThrow();
    assertThat(afterApproval.isCreativeApproved()).isTrue();

    service.delete(creative.getId());

    Experiment afterDelete = experimentRepository.findById(exp.getId()).orElseThrow();
    assertThat(afterDelete.isCreativeApproved()).isFalse();
  }

  /** Garante que nenhuma aprovação humana contorne o parecer multimodal obrigatório. */
  @Test
  void blocksHumanApprovalUntilAgentApproves() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);
    CreateCreativeRequest request = new CreateCreativeRequest();
    request.setFormat("IMAGE");
    request.setImageUrl("https://cdn.test/ad.png");
    request.setStatus(CreativeStatus.DRAFT);
    Creative creative = service.create(exp.getId(), request);

    assertThat(creative.getAgentReviewStatus()).isEqualTo(CreativeAgentReviewStatus.PENDING);
    assertThatThrownBy(() -> service.updateStatus(creative.getId(), CreativeStatus.READY))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("Agente Especialista");

    approveByAgent(creative.getId());
    Creative approved = service.updateStatus(creative.getId(), CreativeStatus.READY);
    assertThat(approved.getStatus()).isEqualTo(CreativeStatus.READY);
  }

  /** Preserva requisitos verificáveis do parecer na fila de correção consumida pelo worker. */
  @Test
  void schedulesImprovementWithStructuredVisualContract() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);
    CreateCreativeRequest create = new CreateCreativeRequest();
    create.setFormat("IMAGE");
    create.setImageUrl("https://cdn.test/ad.png");
    Creative creative = service.create(exp.getId(), create);
    CommercialPlan plan = new CommercialPlan();
    plan.setName("Plano do experimento");
    plan.setStatus(CommercialPlanStatus.IN_PROGRESS);
    plan.setExperiment(exp);
    plan = commercialPlanRepository.save(plan);
    CommercialPlanVisualAsset reference = new CommercialPlanVisualAsset();
    reference.setCommercialPlan(plan);
    reference.setAssetUrl("https://cdn.test/product-proof.png");
    reference.setMediaType("IMAGE");
    reference.setLabel("Prova real do kit");
    reference.setPurpose("ADS");
    reference.setOrigin("Entrega homologada");
    reference.setRightsStatement("Uso comercial autorizado");
    reference.setVersionNumber(1);
    reference.setStatus(CommercialPlanVisualAssetStatus.APPROVED);
    commercialPlanVisualAssetRepository.save(reference);

    service.applyAgentReview(
        creative.getId(),
        adjustmentReview(
            java.util.List.of("Headline Agenda Cheia legível", "CTA Saiba mais legível"),
            java.util.List.of("Texto simulado"),
            java.util.List.of("Headline legível em mobile")));

    var pending = service.claimAgentImprovementQueue(3);
    assertThat(pending).hasSize(1);
    assertThat(pending.getFirst().mandatoryVisualRequirements())
        .containsExactly("Headline Agenda Cheia legível", "CTA Saiba mais legível");
    assertThat(pending.getFirst().forbiddenVisualElements()).containsExactly("Texto simulado");
    assertThat(pending.getFirst().visualAcceptanceCriteria())
        .containsExactly("Headline legível em mobile");
    assertThat(pending.getFirst().referenceImageUrls())
        .containsExactly("https://cdn.test/product-proof.png");
  }

  /** Bloqueia correção vaga antes de consumir geração visual. */
  @Test
  void rejectsImprovementWithoutVerifiableVisualContract() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);
    CreateCreativeRequest create = new CreateCreativeRequest();
    create.setFormat("IMAGE");
    create.setImageUrl("https://cdn.test/ad.png");
    Creative creative = service.create(exp.getId(), create);

    Creative reviewed =
        service.applyAgentReview(
            creative.getId(),
            adjustmentReview(java.util.List.of(), java.util.List.of(), java.util.List.of()));

    assertThat(reviewed.getAgentImprovementStatus())
        .isEqualTo(com.marketinghub.creative.CreativeImprovementStatus.FAILED);
    assertThat(reviewed.getAgentImprovementError()).contains("critérios visuais verificáveis");
  }

  /** Persiste tarefas verificáveis e bloqueia o ciclo quando a mesma falha não progride. */
  @Test
  void coordinatesAndStopsRepeatedConvergenceIssue() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);
    CreateCreativeRequest create = new CreateCreativeRequest();
    create.setFormat("IMAGE");
    create.setImageUrl("https://cdn.test/ad.png");
    Creative creative = service.create(exp.getId(), create);
    CreativeAgentReviewResultRequest review =
        adjustmentReview(
            java.util.List.of("Produto legível"),
            java.util.List.of("Texto simulado"),
            java.util.List.of("Legível em mobile"));

    service.applyAgentReview(creative.getId(), review);
    service.applyAgentReview(creative.getId(), review);
    service.applyAgentReview(creative.getId(), review);

    var cycle =
        convergenceCycleRepository.findAll().stream()
            .filter(item -> item.getRootCreativeId().equals(creative.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(cycle.getStatus()).isEqualTo(ConvergenceCycleStatus.BLOCKED_NO_PROGRESS);
    assertThat(cycle.getRepeatedIssueCount()).isEqualTo(2);
    assertThat(convergenceTaskRepository.findByCycleIdOrderByIdAsc(cycle.getId()))
        .extracting(task -> task.getStatus())
        .containsExactly(
            ConvergenceTaskStatus.PENDING,
            ConvergenceTaskStatus.REPEATED,
            ConvergenceTaskStatus.REPEATED);
  }

  /** Bloqueia copy corrigida que seria truncada nos placements Meta. */
  @Test
  void rejectsAgentCorrectionAboveMetaDisplayLimits() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);
    CreateCreativeRequest create = new CreateCreativeRequest();
    create.setFormat("IMAGE");
    create.setImageUrl("https://cdn.test/ad.png");
    Creative creative = service.create(exp.getId(), create);
    CreativeAgentReviewResultRequest valid =
        adjustmentReview(
            java.util.List.of("Produto legível"),
            java.util.List.of("Texto simulado"),
            java.util.List.of("Legível em mobile"));
    CreativeAgentReviewResultRequest oversized =
        new CreativeAgentReviewResultRequest(
            valid.decision(),
            valid.attentionScore(),
            valid.clarityScore(),
            valid.desireScore(),
            valid.credibilityScore(),
            valid.actionScore(),
            valid.copyAssessment(),
            valid.commercialAestheticAssessment(),
            valid.destinationIntegrationAssessment(),
            valid.summary(),
            valid.issuesJson(),
            valid.recommendationsJson(),
            valid.model(),
            valid.requestJson(),
            valid.responseJson(),
            valid.inputTokens(),
            valid.outputTokens(),
            valid.costUsd(),
            valid.error(),
            valid.revisedHeadline(),
            "x".repeat(126),
            valid.revisedDescription(),
            valid.revisedCta(),
            valid.revisedImagePrompt(),
            valid.mandatoryVisualRequirements(),
            valid.forbiddenVisualElements(),
            valid.visualAcceptanceCriteria(),
            valid.correctionTargets());

    assertThatThrownBy(() -> service.applyAgentReview(creative.getId(), oversized))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("texto principal: 125");
  }

  /** Monta um parecer de ajuste completo para os testes do contrato de correção. */
  private CreativeAgentReviewResultRequest adjustmentReview(
      java.util.List<String> mandatory,
      java.util.List<String> forbidden,
      java.util.List<String> acceptance) {
    return new CreativeAgentReviewResultRequest(
        CreativeAgentReviewStatus.ADJUST,
        60,
        60,
        60,
        60,
        60,
        "Copy precisa de benefício específico.",
        "Hierarquia visual insuficiente.",
        "Promessa ainda não coincide com a landing.",
        "Ajustar hierarquia visual.",
        "[\"Headline ilegível\"]",
        "[\"Aumentar contraste\"]",
        "gpt-test",
        "{}",
        "{}",
        10,
        10,
        java.math.BigDecimal.ZERO,
        null,
        "Agenda Cheia",
        "Preencha sua agenda",
        "Método prático",
        "LEARN_MORE",
        "Crie uma arte premium para manicures.",
        mandatory,
        forbidden,
        acceptance,
        java.util.List.of(
            new CreativeAgentReviewResultRequest.ConvergenceCorrectionTarget(
                "CREATIVE_MEDIA",
                "PRODUCT_DEMONSTRATION",
                "Mostrar o produto digital de forma legível.",
                "A imagem deve mostrar post e story legíveis em mobile.")));
  }

  /** Registra um parecer multimodal aprovado para cenários que exercitam a aprovação humana. */
  private void approveByAgent(Long creativeId) {
    service.applyAgentReview(
        creativeId,
        new CreativeAgentReviewResultRequest(
            CreativeAgentReviewStatus.APPROVED,
            80,
            80,
            80,
            80,
            80,
            "Copy específica e persuasiva.",
            "Design comercial premium.",
            "Anúncio e landing apresentam a mesma promessa e ação.",
            "Peça pronta para revisão humana.",
            "[]",
            "[]",
            "gpt-test",
            "{}",
            "{}",
            10,
            10,
            java.math.BigDecimal.ZERO,
            null,
            "Peça pronta para revisão humana.",
            "Texto aprovado.",
            "Descrição aprovada.",
            "LEARN_MORE",
            "",
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of()));
  }

  /** Garante que a correção crie outro registro e preserve o criativo original. */
  @Test
  void createVersionPreservesOriginalAndResetsCommercialGates() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment exp = fixtures.createAndSaveExperiment(niche);
    CreateCreativeRequest originalRequest = new CreateCreativeRequest();
    originalRequest.setFormat("IMAGE");
    originalRequest.setHeadline("Original");
    originalRequest.setPrimaryText("Texto original");
    originalRequest.setImageUrl("https://cdn.test/original.png");
    originalRequest.setStatus(CreativeStatus.DRAFT);
    Creative original = service.create(exp.getId(), originalRequest);

    CreateCreativeRequest revisionRequest = new CreateCreativeRequest();
    revisionRequest.setFormat("IMAGE");
    revisionRequest.setHeadline("Revisão");
    revisionRequest.setPrimaryText("Texto corrigido");
    revisionRequest.setImageUrl("https://cdn.test/revision.png");
    revisionRequest.setDestinationUrl("https://agenda-cheia.test/previa");
    revisionRequest.setStatus(CreativeStatus.READY);

    Creative revision = service.createVersion(original.getId(), revisionRequest);

    assertThat(revision.getId()).isNotEqualTo(original.getId());
    assertThat(revision.getSourceCreative().getId()).isEqualTo(original.getId());
    assertThat(revision.getVersionNumber()).isEqualTo(2);
    assertThat(revision.getStatus()).isEqualTo(CreativeStatus.DRAFT);
    assertThat(revision.getAgentReviewStatus()).isEqualTo(CreativeAgentReviewStatus.PENDING);
    assertThat(repository.findById(original.getId()).orElseThrow().getHeadline())
        .isEqualTo("Original");
  }

  /** Garante reutilização auditável no mesmo produto sem herdar aprovações comerciais. */
  @Test
  void reuseInExperimentPreservesSourceAndResetsGates() {
    MarketNiche niche = fixtures.createAndSaveNiche();
    Experiment sourceExperiment = fixtures.createAndSaveExperiment(niche);
    Experiment targetExperiment = fixtures.createAndSaveExperiment(niche);
    CreateCreativeRequest request = new CreateCreativeRequest();
    request.setFormat("IMAGE");
    request.setHeadline("Agenda cheia");
    request.setImageUrl("https://cdn.test/agenda.png");
    Creative source = service.create(sourceExperiment.getId(), request);
    approveByAgent(source.getId());
    service.updateStatus(source.getId(), CreativeStatus.READY);

    Creative reused = service.reuseInExperiment(targetExperiment.getId(), source.getId());

    assertThat(reused.getExperiment().getId()).isEqualTo(targetExperiment.getId());
    assertThat(reused.getSourceCreative().getId()).isEqualTo(source.getId());
    assertThat(reused.getStatus()).isEqualTo(CreativeStatus.DRAFT);
    assertThat(reused.getAgentReviewStatus()).isEqualTo(CreativeAgentReviewStatus.PENDING);
  }

  /** Impede que anúncios de outro produto/nicho sejam associados ao experimento. */
  @Test
  void reuseInExperimentRejectsCreativeFromAnotherNiche() {
    MarketNiche sourceNiche = fixtures.createAndSaveNiche();
    MarketNiche targetNiche = fixtures.createAndSaveNiche();
    Experiment sourceExperiment = fixtures.createAndSaveExperiment(sourceNiche);
    Experiment targetExperiment = fixtures.createAndSaveExperiment(targetNiche);
    Creative source = fixtures.createAndSaveCreative(sourceExperiment);

    assertThatThrownBy(() -> service.reuseInExperiment(targetExperiment.getId(), source.getId()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mesmo produto/nicho");
  }
}
