package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.Capability;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository;
import com.marketinghub.salesvideo.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Comprova a passagem de vídeos corrigidos sem apagar rejeições nem antecipar aceite humano. */
class LearningCycleVideoReplacementTest {
  private final ObjectMapper json = new ObjectMapper();
  private final ExperimentVideoAssetRepository videos = mock(ExperimentVideoAssetRepository.class);
  private final VideoProjectRepository projects = mock(VideoProjectRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final CreativeFinalVideoEvidence finalReview =
      new CreativeFinalVideoEvidence(videos, projects, tasks, json);
  private final LearningCycleVideoEvidence evidence =
      new LearningCycleVideoEvidence(
          videos,
          mock(CreativeRepository.class),
          mock(PdeProductionSlotRepository.class),
          events,
          new LearningCycleJson(json),
          finalReview);
  private final LearningCycleVideoBinding binding =
      new LearningCycleVideoBinding(
          cycles, events, evidence, new LearningCyclePrototypeContext(events, json), json);
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final List<LearningSalesCycleEvent> history = new ArrayList<>();
  private final List<VideoProject> studio = new ArrayList<>();
  private final Instant now = Instant.parse("2026-10-16T16:50:00Z");
  private Experiment experiment;
  private ExperimentVideoAsset oldAd, oldHero, newAd, newHero;
  private AgentTask review;

  /**
   * Caso original e outra identidade avançam somente após os dois aceites dos arquivos revistos.
   */
  @ParameterizedTest
  @CsvSource({"7,12,105", "9707,9712,97105"})
  void correctedVideosReplaceRejectedHistoryOnlyAfterExplicitApproval(
      long product, long id, long exp) {
    setup(product, id, exp);
    assertThat(evidence.hasRejectedVideo(cycle)).isFalse();
    var capabilities =
        mock(com.marketinghub.pde.kit.privateprototype.v1.service.KitPrototypeCapabilities.class);
    when(capabilities.resolve(cycle))
        .thenReturn(new Capability(false, null, "Etapa audiovisual", null));
    var projection =
        new LearningCycleValueFlowProjection(
            capabilities,
            mock(com.marketinghub.repository.jpa.kit.KitPrivateArtifactRepository.class),
            tasks,
            events,
            mock(com.marketinghub.repository.jpa.processautomation.ProcessRunRepository.class),
            new LearningCyclePrototypeContext(events, json),
            new LearningCycleJson(json),
            new LearningCycleVideoBudget(events, new LearningCycleJson(json)),
            evidence, mock(LearningCycleDecisionProposalRepository.class));
    var flow = projection.resolve(cycle, null);
    assertThat(flow.decisionNeeded()).isTrue();
    assertThat(flow.situation()).contains("aguardam seu aceite");
    assertThat(binding.awaitingApproval(cycle)).isTrue();
    assertThatThrownBy(() -> binding.prepare(cycle)).hasMessageContaining("aguarda aprovação");
    approve(newAd);
    assertThat(binding.awaitingApproval(cycle)).isTrue();
    approve(newHero);
    assertThat(binding.awaitingApproval(cycle)).isFalse();
    var receipt = binding.prepare(cycle);
    assertThat(receipt.at("/campaignVideo/assetId").asLong()).isEqualTo(newAd.getId());
    assertThat(receipt.at("/heroVideo/assetId").asLong()).isEqualTo(newHero.getId());
    assertThat(receipt.path("publicationAuthorized").asBoolean()).isFalse();
    assertThat(oldAd.getReviewStatus()).isEqualTo(ExperimentVideoReviewStatus.REJECTED);
    assertThat(history).hasSize(3);
    verify(videos, never()).save(any());
    verify(events, never()).saveAndFlush(any());
    event("VIDEO_APPROVAL", receipt, now.plusSeconds(1));
    assertThat(binding.current(cycle)).isEqualTo(receipt);
    when(videos.findReadyExperimentVideosForReview(ExperimentVideoStatus.READY, null, product, exp))
        .thenReturn(List.of());
    assertThat(binding.current(cycle)).isEqualTo(receipt);
    newAd.setAssetUrl("https://fixture.invalid/changed.mp4");
    assertThatThrownBy(() -> binding.current(cycle))
        .hasMessageContaining("mudou após a integração");
  }

  /** Parecer sem conclusão ou de outro hash não transforma a correção em decisão humana pronta. */
  @ParameterizedTest
  @CsvSource({"PENDING,false", "COMPLETED,true"})
  void unfinishedOrWrongHashReviewPreservesCorrection(String status, boolean wrongHash) {
    setup(7, 12, 105);
    review.setStatus(status);
    if (wrongHash)
      review.setResultJson(review.getResultJson().replace("a".repeat(64), "d".repeat(64)));
    assertThat(evidence.hasRejectedVideo(cycle)).isTrue();
    assertThat(binding.awaitingApproval(cycle)).isTrue();
    assertThatThrownBy(() -> binding.prepare(cycle)).hasMessageContaining("aguarda aprovação");
  }

  /** Versão, tenant e obrigatoriedade precisam corresponder ao ciclo antes da substituição. */
  @ParameterizedTest
  @CsvSource({"VERSION", "TENANT", "OPTIONAL", "OLD"})
  void unrelatedCandidateDoesNotHideRejection(String mismatch) {
    setup(7, 12, 105);
    switch (mismatch) {
      case "VERSION" -> studio.getFirst().setCampaignKey("other-version");
      case "TENANT" -> studio.getFirst().setTenantId("other-tenant");
      case "OPTIONAL" -> newAd.setRequiredForRelease(false);
      default -> newAd.setCreatedAt(now.minusSeconds(3600));
    }
    assertThat(evidence.hasRejectedVideo(cycle)).isTrue();
    assertThat(binding.awaitingApproval(cycle)).isTrue();
  }

  /** Peças aceitas anteriormente não são trocadas por candidatas posteriores sem decisão. */
  @Test
  void previouslyApprovedSelectionRemainsStable() {
    setup(7, 12, 105);
    approve(oldAd);
    approve(oldHero);
    assertThat(binding.awaitingApproval(cycle)).isFalse();
    assertThat(binding.prepare(cycle).at("/campaignVideo/assetId").asLong())
        .isEqualTo(oldAd.getId());
  }

  /**
   * Reconstrói histórico, produção, revisão e integração com fontes reais e dependências locais.
   */
  private void setup(long productId, long cycleId, long experimentId) {
    cycle.setId(cycleId);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    cycle.setChainDefinitionId(26L);
    cycle.setProductVersion("candidate-v3");
    cycle.setStatus("OPEN");
    cycle.setStage("VIDEO_APPROVAL");
    cycle.setCreatedAt(now.minusSeconds(120));
    cycle.setVersionChangedAt(now.minusSeconds(120));
    var product = new Product();
    product.setId(productId);
    experiment = new Experiment();
    experiment.setId(experimentId);
    experiment.setProduct(product);
    when(events.findByCycleIdOrderByRevisionAsc(cycleId)).thenReturn(history);
    org.springframework.test.util.ReflectionTestUtils.setField(
        evidence, "automaticBinding", binding);
    var proof = json.createObjectNode().put("productVersion", cycle.getProductVersion());
    proof
        .putObject("privatePrototype")
        .put("prototypeVersion", cycle.getProductVersion())
        .put("privateAccessUrl", "https://fixture.invalid/vega-private");
    event("ADJUSTMENT", proof, now.minusSeconds(110));
    oldAd = media(55L, ExperimentVideoSlot.AD, "c", now.minusSeconds(60));
    oldHero = media(56L, ExperimentVideoSlot.LANDING_HERO, "d", now.minusSeconds(60));
    for (var old : List.of(oldAd, oldHero)) {
      var stage = old.getSlot() == ExperimentVideoSlot.AD ? "CAMPAIGN_VIDEO" : "PDE_ENTRY_VIDEO";
      var field =
          old.getSlot() == ExperimentVideoSlot.AD ? "campaignVideoAssetId" : "pdeVideoAssetId";
      var selection =
          json.createObjectNode()
              .put(field, old.getId())
              .put("productionEvidence", "Fixture local");
      evidence.production(cycle, selection, old.getSlot());
      event(stage, selection, now.minusSeconds(50));
      old.setReviewStatus(ExperimentVideoReviewStatus.REJECTED);
    }
    newAd = media(76L, ExperimentVideoSlot.AD, "a", now);
    newHero = media(75L, ExperimentVideoSlot.LANDING_HERO, "b", now);
    when(videos.findReadyExperimentVideosForReview(
            ExperimentVideoStatus.READY, null, productId, experimentId))
        .thenReturn(List.of(newAd, newHero));
    var agent = new Agent();
    agent.setAgentKey("customer-agent");
    var process = new BusinessProcessDefinition();
    process.setProcessCode("creative-production-approval");
    review = new AgentTask();
    review.setId(744L);
    review.setStatus("COMPLETED");
    review.setAssignedAgent(agent);
    review.setProcessDefinition(process);
    review.setProcessActivityId("customer");
    review.setResultJson(
        "{\"decision\":\"APPROVED\",\"videoAudit\":[{\"assetId\":76,\"sha256\":\""
            + "a".repeat(64)
            + "\",\"criticalDeficit\":false},{\"assetId\":75,\"sha256\":\""
            + "b".repeat(64)
            + "\",\"criticalDeficit\":false}]}");
    when(tasks.findTopByAssignedAgentAgentKeyAndSourceReferenceOrderByUpdatedAtDescIdDesc(
            "customer-agent", "experiment:" + experimentId))
        .thenReturn(Optional.of(review));
  }

  /** Cria uma mídia com projeto da versão, hash, legenda e arquivo de teste identificáveis. */
  private ExperimentVideoAsset media(
      long id, ExperimentVideoSlot role, String hash, Instant created) {
    var video = new ExperimentVideoAsset();
    video.setId(id);
    video.setExperiment(experiment);
    video.setSlot(role);
    video.setStatus(ExperimentVideoStatus.READY);
    video.setReviewStatus(ExperimentVideoReviewStatus.PENDING);
    video.setCreatedAt(created);
    video.setRequiredForRelease(true);
    video.setHasAudio(true);
    video.setDurationSeconds(35);
    video.setAssetUrl("https://fixture.invalid/" + id + ".mp4");
    video.setHlsPlaybackUrl("https://fixture.invalid/" + id + ".m3u8");
    video.setResponseJson(
        "{\"customerQualityContract\":\"CUSTOMER_VIDEO_QUALITY_V1\",\"hls_delivery\":{\"sourceSha256\":\""
            + hash.repeat(64)
            + "\"},\"captions\":{\"burned_in\":true,\"text\":\"Explicação útil\"}}");
    if (id >= 75) {
      var job = new SalesVideoJob();
      job.setTenantId("fixture-tenant");
      job.setMetadataJson("{\"videoProjectId\":" + id + "}");
      video.setSalesVideoJob(job);
      var project = new VideoProject();
      project.setId(id);
      project.setProductId(cycle.getProductId());
      project.setExperimentId(cycle.getExperimentId());
      project.setCampaignKey(cycle.getProductVersion());
      project.setTenantId("fixture-tenant");
      studio.add(project);
      when(projects.findById(id)).thenReturn(Optional.of(project));
    }
    when(videos.findById(id)).thenReturn(Optional.of(video));
    return video;
  }

  /** Registra aceite explícito somente na fixture, preservando responsável e horário. */
  private void approve(ExperimentVideoAsset video) {
    video.setReviewStatus(ExperimentVideoReviewStatus.APPROVED);
    video.setReviewedBy("Operador da fixture");
    video.setReviewedAt(now);
  }

  /** Preserva os eventos anteriores e registra a identidade explícita da integração. */
  private void event(String stage, ObjectNode proof, Instant created) {
    var event = new LearningSalesCycleEvent();
    event.setId((long) history.size() + 1);
    event.setCycleId(cycle.getId());
    event.setFromStage(stage);
    event.setAction("ADJUSTMENT".equals(stage) ? "REWORK" : "COMPLETE");
    event.setCreatedAt(created);
    event.setEvidenceJson(proof.toString());
    history.add(event);
  }
}
