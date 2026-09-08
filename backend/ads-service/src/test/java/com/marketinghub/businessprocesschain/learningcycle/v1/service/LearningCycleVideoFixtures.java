package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.*;
import com.marketinghub.experiment.video.*;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

/**
 * Responsabilidade: simular exclusivamente os resultados externos de mídia para homologar o BPM.
 */
@TestConfiguration
@Profile("learning-cycles-fixture")
@Import(LearningCycleVideoFixtures.Controller.class)
public class LearningCycleVideoFixtures {
  static final Map<Long, ExperimentVideoAsset> VIDEOS = new ConcurrentHashMap<>();
  static final Map<Long, Creative> CREATIVES = new ConcurrentHashMap<>();
  static final Map<Long, PdeProductionSlot> SLOTS = new ConcurrentHashMap<>();

  /** Simula consulta de mídia; o ciclo e sua auditoria continuam em MySQL real. */
  @Bean
  ExperimentVideoAssetRepository videos() {
    var repo = mock(ExperimentVideoAssetRepository.class);
    when(repo.findById(anyLong()))
        .thenAnswer(c -> Optional.ofNullable(VIDEOS.get(c.getArgument(0))));
    when(repo.findByExperimentIdOrderByCreatedAtDesc(anyLong()))
        .thenAnswer(
            c ->
                VIDEOS.values().stream()
                    .filter(v -> v.getExperiment().getId().equals(c.getArgument(0)))
                    .toList());
    return repo;
  }

  /** Simula seleção de criativo aprovada pelos contratos externos ao ciclo. */
  @Bean
  CreativeRepository creatives() {
    var repo = mock(CreativeRepository.class);
    when(repo.findById(anyLong()))
        .thenAnswer(c -> Optional.ofNullable(CREATIVES.get(c.getArgument(0))));
    when(repo.findByExperimentIdAndVideoUrl(anyLong(), anyString()))
        .thenAnswer(
            c ->
                CREATIVES.values().stream()
                    .filter(
                        v ->
                            v.getExperiment().getId().equals(c.getArgument(0))
                                && v.getVideoUrl().equals(c.getArgument(1)))
                    .toList());
    return repo;
  }

  /** Simula contrato PDE sem instalar produto ou publicar domínio externo. */
  @Bean
  PdeProductionSlotRepository slots() {
    var repo = mock(PdeProductionSlotRepository.class);
    when(repo.findById(anyLong()))
        .thenAnswer(c -> Optional.ofNullable(SLOTS.get(c.getArgument(0))));
    when(repo.findByProductSlugOrderBySlotCodeAsc(anyString()))
        .thenAnswer(
            c ->
                SLOTS.values().stream()
                    .filter(v -> v.getProductSlug().equals(c.getArgument(0)))
                    .toList());
    return repo;
  }

  /** Responsabilidade: oferecer controle local explícito dos test doubles de vídeo e publicação. */
  @RestController
  @Profile("learning-cycles-fixture")
  static class Controller {
    private final ObjectMapper mapper;

    /** Recebe o serializador da aplicação local. */
    Controller(ObjectMapper mapper) {
      this.mapper = mapper;
    }

    /** Instala dois vídeos e uma integração fictícios para o experimento de teste selecionado. */
    @PostMapping("/fixture/videos")
    Map<String, Object> seed(@RequestBody Map<String, Object> body) throws Exception {
      long experimentId = ((Number) body.get("experimentId")).longValue();
      var experiment = LearningCycleLocalApplication.EXPERIMENTS.get(experimentId);
      if (experiment == null) throw new IllegalArgumentException("Somente experimento local.");
      String version = body.get("productVersion").toString();
      experiment.setFollowUpActionUrl("https://fixture.invalid/" + experimentId + "/" + version);
      long adId = experimentId * 10, heroId = adId + 1;
      for (long id : List.of(adId, heroId)) {
        var video = new ExperimentVideoAsset();
        video.setId(id);
        video.setExperiment(experiment);
        video.setSlot(id == adId ? ExperimentVideoSlot.AD : ExperimentVideoSlot.LANDING_HERO);
        video.setStatus(ExperimentVideoStatus.READY);
        video.setReviewStatus(ExperimentVideoReviewStatus.APPROVED);
        video.setAssetUrl("https://fixture.invalid/" + version + "/" + id + ".mp4");
        video.setHlsPlaybackUrl("https://fixture.invalid/" + version + "/" + id + ".m3u8");
        video.setHasAudio(true);
        video.setDurationSeconds(30);
        video.setObjective("Entrega audiovisual simulada " + video.getSlot());
        video.setPrimaryMetric(id == adId ? "Sessão qualificada" : "Primeiro resultado e compra");
        VIDEOS.put(id, video);
      }
      var creative = new Creative();
      creative.setId(adId);
      creative.setExperiment(experiment);
      creative.setVideoUrl(VIDEOS.get(adId).getAssetUrl());
      creative.setDestinationUrl(experiment.getFollowUpActionUrl());
      creative.setStatus(CreativeStatus.READY);
      creative.setAgentReviewStatus(CreativeAgentReviewStatus.APPROVED);
      creative.setHeadline("Demonstração local");
      CREATIVES.put(adId, creative);
      var slot = new PdeProductionSlot();
      slot.setId(heroId);
      slot.setProductSlug(experiment.getProduct().getSlug());
      slot.setSourceExperimentId(experimentId);
      slot.setExperienceVersion(version);
      slot.setSlotCode(version);
      slot.setPublicUrl(experiment.getFollowUpActionUrl());
      slot.setDraftExperienceJson(
          mapper.writeValueAsString(
              Map.of(
                  "heroVideos",
                  List.of(
                      Map.of(
                          "experimentVideoAssetId",
                          heroId,
                          "hlsPlaybackUrl",
                          VIDEOS.get(heroId).getHlsPlaybackUrl(),
                          "status",
                          "READY",
                          "reviewStatus",
                          "APPROVED")))));
      slot.setPublishedExperienceJson(slot.getDraftExperienceJson());
      SLOTS.put(heroId, slot);
      return Map.of(
          "campaignVideoAssetId",
          adId,
          "pdeVideoAssetId",
          heroId,
          "creativeId",
          adId,
          "pdeSlotId",
          heroId);
    }

    /** Injeta falhas locais para comprovar revogação, integridade e recuperação. */
    @PostMapping("/fixture/videos/mutate")
    Map<String, Object> mutate(@RequestBody Map<String, Object> body) {
      long id = ((Number) body.get("id")).longValue();
      String kind = body.get("kind").toString();
      switch (kind) {
        case "reject" -> VIDEOS.get(id).setReviewStatus(ExperimentVideoReviewStatus.REJECTED);
        case "change-url" -> VIDEOS.get(id).setAssetUrl("https://fixture.invalid/replaced.mp4");
        case "unpublish" -> SLOTS.get(id).setPublishedExperienceJson("{}");
        case "publish" ->
            SLOTS.get(id).setPublishedExperienceJson(SLOTS.get(id).getDraftExperienceJson());
        case "wrong-version" -> SLOTS.get(id).setExperienceVersion("outra-versao");
        default -> throw new IllegalArgumentException("Falha de fixture desconhecida.");
      }
      return Map.of("simulated", true);
    }
  }
}
