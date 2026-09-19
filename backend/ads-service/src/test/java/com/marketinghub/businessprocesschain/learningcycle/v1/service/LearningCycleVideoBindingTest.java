package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.*;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Responsabilidade: comprovar integração privada sem publicação, duplicação de aprovação ou provas
 * antigas.
 */
class LearningCycleVideoBindingTest {
  final ObjectMapper json = new ObjectMapper();
  final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  final LearningSalesCycleEventRepository events = mock(LearningSalesCycleEventRepository.class);
  final ExperimentVideoAssetRepository videos = mock(ExperimentVideoAssetRepository.class);
  final CreativeRepository creatives = mock(CreativeRepository.class);
  final PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
  final LearningCycleVideoEvidence evidence =
      new LearningCycleVideoEvidence(videos, creatives, slots, events, new LearningCycleJson(json));
  final LearningCyclePrototypeContext prototypes = new LearningCyclePrototypeContext(events, json);
  final LearningCycleVideoBinding binding =
      new LearningCycleVideoBinding(cycles, events, evidence, prototypes, json);
  final List<LearningSalesCycleEvent> history = new ArrayList<>();
  final LearningSalesCycle cycle = new LearningSalesCycle();
  final Instant now = Instant.parse("2026-09-14T22:46:04Z");
  Experiment experiment;
  ExperimentVideoAsset ad, hero;

  /** Instala duas mídias e destino privados em um experimento sem slot ou URL comercial. */
  @BeforeEach
  void setup() {
    cycle.setId(91001L);
    cycle.setProductId(91004L);
    cycle.setExperimentId(91092L);
    cycle.setChainDefinitionId(91014L);
    cycle.setProductVersion("private-v12");
    cycle.setStatus("OPEN");
    cycle.setStage("VIDEO_APPROVAL");
    cycle.setVersionChangedAt(now.minusSeconds(100));
    when(cycles.findById(cycle.getId())).thenReturn(Optional.of(cycle));
    when(cycles.findByExperimentId(cycle.getExperimentId())).thenReturn(Optional.of(cycle));
    when(events.findByCycleIdOrderByRevisionAsc(cycle.getId())).thenReturn(history);
    var product = new Product();
    product.setId(cycle.getProductId());
    product.setSlug("fixture");
    experiment = new Experiment();
    experiment.setId(cycle.getExperimentId());
    experiment.setProduct(product);
    ad = media(91041L, experiment, ExperimentVideoSlot.AD, "a");
    hero = media(91042L, experiment, ExperimentVideoSlot.LANDING_HERO, "b");
    event(
        "ADJUSTMENT",
        "REWORK",
        json.createObjectNode()
            .put("productVersion", cycle.getProductVersion())
            .set(
                "privatePrototype",
                json.createObjectNode()
                    .put("prototypeVersion", cycle.getProductVersion())
                    .put("privateAccessUrl", "https://fixture.invalid/vega-private")));
    var production =
        json.createObjectNode()
            .put("campaignVideoAssetId", ad.getId())
            .put("productionEvidence", "fixture");
    evidence.production(cycle, production, ExperimentVideoSlot.AD);
    event("CAMPAIGN_VIDEO", "COMPLETE", production);
    production =
        json.createObjectNode()
            .put("pdeVideoAssetId", hero.getId())
            .put("productionEvidence", "fixture");
    evidence.production(cycle, production, ExperimentVideoSlot.LANDING_HERO);
    event("PDE_ENTRY_VIDEO", "COMPLETE", production);
    ReflectionTestUtils.setField(evidence, "automaticBinding", binding);
  }

  /** Cria mídia com hash e legendas reais no contrato, sem acessar rede nem gastar. */
  ExperimentVideoAsset media(
      long id, Experiment experiment, ExperimentVideoSlot role, String hash) {
    var value = new ExperimentVideoAsset();
    value.setId(id);
    value.setExperiment(experiment);
    value.setSlot(role);
    value.setStatus(ExperimentVideoStatus.READY);
    value.setReviewStatus(ExperimentVideoReviewStatus.APPROVED);
    value.setReviewedBy("Responsável da fixture");
    value.setReviewedAt(now);
    value.setHasAudio(true);
    value.setDurationSeconds(15);
    value.setAssetUrl("https://fixture.invalid/" + id + ".mp4");
    value.setHlsPlaybackUrl("https://fixture.invalid/" + id + ".m3u8");
    value.setResponseJson(
        "{\"hls_delivery\":{\"sourceSha256\":\""
            + hash.repeat(64)
            + "\"},\"captions\":{\"burned_in\":true,\"text\":\"Explicação útil\"}}");
    when(videos.findById(id)).thenReturn(Optional.of(value));
    return value;
  }

  /** Registra uma prova no diário isolado mantendo o marco temporal da integração. */
  void event(String stage, String action, ObjectNode proof) {
    var event = new LearningSalesCycleEvent();
    event.setId((long) history.size() + 1);
    event.setCycleId(cycle.getId());
    event.setFromStage(stage);
    event.setAction(action);
    event.setEvidenceJson(proof.toString());
    event.setCreatedAt(now.plusSeconds(1));
    history.add(event);
  }

  /** Reaproveita ambas as aprovações, deixa homologação pendente e não exige publicação privada. */
  @Test
  void integratesPrivateDestinationAndKeepsCommercialBoundaries() {
    var proof = binding.prepare(cycle);
    assertEquals("PRIVATE_PDE", proof.path("mode").asText());
    assertEquals("https://fixture.invalid/vega-private", proof.path("destinationUrl").asText());
    assertTrue(proof.path("technicalValidationRequired").asBoolean());
    for (String key :
        List.of(
            "paymentEnabled",
            "publicationAuthorized",
            "campaignAuthorized",
            "mediaSpendAuthorized")) assertFalse(proof.path(key).asBoolean(true));
    assertEquals(now.toString(), proof.path("heroVideo").path("reviewedAt").asText());
    assertEquals(proof, binding.prepare(cycle));
    verifyNoInteractions(slots, creatives);
    verify(events, never()).save(any());
    event("VIDEO_APPROVAL", "COMPLETE", proof);
    assertEquals(proof, binding.current(cycle));
    evidence.current(cycle, false);
    assertThrows(RuntimeException.class, () -> evidence.current(cycle, true));
  }

  /**
   * Aceita a promoção somente quando o contrato público ativo preserva as mídias aprovadas e o
   * criativo aponta para o destino comercial exato.
   */
  @Test
  void acceptsPublishedPromotionOfPrivateIntegration() {
    event("VIDEO_APPROVAL", "COMPLETE", binding.prepare(cycle));
    experiment.setFollowUpActionUrl("https://fixture.invalid/v8");
    var slot = new PdeProductionSlot();
    slot.setId(91008L);
    slot.setProductSlug(experiment.getProduct().getSlug());
    slot.setSourceExperimentId(cycle.getExperimentId());
    slot.setExperienceVersion(cycle.getProductVersion());
    slot.setStatus(PdeProductionSlotStatus.ACTIVE);
    slot.setPublicUrl(experiment.getFollowUpActionUrl());
    slot.setPublishedAt(now.plusSeconds(20));
    slot.setPublishedExperienceJson(
        json.createObjectNode()
            .set(
                "heroVideos",
                json.createArrayNode()
                    .add(
                        json.createObjectNode()
                            .put("experimentVideoAssetId", hero.getId())
                            .put("hlsPlaybackUrl", hero.getHlsPlaybackUrl())
                            .put("status", "READY")
                            .put("reviewStatus", "APPROVED")))
            .toString());
    when(slots.findByProductSlugOrderBySlotCodeAsc(experiment.getProduct().getSlug()))
        .thenReturn(List.of(slot));
    var creative = new Creative();
    creative.setExperiment(experiment);
    creative.setStatus(CreativeStatus.READY);
    creative.setAgentReviewStatus(CreativeAgentReviewStatus.APPROVED);
    creative.setVideoUrl(ad.getAssetUrl());
    creative.setDestinationUrl(slot.getPublicUrl());
    when(creatives.findByExperimentIdAndVideoUrl(cycle.getExperimentId(), ad.getAssetUrl()))
        .thenReturn(List.of(creative));

    assertDoesNotThrow(() -> evidence.current(cycle, true));

    slot.setPublishedExperienceJson("{\"heroVideos\":[]}");
    assertThrows(RuntimeException.class, () -> evidence.current(cycle, true));
  }

  /** Bloqueia desvio ou revogação sem converter cadastro antigo em aprovação do conjunto novo. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "review",
        "url",
        "hls",
        "hash",
        "captions",
        "duration",
        "product",
        "experiment",
        "role",
        "reviewer",
        "destination"
      })
  void refusesChangedInputAfterIntegration(String field) {
    event("VIDEO_APPROVAL", "COMPLETE", binding.prepare(cycle));
    switch (field) {
      case "review" -> hero.setReviewStatus(ExperimentVideoReviewStatus.REJECTED);
      case "url" -> hero.setAssetUrl("https://fixture.invalid/replacement.mp4");
      case "hls" -> hero.setHlsPlaybackUrl("https://fixture.invalid/replacement.m3u8");
      case "hash" ->
          hero.setResponseJson(hero.getResponseJson().replace("b".repeat(64), "c".repeat(64)));
      case "captions" -> hero.setResponseJson("{}");
      case "duration" -> hero.setDurationSeconds(30);
      case "product" -> hero.getExperiment().getProduct().setId(999L);
      case "experiment" -> hero.getExperiment().setId(999L);
      case "role" -> hero.setSlot(ExperimentVideoSlot.AD);
      case "reviewer" -> hero.setReviewedBy("Outra decisão");
      case "destination" ->
          history
              .getFirst()
              .setEvidenceJson(
                  history.getFirst().getEvidenceJson().replace("/vega-private", "/another"));
    }
    assertThrows(RuntimeException.class, () -> binding.current(cycle));
  }

  /** Uma aprovação pendente deve permanecer explícita e não ser feita pelo coordenador. */
  @Test
  void waitsForActualHumanApproval() {
    hero.setReviewStatus(ExperimentVideoReviewStatus.PENDING);
    assertTrue(binding.awaitingApproval(cycle));
    assertThrows(RuntimeException.class, () -> binding.prepare(cycle));
    assertEquals(ExperimentVideoReviewStatus.PENDING, hero.getReviewStatus());
  }

  /** Impede apresentação de outro ciclo ou versão e conserva diário somente leitura. */
  @Test
  void scopesPresentationByCycleAndVersion() {
    event("VIDEO_APPROVAL", "COMPLETE", binding.prepare(cycle));
    assertTrue(binding.presentation(cycle.getId(), "other").isEmpty());
    assertTrue(binding.presentation(999L, cycle.getProductVersion()).isEmpty());
    assertTrue(binding.forReference("experiment:999").isEmpty());
    assertTrue(binding.presentation(cycle.getId(), cycle.getProductVersion()).isPresent());
    verify(events, never()).saveAndFlush(any());
  }

  /** Exige provas novas identificadas e todos os checks medidos antes do gate. */
  @Test
  void rejectsOldOrIncompleteReviewsAndAcceptsCurrentHarness() {
    var proof = binding.prepare(cycle);
    event("VIDEO_APPROVAL", "COMPLETE", proof);
    var reviewed =
        LearningCycleVideoBinding.REVIEWS.stream()
            .map(
                code -> {
                  var task = new AgentTask();
                  task.setId((long) code.length());
                  task.setProcessActivityId(code);
                  task.setCreatedAt(now);
                  var result =
                      json.createObjectNode()
                          .set("videoIntegrationFingerprint", proof.path("integrationFingerprint"));
                  ((ObjectNode) result)
                      .putObject("checks")
                      .put("videoIdentity", true)
                      .put("videoPlayback", true)
                      .put("videoOptional", true)
                      .put("videoFailureRecovery", true);
                  task.setResultJson(result.toString());
                  return task;
                })
            .toList();
    String reference = "experiment:" + cycle.getExperimentId();
    assertThrows(RuntimeException.class, () -> binding.validateReviews(reference, reviewed));
    reviewed.forEach(t -> t.setCreatedAt(now.plusSeconds(2)));
    assertDoesNotThrow(() -> binding.validateReviews(reference, reviewed));
    reviewed.getFirst().setResultJson("{}");
    assertThrows(RuntimeException.class, () -> binding.validateReviews(reference, reviewed));
  }
}
