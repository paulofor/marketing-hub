package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.CreativeFinalVideoEvidence;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.creative.*;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.video.*;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: proteger o gate de vídeo contra troca de contexto, aprovação e mídia. */
class LearningCycleVideoEvidenceTest {
  final ObjectMapper mapper = new ObjectMapper();
  final LearningCycleJson json = new LearningCycleJson(mapper);
  final ExperimentVideoAssetRepository videos = mock(ExperimentVideoAssetRepository.class);
  final CreativeRepository creatives = mock(CreativeRepository.class);
  final PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
  final LearningSalesCycleEventRepository events = mock(LearningSalesCycleEventRepository.class);
  private final CreativeFinalVideoEvidence finalReview =
      new CreativeFinalVideoEvidence(
          videos,
          mock(com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository.class),
          mock(com.marketinghub.repository.jpa.agenttask.AgentTaskRepository.class),
          mapper);
  final LearningCycleVideoEvidence service =
      new LearningCycleVideoEvidence(videos, creatives, slots, events, json, finalReview);
  final LearningSalesCycle cycle = new LearningSalesCycle();
  final List<LearningSalesCycleEvent> history = new ArrayList<>();
  Experiment experiment;
  ExperimentVideoAsset ad, hero;
  Creative creative;
  PdeProductionSlot slot;

  /** Prepara peças distintas e independentes, sem acessar qualquer provider ou banco produtivo. */
  @BeforeEach
  void setup() {
    var product = new Product();
    product.setId(4L);
    product.setSlug("musa");
    experiment = new Experiment();
    experiment.setId(92L);
    experiment.setProduct(product);
    experiment.setFollowUpActionUrl("https://fixture.invalid/v8");
    cycle.setId(1L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setProductVersion("v8");
    cycle.setVersionChangedAt(Instant.parse("2026-09-08T00:00:00Z"));
    when(events.findByCycleIdOrderByRevisionAsc(1L)).thenReturn(history);
    ad = video(10L, ExperimentVideoSlot.AD);
    hero = video(11L, ExperimentVideoSlot.LANDING_HERO);
    creative = new Creative();
    creative.setId(20L);
    creative.setExperiment(experiment);
    creative.setStatus(CreativeStatus.READY);
    creative.setAgentReviewStatus(CreativeAgentReviewStatus.APPROVED);
    creative.setVideoUrl(ad.getAssetUrl());
    creative.setDestinationUrl(experiment.getFollowUpActionUrl());
    creative.setHeadline("Teste local");
    when(creatives.findById(20L)).thenReturn(Optional.of(creative));
    slot = new PdeProductionSlot();
    slot.setId(30L);
    slot.setProductSlug("musa");
    slot.setExperienceVersion("v8");
    slot.setSourceExperimentId(92L);
    slot.setPublicUrl(experiment.getFollowUpActionUrl());
    slot.setDraftExperienceJson(
        json.write(
            Map.of(
                "heroVideos",
                List.of(
                    Map.of(
                        "experimentVideoAssetId",
                        11,
                        "hlsPlaybackUrl",
                        hero.getHlsPlaybackUrl(),
                        "status",
                        "READY",
                        "reviewStatus",
                        "APPROVED")))));
    slot.setPublishedExperienceJson(slot.getDraftExperienceJson());
    when(slots.findById(30L)).thenReturn(Optional.of(slot));
  }

  /** Cria um ativo consultável com mídia e finalidade próprias. */
  private ExperimentVideoAsset video(Long id, ExperimentVideoSlot role) {
    var value = new ExperimentVideoAsset();
    value.setId(id);
    value.setExperiment(experiment);
    value.setSlot(role);
    value.setStatus(ExperimentVideoStatus.READY);
    value.setReviewStatus(ExperimentVideoReviewStatus.APPROVED);
    value.setHasAudio(true);
    value.setDurationSeconds(30);
    value.setAssetUrl("https://fixture.invalid/" + id + ".mp4");
    value.setHlsPlaybackUrl("https://fixture.invalid/" + id + ".m3u8");
    when(videos.findById(id)).thenReturn(Optional.of(value));
    return value;
  }

  /** Persiste somente no double uma decisão anterior pertencente à revisão atual. */
  private void event(String stage, ObjectNode proof) {
    var event = new LearningSalesCycleEvent();
    event.setFromStage(stage);
    event.setAction("COMPLETE");
    event.setCreatedAt(cycle.getVersionChangedAt().plusSeconds(1));
    event.setEvidenceJson(json.write(proof));
    history.add(event);
  }

  /** Registra as duas produções usando as assinaturas calculadas pelo próprio backend. */
  private void produced() {
    var proof =
        mapper
            .createObjectNode()
            .put("campaignVideoAssetId", ad.getId())
            .put("productionEvidence", "Estúdio local");
    service.production(cycle, proof, ExperimentVideoSlot.AD);
    event("CAMPAIGN_VIDEO", proof);
    proof =
        mapper
            .createObjectNode()
            .put("pdeVideoAssetId", hero.getId())
            .put("productionEvidence", "Estúdio local");
    service.production(cycle, proof, ExperimentVideoSlot.LANDING_HERO);
    event("PDE_ENTRY_VIDEO", proof);
  }

  /** Declara conferências humanas sem substituir as aprovações lidas nos ativos e criativo. */
  private ObjectNode integration() {
    return mapper
        .createObjectNode()
        .put("creativeId", 20)
        .put("pdeSlotId", 30)
        .put("technicalEvidence", "Relatório local")
        .put("customerReviewEvidence", "Referência independente")
        .put("captionsVerified", true)
        .put("mobileVerified", true)
        .put("optionalPlaybackVerified", true)
        .put("testDataExcluded", true);
  }

  /** Exige briefing explícito para as duas funções comerciais. */
  @Test
  void briefNeedsSeparateGoals() {
    var data = mapper.createObjectNode();
    assertThrows(ResponseStatusException.class, () -> service.brief(data));
    for (String key :
        List.of(
            "briefReference",
            "campaignGoal",
            "campaignCta",
            "campaignMetric",
            "pdeGoal",
            "pdeCta",
            "pdeMetric",
            "controlledVariables",
            "productionBudgetReference")) data.put(key, "Referência " + key);
    assertDoesNotThrow(() -> service.brief(data));
  }

  /** Aceita o percurso completo e a revalidação da mesma mídia depois da integração. */
  @Test
  void acceptsTwoVideosAndRevalidatesPublication() {
    produced();
    var data = integration();
    service.integration(cycle, data);
    event("VIDEO_APPROVAL", data);
    assertDoesNotThrow(() -> service.current(cycle, false));
    assertDoesNotThrow(() -> service.current(cycle, true));
    assertTrue(data.path("integrationFingerprint").asText().matches("[a-f0-9]{64}"));
  }

  /** Rejeita cada forma relevante de divergência sem concluir a atividade. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "rejected",
        "missing",
        "wrong-role",
        "other-experiment",
        "other-product",
        "silent",
        "url",
        "hls",
        "changed-media",
        "draft-creative",
        "agent-review",
        "creative-destination",
        "wrong-version",
        "wrong-slot-product",
        "wrong-slot-experiment",
        "missing-contract",
        "wrong-hero",
        "wrong-contract-version",
        "invalid-json",
        "missing-product-slug",
        "same-video",
        "old-evidence",
        "unverified-mobile"
      })
  void refusesInvalidIntegration(String defect) {
    produced();
    var data = integration();
    switch (defect) {
      case "rejected" -> ad.setReviewStatus(ExperimentVideoReviewStatus.REJECTED);
      case "missing" -> when(videos.findById(10L)).thenReturn(Optional.empty());
      case "wrong-role" -> ad.setSlot(ExperimentVideoSlot.LANDING_HERO);
      case "other-experiment" -> {
        var other = new Experiment();
        other.setId(91L);
        ad.setExperiment(other);
      }
      case "other-product" -> experiment.getProduct().setId(5L);
      case "silent" -> ad.setHasAudio(false);
      case "url" -> ad.setAssetUrl("file:///tmp/video.mp4");
      case "hls" -> hero.setHlsPlaybackUrl(null);
      case "changed-media" -> ad.setAssetUrl("https://fixture.invalid/replaced.mp4");
      case "draft-creative" -> creative.setStatus(CreativeStatus.DRAFT);
      case "agent-review" -> creative.setAgentReviewStatus(CreativeAgentReviewStatus.REJECTED);
      case "creative-destination" -> creative.setDestinationUrl("https://other.invalid");
      case "wrong-version" -> slot.setExperienceVersion("v7");
      case "wrong-slot-product" -> slot.setProductSlug("mira");
      case "wrong-slot-experiment" -> slot.setSourceExperimentId(91L);
      case "missing-contract" -> slot.setDraftExperienceJson(null);
      case "wrong-contract-version" ->
          slot.setDraftExperienceJson("{\"experienceVersion\":\"v7\",\"heroVideos\":[]}");
      case "invalid-json" -> slot.setDraftExperienceJson("{");
      case "missing-product-slug" -> experiment.getProduct().setSlug(null);
      case "wrong-hero" -> slot.setDraftExperienceJson("{\"heroVideos\":[]}");
      case "same-video" -> hero.setAssetUrl(ad.getAssetUrl());
      case "old-evidence" -> cycle.setVersionChangedAt(Instant.parse("2026-09-08T01:00:00Z"));
      case "unverified-mobile" -> data.put("mobileVerified", false);
    }
    assertThrows(ResponseStatusException.class, () -> service.integration(cycle, data), defect);
  }

  /** Reprovação posterior bloqueia até a mídia voltar ao fluxo independente de revisão. */
  @Test
  void revokedApprovalBlocksCurrentAuthorization() {
    produced();
    var data = integration();
    service.integration(cycle, data);
    event("VIDEO_APPROVAL", data);
    hero.setReviewStatus(ExperimentVideoReviewStatus.REJECTED);
    assertNotNull(service.blocker(cycle, false));
  }

  /** Contrato preparado não comprova publicação e alterações posteriores invalidam a integração. */
  @Test
  void publicationMustMatchReviewedDraft() {
    produced();
    var data = integration();
    service.integration(cycle, data);
    event("VIDEO_APPROVAL", data);
    slot.setPublishedExperienceJson("{}");
    assertNotNull(service.blocker(cycle, true));
    slot.setPublishedExperienceJson(slot.getDraftExperienceJson());
    assertNull(service.blocker(cycle, true));
    creative.setHeadline("Promessa alterada");
    assertNotNull(service.blocker(cycle, true));
  }

  /**
   * Kits usam página auditada com o mesmo vídeo, sem inventar slot ou reutilizar mídia rejeitada.
   */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"7,105,76,75", "207,9105,976,975"})
  void validatesPublishedLowTicketVideoWithoutMembershipSlot(
      long productId, long experimentId, long adId, long heroId) {
    experiment
        .getProduct()
        .setProductTypeDefinition(
            com.marketinghub.producttype.ProductTypeDefinition.builder()
                .code("LOW_TICKET_DIGITAL_PRODUCT")
                .build());
    experiment.getProduct().setId(productId);
    experiment.setId(experimentId);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    ad.setId(adId);
    hero.setId(heroId);
    when(videos.findById(adId)).thenReturn(Optional.of(ad));
    when(videos.findById(heroId)).thenReturn(Optional.of(hero));
    produced();
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    var policy =
        mock(com.marketinghub.experiment.service.ExperimentCampaignDestinationPolicy.class);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "salesPageDestinations", policy);
    var publication = new com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit();
    publication.setExperimentId(experiment.getId());
    publication.setPublishedAt(Instant.now());
    publication.setSalesPageUrl(experiment.getFollowUpActionUrl());
    publication.setHtml(
        "<section data-mh-reviewed-video='v1'><video controls data-video-asset-id='"
            + heroId
            + "'><source src='"
            + hero.getAssetUrl()
            + "'></video></section>");
    when(policy.auditedSalesPagePublication(experiment)).thenReturn(Optional.of(publication));
    when(policy.hasCompletedGeraSalesPagePipeline(experiment)).thenReturn(true);
    when(creatives.findByExperimentIdAndVideoUrl(cycle.getExperimentId(), ad.getAssetUrl()))
        .thenReturn(List.of(creative));
    var proof = mapper.createObjectNode();
    for (var asset : List.of(ad, hero))
      proof
          .putObject(asset == ad ? "campaignVideo" : "heroVideo")
          .put("assetId", asset.getId())
          .put("assetUrl", asset.getAssetUrl())
          .put("hlsPlaybackUrl", asset.getHlsPlaybackUrl())
          .put("reviewStatus", "APPROVED");
    assertDoesNotThrow(
        () ->
            org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "publishedPrivateIntegration", cycle, proof));
    verify(slots, never()).findByProductSlugOrderBySlotCodeAsc(anyString());
    publication.setHtml(publication.getHtml().replace("controls", "controls autoplay"));
    assertThrows(
        ResponseStatusException.class,
        () ->
            org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "publishedPrivateIntegration", cycle, proof));
    publication.setHtml(
        publication
            .getHtml()
            .replace("controls autoplay", "controls")
            .replace(hero.getAssetUrl(), "https://fixture.invalid/wrong.mp4"));
    assertThrows(
        ResponseStatusException.class,
        () ->
            org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "publishedPrivateIntegration", cycle, proof));
  }

  /** PDE e Produto IA legados não viram kits por usarem o enum antigo do experimento. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"PDE", "AI_PRODUCT"})
  void legacyLowTicketPdePublicationDoesNotRequireSalesPage(String type) {
    produced();
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment
        .getProduct()
        .setProductTypeDefinition(
            com.marketinghub.producttype.ProductTypeDefinition.builder().code(type).build());
    var policy =
        mock(com.marketinghub.experiment.service.ExperimentCampaignDestinationPolicy.class);
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "salesPageDestinations", policy);
    slot.setStatus(com.marketinghub.pde.PdeProductionSlotStatus.ACTIVE);
    slot.setPublishedAt(Instant.now());
    when(slots.findByProductSlugOrderBySlotCodeAsc("musa")).thenReturn(List.of(slot));
    when(creatives.findByExperimentIdAndVideoUrl(cycle.getExperimentId(), ad.getAssetUrl()))
        .thenReturn(List.of(creative));
    var proof = mapper.createObjectNode();
    for (var asset : List.of(ad, hero))
      proof
          .putObject(asset == ad ? "campaignVideo" : "heroVideo")
          .put("assetId", asset.getId())
          .put("assetUrl", asset.getAssetUrl())
          .put("hlsPlaybackUrl", asset.getHlsPlaybackUrl())
          .put("reviewStatus", "APPROVED");
    assertDoesNotThrow(
        () ->
            org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "publishedPrivateIntegration", cycle, proof));
    verifyNoInteractions(policy);
  }

  /** Mantém a sequência antiga e acrescenta os vídeos somente ao BPM v2. */
  @Test
  void preservesOriginalBpmAndVideoOrder() {
    assertEquals("VALIDATION", LearningCycleRules.next("ADJUSTMENT", false));
    assertEquals("VIDEO_BRIEF", LearningCycleRules.next("ADJUSTMENT", true));
    assertEquals("CAMPAIGN_VIDEO", LearningCycleRules.next("VIDEO_BRIEF", true));
    assertEquals("PDE_ENTRY_VIDEO", LearningCycleRules.next("CAMPAIGN_VIDEO", true));
    assertEquals("VIDEO_APPROVAL", LearningCycleRules.next("PDE_ENTRY_VIDEO", true));
    assertEquals("VALIDATION", LearningCycleRules.next("VIDEO_APPROVAL", true));
  }

  /** Completa versão e layout de um contrato identificado, preservando os vídeos revisados. */
  @Test
  void acceptsCanonicalPublicationNormalization() {
    produced();
    slot.setSlotCode("v8");
    slot.setLayoutKey("video-explicativo");
    var draft = (ObjectNode) json.read(slot.getDraftExperienceJson());
    draft.put("slug", slot.getProductSlug());
    slot.setDraftExperienceJson(json.write(draft));
    var data = integration();
    service.integration(cycle, data);
    event("VIDEO_APPROVAL", data);
    when(slots.findByProductSlugAndSlotCode("musa", "v8")).thenReturn(Optional.of(slot));
    when(slots.save(any())).thenAnswer(call -> call.getArgument(0));
    var publication =
        new com.marketinghub.pde.service.PdeProductionSlotService(
            slots, videos, mock(java.net.http.HttpClient.class), mapper);
    publication.publishProductionSlotContract(
        "musa",
        "v8",
        new com.marketinghub.pde.service.publishslotcontract
            .PublishPdeProductionSlotContractRequest(null, "Homologação local"));
    var published = json.read(slot.getPublishedExperienceJson());
    assertEquals("musa", published.path("slug").asText());
    assertEquals("v8", published.path("experienceVersion").asText());
    assertEquals("video-explicativo", published.path("layoutKey").asText());
    assertEquals(draft.path("heroVideos"), published.path("heroVideos"));
    assertDoesNotThrow(() -> service.current(cycle, true));
  }
}
