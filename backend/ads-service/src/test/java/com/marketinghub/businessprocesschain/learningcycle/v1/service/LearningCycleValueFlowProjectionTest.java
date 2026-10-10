package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleProcessContext.Work;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.*;
import com.marketinghub.pde.kit.privateprototype.v1.service.contract.KitPrivateContract.Capability;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.kit.KitPrivateArtifactRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.processautomation.ProcessRunRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Responsabilidade: preservar a dependência real e a diferença entre dado ausente, teste e venda
 * conciliada.
 */
class LearningCycleValueFlowProjectionTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final KitPrototypeCapabilities capabilities = mock(KitPrototypeCapabilities.class);
  private final KitPrivateArtifactRepository artifacts = mock(KitPrivateArtifactRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final LearningCyclePrototypeContext prototype = mock(LearningCyclePrototypeContext.class);
  private final ProcessRunRepository runs = mock(ProcessRunRepository.class);
  private final ExperimentVideoAssetRepository videos = mock(ExperimentVideoAssetRepository.class);
  private final LearningCycleVideoEvidence videoEvidence =
      new LearningCycleVideoEvidence(
          videos,
          mock(CreativeRepository.class),
          mock(PdeProductionSlotRepository.class),
          events,
          new LearningCycleJson(mapper));
  private final LearningCycleValueFlowProjection projection =
      new LearningCycleValueFlowProjection(
          capabilities,
          artifacts,
          tasks,
          events,
          runs,
          prototype,
          new LearningCycleJson(mapper),
          new LearningCycleVideoBudget(events, new LearningCycleJson(mapper)),
          videoEvidence);

  /** Mira e outro contexto usam as duas peças reais selecionadas para expor o aceite pendente. */
  @ParameterizedTest
  @CsvSource({"10,9,102,mira-private-candidate-v3", "87,56,195,independent-v2"})
  void producedVideosExposeActualHumanDecision(
      long productId, long cycleId, long experimentId, String version) throws Exception {
    var cycle = videoFixture(productId, cycleId, experimentId, version);
    cycle.setStage("VIDEO_APPROVAL");
    var selected = producedVideos(cycle);
    selected.getFirst().setReviewStatus(ExperimentVideoReviewStatus.APPROVED);
    var flow = projection.resolve(cycle, null);
    assertThat(flow.decisionNeeded()).isTrue();
    assertThat(flow.situation()).contains("produzidos", "aceite de uso");
    assertThat(flow.resolvingResponsible()).startsWith("Você");
    assertThat(flow.decisionReason())
        .contains("com som", "pedir ajustes", "Ver aprovações dos vídeos")
        .doesNotContain("Nenhuma decisão nova");
    assertThat(flow.saleBlocker()).contains("seleção humana", "teto financeiro não substitui");
    assertThat(flow.activeExecution()).isFalse();
    assertThat(flow.acceptance()).contains("homologação", "antes da campanha");
    assertThat(flow.marketMeasurement()).isNull();
    verify(videos, never()).save(any());
    verify(events, never()).saveAndFlush(any());
    verifyNoInteractions(runs);
    String evidenceDirectory = System.getProperty("learningCycle.valueFlow.evidence");
    if (evidenceDirectory != null)
      java.nio.file.Files.writeString(
          java.nio.file.Path.of(
              evidenceDirectory, "value-flow-" + productId + "-" + cycleId + ".json"),
          mapper
              .copy()
              .findAndRegisterModules()
              .disable(
                  com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
              .writeValueAsString(flow));
  }

  /** Ambos os aceites eliminam a pergunta sem fabricar integração, homologação ou execução. */
  @Test
  void approvedVideosDoNotRequestApprovalAgain() {
    var cycle = videoFixture(4, 113, 206, "vega-private-candidate-v14");
    cycle.setStage("VIDEO_APPROVAL");
    producedVideos(cycle).forEach(v -> v.setReviewStatus(ExperimentVideoReviewStatus.APPROVED));
    var flow = projection.resolve(cycle, null);
    assertThat(flow.decisionNeeded()).isFalse();
    assertThat(flow.situation()).contains("aceite", "registrado", "precisam ser comprovadas");
    assertThat(flow.saleBlocker()).contains("integração", "homologação", "liberação comercial");
    assertThat(flow.resolvingResponsible()).startsWith("Backend");
    assertThat(flow.activeExecution()).isFalse();
    assertThat(flow.decisionReason()).contains("reutilizados");
    verify(events, never()).saveAndFlush(any());
  }

  /** Evidência divergente permanece como falha técnica, sem inventar uma decisão do operador. */
  @Test
  void changedVideoEvidenceDoesNotMasqueradeAsHumanApproval() {
    var cycle = videoFixture(87, 56, 195, "independent-v2");
    cycle.setStage("VIDEO_APPROVAL");
    producedVideos(cycle).getFirst().setAssetUrl("https://fixture.invalid/changed.mp4");
    var flow = projection.resolve(cycle, null);
    assertThat(flow.decisionNeeded()).isNull();
    assertThat(flow.saleBlocker()).contains("evidência");
    assertThat(flow.situation()).contains("conferência");
    assertThat(flow.resolvingResponsible()).contains("vínculos");
    verify(events, never()).saveAndFlush(any());
  }

  /** O relógio considera a última entrega audiovisual, em vez da implementação antiga. */
  @Test
  void videoApprovalWaitStartsAtCurrentVideoDelivery() {
    var cycle = videoFixture(10, 9, 102, "mira-private-candidate-v3");
    cycle.setStage("VIDEO_APPROVAL");
    producedVideos(cycle);
    var flow = projection.resolve(cycle, null);
    assertThat(flow.stalledSince()).isAfter(Instant.now().minusSeconds(60));
    assertThat(flow.stalledSeconds()).isBetween(0L, 60L);
  }

  /** O histórico encerrado não solicita outra seleção nem consulta as peças para execução. */
  @Test
  void closedVideoApprovalDoesNotRequestHumanDecision() {
    var cycle = videoFixture(10, 9, 102, "mira-private-candidate-v3");
    cycle.setStage("VIDEO_APPROVAL");
    cycle.setStatus("CLOSED");
    var flow = projection.resolve(cycle, null);
    assertThat(flow.decisionNeeded()).isFalse();
    assertThat(flow.situation()).contains("encerrado");
    verifyNoInteractions(videos);
  }

  /** Monta seleções com fingerprints calculados pelo gate canônico, sem aprovar qualquer peça. */
  private List<ExperimentVideoAsset> producedVideos(LearningSalesCycle cycle) {
    var product = new Product();
    product.setId(cycle.getProductId());
    var experiment = new Experiment();
    experiment.setId(cycle.getExperimentId());
    experiment.setProduct(product);
    var history = new ArrayList<LearningSalesCycleEvent>();
    when(events.findByCycleIdOrderByRevisionAsc(cycle.getId())).thenReturn(history);
    var selected = new ArrayList<ExperimentVideoAsset>();
    for (var role : List.of(ExperimentVideoSlot.AD, ExperimentVideoSlot.LANDING_HERO)) {
      long id = cycle.getExperimentId() * 10 + selected.size();
      var video = new ExperimentVideoAsset();
      video.setId(id);
      video.setExperiment(experiment);
      video.setSlot(role);
      video.setStatus(ExperimentVideoStatus.READY);
      video.setReviewStatus(ExperimentVideoReviewStatus.PENDING);
      video.setHasAudio(true);
      video.setDurationSeconds(30);
      video.setAssetUrl("https://fixture.invalid/" + id + ".mp4");
      video.setHlsPlaybackUrl("https://fixture.invalid/" + id + ".m3u8");
      when(videos.findById(id)).thenReturn(Optional.of(video));
      var proof =
          mapper
              .createObjectNode()
              .put(role == ExperimentVideoSlot.AD ? "campaignVideoAssetId" : "pdeVideoAssetId", id)
              .put("productionEvidence", "Entrega simulada e segregada de homologação local");
      videoEvidence.production(cycle, proof, role);
      var event = new LearningSalesCycleEvent();
      event.setCycleId(cycle.getId());
      event.setAction("COMPLETE");
      event.setFromStage(role == ExperimentVideoSlot.AD ? "CAMPAIGN_VIDEO" : "PDE_ENTRY_VIDEO");
      event.setCreatedAt(Instant.now());
      event.setEvidenceJson(proof.toString());
      history.add(event);
      selected.add(video);
    }
    return selected;
  }

  /** Mira e outro produto devem expor a decisão real sem inventar orçamento ou execução. */
  @ParameterizedTest
  @CsvSource({"10,9,102,mira-private-candidate-v3", "87,56,195,independent-v2"})
  void videoBriefRequiresItsOwnBudget(
      long productId, long cycleId, long experimentId, String version) {
    var cycle = videoFixture(productId, cycleId, experimentId, version);
    var flow = projection.resolve(cycle, null);
    assertThat(flow.decisionNeeded()).isTrue();
    assertThat(flow.situation()).contains("aguarda sua decisão", "produção e revisão");
    assertThat(flow.resolvingResponsible()).contains("Você");
    assertThat(flow.awaitingResponsible()).contains("Apolo", "Plutus");
    assertThat(flow.decisionReason()).contains("Financeiro dos vídeos", "US$", "duas peças");
    assertThat(flow.saleBlocker()).contains("não autoriza vídeos ou mídia");
    assertThat(flow.activeExecution()).isFalse();
    assertThat(flow.marketMeasurement()).isNull();
    verify(events).findByCycleIdAndActionOrderByRevisionDesc(cycleId, "AUTHORIZE_VIDEO_BUDGET");
    verify(events, never()).saveAndFlush(any());
    verifyNoInteractions(runs);
  }

  /** A autorização vigente elimina a pergunta financeira sem afirmar que a produção começou. */
  @Test
  void currentVideoBudgetIsReusedWithoutClaimingProduction() {
    var cycle = videoFixture(10, 9, 102, "mira-private-candidate-v3");
    when(events.findByCycleIdAndActionOrderByRevisionDesc(9L, "AUTHORIZE_VIDEO_BUDGET"))
        .thenReturn(List.of(videoAuthorization(cycle, cycle.getProductVersion(), 102L)));
    var flow = projection.resolve(cycle, null);
    assertThat(flow.decisionNeeded()).isFalse();
    assertThat(flow.situation()).contains("teto dos vídeos está registrado", "briefing");
    assertThat(flow.decisionReason()).contains("reutilizado", "não inicia produção");
    assertThat(flow.saleBlocker()).contains("produção, revisão e integração");
    assertThat(flow.activeExecution()).isFalse();
    verify(events, never()).saveAndFlush(any());
  }

  /** Um recibo de outra versão ou experimento não autoriza a candidata corrente. */
  @ParameterizedTest
  @CsvSource({"old-version,102", "mira-private-candidate-v3,93"})
  void historicalVideoBudgetDoesNotHideCurrentDecision(String version, long experimentId) {
    var cycle = videoFixture(10, 9, 102, "mira-private-candidate-v3");
    when(events.findByCycleIdAndActionOrderByRevisionDesc(9L, "AUTHORIZE_VIDEO_BUDGET"))
        .thenReturn(List.of(videoAuthorization(cycle, version, experimentId)));
    assertThat(projection.resolve(cycle, null).decisionNeeded()).isTrue();
  }

  /** Ciclo encerrado preserva o histórico sem solicitar uma autorização nova. */
  @Test
  void closedVideoBriefDoesNotRequestAnotherBudget() {
    var cycle = videoFixture(10, 9, 102, "mira-private-candidate-v3");
    cycle.setStatus("CLOSED");
    var flow = projection.resolve(cycle, null);
    assertThat(flow.decisionNeeded()).isFalse();
    assertThat(flow.situation()).contains("encerrado");
    verify(events, never()).findByCycleIdAndActionOrderByRevisionDesc(anyLong(), anyString());
  }

  /** Reproduz a etapa após a entrega privada aceita, sem tarefa ou processo ativo. */
  private LearningSalesCycle videoFixture(
      long productId, long cycleId, long experimentId, String version) {
    var cycle = fixture();
    cycle.setProductId(productId);
    cycle.setId(cycleId);
    cycle.setExperimentId(experimentId);
    cycle.setProductVersion(version);
    cycle.setStage("VIDEO_BRIEF");
    cycle.setVersionChangedAt(Instant.EPOCH);
    when(capabilities.resolve(cycle))
        .thenReturn(new Capability(false, null, "Etapa audiovisual", null));
    when(prototype.resolve(cycle)).thenReturn(Optional.of(mapper.createObjectNode()));
    return cycle;
  }

  /** Monta recibo real para usar o mesmo validador de identidade do financeiro dos vídeos. */
  private LearningSalesCycleEvent videoAuthorization(
      LearningSalesCycle cycle, String version, long experimentId) {
    var event = new LearningSalesCycleEvent();
    event.setId(777L);
    event.setCycleId(cycle.getId());
    event.setCreatedAt(Instant.now());
    event.setEvidenceReference(
        "internal://learning-cycles/" + cycle.getId() + "/video-budget/test");
    event.setEvidenceJson(
        mapper
            .createObjectNode()
            .put("productVersion", version)
            .put("experimentId", experimentId)
            .put("currency", "USD")
            .put("scope", "TWO_VIDEOS_PRODUCTION_AND_REVIEW")
            .put("budgetLimitUsd", 5)
            .toString());
    return event;
  }

  /** Capella homologada continua pausada; a tela não pode negar a necessidade de retomada. */
  @Test
  void acceptedImplementationKeepsPausedProcessVisible() {
    var cycle = fixture();
    cycle.setId(5L);
    cycle.setProductId(7L);
    cycle.setExperimentId(98L);
    cycle.setChainDefinitionId(26L);
    when(prototype.resolve(cycle)).thenReturn(Optional.of(mapper.createObjectNode()));
    var run = pausedRun(47L);
    when(runs
            .findFirstByProductIdAndProcessDefinitionIdAndChainDefinitionIdAndLearningCycleIdAndSourceReferenceOrderByIdDesc(
                7L, 117L, 26L, 5L, "experiment:98"))
        .thenReturn(Optional.of(run));
    var flow = projection.resolve(cycle, reviewWork());
    assertThat(flow.situation()).contains("pausada");
    assertThat(flow.saleBlocker()).contains("#47", "Pausado");
    assertThat(flow.decisionNeeded()).isTrue();
    assertThat(flow.decisionReason()).contains("limites", "não libera novos gastos");
    assertThat(flow.awaitingResponsible()).isEqualTo("Psique");
    assertThat(flow.prototypeRegistered()).isTrue();
    assertThat(flow.activeExecution()).isFalse();
  }

  /** A pausa de outro produto só aparece na consulta da sua identidade completa. */
  @Test
  void pausedIndependentProductUsesItsOwnProcessScope() {
    var cycle = fixture();
    cycle.setChainDefinitionId(16L);
    when(runs
            .findFirstByProductIdAndProcessDefinitionIdAndChainDefinitionIdAndLearningCycleIdAndSourceReferenceOrderByIdDesc(
                75L, 117L, 16L, 55L, "experiment:95"))
        .thenReturn(Optional.of(pausedRun(87L)));
    var flow = projection.resolve(cycle, work());
    assertThat(flow.saleBlocker()).contains("#87").doesNotContain("#47");
    assertThat(flow.decisionNeeded()).isTrue();
    assertThat(flow.prototypeRegistered()).isFalse();
  }

  /** Uma tarefa técnica em conclusão de pausa continua em execução, sem exigir nova decisão. */
  @Test
  void pausingTaskPreservesActiveTechnicalExecution() {
    var cycle = fixture();
    var run = pausedRun(87L);
    run.setStatus("PAUSING");
    when(runs
            .findFirstByProductIdAndProcessDefinitionIdAndChainDefinitionIdAndLearningCycleIdAndSourceReferenceOrderByIdDesc(
                75L, 117L, null, 55L, "experiment:95"))
        .thenReturn(Optional.of(run));
    when(prototype.resolve(cycle)).thenReturn(Optional.of(mapper.createObjectNode()));
    when(tasks.findFunctionalSnapshots(eq("experiment:95"), anySet(), eq(Instant.EPOCH)))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    1000L,
                    117L,
                    "pde-construction-approval",
                    "technicalHomologation",
                    "customer-agent",
                    "IN_PROGRESS",
                    Instant.EPOCH,
                    null,
                    null)));
    var flow = projection.resolve(cycle, work());
    assertThat(flow.activeExecution()).isTrue();
    assertThat(flow.decisionNeeded()).isNull();
    assertThat(flow.situation()).doesNotContain("está pausada");
  }

  /** Constrói somente o estado persistido de pausa, sem inventar autorização ou tarefa. */
  private ProcessRun pausedRun(Long id) {
    var run = new ProcessRun();
    run.setId(id);
    run.setStatus("PAUSED");
    run.setReason("Pausado. Resultados preservados; retome quando desejar continuar.");
    return run;
  }

  /** Mantém a próxima revisão tecnicamente disponível no mesmo processo. */
  private Work reviewWork() {
    return new Work(
        117L,
        3,
        "Construção",
        "psiqueAdherent",
        7,
        "Cenário aderente",
        "Psique",
        "NOT_STARTED",
        "Homologação técnica aprovada; revisão disponível.",
        "/products/7/value-chain-history");
  }

  /** O ciclo encerrado preserva a história e não consulta uma pausa para criar nova decisão. */
  @Test
  void closedCycleDoesNotCreatePauseDecisionFromHistory() {
    var cycle = fixture();
    cycle.setStatus("CLOSED");
    projection.resolve(cycle, work());
    verifyNoInteractions(runs);
  }

  /**
   * Mostra Dédalo como dependência de Psique sem fabricar execução, decisão humana ou métrica zero.
   */
  @Test
  void preservesImplementationOwnerAndUnknownMetrics() {
    var cycle = fixture();
    var flow = projection.resolve(cycle, work());
    assertThat(flow.resolvingResponsible()).contains("Dédalo");
    assertThat(flow.awaitingResponsible()).isEqualTo("Psique");
    assertThat(flow.activeExecution()).isFalse();
    assertThat(flow.decisionNeeded()).isFalse();
    assertThat(flow.marketMeasurement()).isNull();
    assertThat(flow.deliverables().getFirst().usableOutput())
        .contains("não comprova implementação");
  }

  /**
   * O relógio de espera usa a entrega do pacote mais recente, sem manter a data da especificação
   * anterior.
   */
  @Test
  void measuresWaitFromLatestUsableDelivery() {
    var cycle = fixture();
    Instant delivered = Instant.now().minusSeconds(10);
    var result = mock(KitPrivateArtifactRepository.Summary.class);
    when(result.getStatus()).thenReturn("READY");
    when(result.getFinishedAt()).thenReturn(delivered);
    when(artifacts.summaries(55L)).thenReturn(List.of(result));
    var manifest =
        mapper.createObjectNode().put("packageContractVersion", "PDE_PRIVATE_KIT_PACKAGE_V2");
    var files = manifest.putArray("files");
    for (int i = 0; i < 36; i++) files.addObject().put("name", "arquivo-" + i);
    when(artifacts.acceptedManifests(55L, 75L, 95L, "other-kit-v1", "barber-v1"))
        .thenReturn(List.of(manifest.toString()));
    var flow = projection.resolve(cycle, work());
    assertThat(flow.stalledSince()).isEqualTo(delivered);
    assertThat(flow.readyPackages()).isEqualTo(1);
    assertThat(flow.activeExecution()).isFalse();
  }

  /** Um pacote histórico preservado não pode aparecer como entrega atual utilizável. */
  @Test
  void previousPackageStaysInHistoryWithoutClaimingCurrentReadiness() {
    var cycle = fixture();
    var old = mock(KitPrivateArtifactRepository.Summary.class);
    when(old.getStatus()).thenReturn("READY");
    when(artifacts.summaries(55L)).thenReturn(List.of(old));
    when(artifacts.acceptedManifests(55L, 75L, 95L, "other-kit-v1", "barber-v1"))
        .thenReturn(List.of("{\"files\":[{}]}"));
    var flow = projection.resolve(cycle, work());
    assertThat(flow.readyPackages()).isZero();
    assertThat(flow.situation()).contains("falta comprovar").doesNotContain("está utilizável");
    assertThat(flow.prototypeRegistered()).isFalse();
    assertThat(flow.decisionNeeded()).isFalse();
  }

  /**
   * Conciliação válida da própria ocorrência aceita zero medido; dado inválido mantém o valor
   * desconhecido.
   */
  @Test
  void acceptsOnlyValidReconciledCommercialMeasurement() {
    var cycle = fixture();
    var event = new LearningSalesCycleEvent();
    event.setId(71L);
    event.setCycleId(55L);
    event.setAction("MEASURE");
    event.setToStage("AUTHORIZATION");
    event.setEvidenceJson(
        "{\"netSales\":0,\"revenueBrl\":0,\"dataValid\":false,\"testDataExcluded\":true}");
    when(events.findByCycleIdOrderByRevisionAsc(55L)).thenReturn(List.of(event));
    assertThat(projection.resolve(cycle, work()).marketMeasurement()).isNull();
    event.setEvidenceJson(event.getEvidenceJson().replace("false", "true"));
    assertThat(projection.resolve(cycle, work()).marketMeasurement().path("netSales").asInt(-1))
        .isZero();
  }

  /** Configura um produto independente para prevenir exceções por identidade de Capella. */
  private LearningSalesCycle fixture() {
    var cycle = new LearningSalesCycle();
    cycle.setId(55L);
    cycle.setProductId(75L);
    cycle.setExperimentId(95L);
    cycle.setProductVersion("other-kit-v1");
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    cycle.setCreatedAt(Instant.EPOCH);
    when(capabilities.resolve(cycle))
        .thenReturn(
            new Capability(
                true,
                "barber-v1",
                "Contrato aprovado",
                "https://pagamentopalf.site/mh-api/pde/kit/private/v1/prototype"));
    when(prototype.resolve(cycle)).thenReturn(Optional.empty());
    when(tasks.findFunctionalSnapshots(eq("experiment:95"), anySet(), eq(Instant.EPOCH)))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    999L,
                    117L,
                    "pde-construction-approval",
                    "access",
                    "landing-generator",
                    "COMPLETED",
                    Instant.EPOCH,
                    Instant.EPOCH,
                    "{}")));
    return cycle;
  }

  /** Mantém a atividade real de homologação independente como quem aguarda a implementação. */
  private Work work() {
    return new Work(
        117L,
        3,
        "Construção",
        "technicalHomologation",
        6,
        "Homologação",
        "Psique",
        "BLOCKED",
        "Falta implementação.",
        "/products/75/value-chain-history");
  }
}
