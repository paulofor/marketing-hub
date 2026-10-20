package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.agenttask.AgentTaskVisualEvidence;
import com.marketinghub.agenttask.AgentTaskVisualEvidenceService;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.agenttask.CreativeVisualEvidenceService;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleConstructionContext;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskVisualEvidenceRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: comprovar a passagem privada a Íris sem reaproveitar plano ou prova inválida.
 */
class IrisLearningCycleContextTest {
  private static final String SOURCE = "experiment:91092";
  private static final String VERSION = "musa-pde-entry-v12-primeiro-ajuste-aplicavel";
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final LearningCycleConstructionContext construction =
      mock(LearningCycleConstructionContext.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final IrisLearningCycleContext provider =
      new IrisLearningCycleContext(
          cycles, experiments, chains, construction, instances, tasks, json);
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final BusinessProcessActivityInstance gate = new BusinessProcessActivityInstance();
  private final ObjectNode proof = json.createObjectNode();
  private final ObjectNode pde = json.createObjectNode();
  private final List<PdeValidationTaskSnapshot> history = new ArrayList<>();

  /** Prepara contexto sintético com três contratos privados e cinco provas imutáveis aprovadas. */
  @BeforeEach
  void fixture() throws Exception {
    fixture(91004L, 91002L, 91092L, 91014L);
  }

  /** Monta a mesma passagem para identidades distintas sem depender do produto real. */
  private void fixture(long productId, long cycleId, long experimentId, long chainId)
      throws Exception {
    String source = "experiment:" + experimentId;
    history.clear();
    proof.removeAll();
    pde.removeAll();
    cycle.setId(cycleId);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    cycle.setChainDefinitionId(chainId);
    cycle.setProductVersion(VERSION);
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    var product =
        Product.builder()
            .id(productId)
            .name("PDE sintético")
            .internalName("Vega QA")
            .slug("vega-qa")
            .build();
    var experiment = new Experiment();
    experiment.setId(experimentId);
    experiment.setProduct(product);
    experiment.setStatus(ExperimentStatus.PLANNED);
    var process = new BusinessProcessDefinition();
    process.setId(70L);
    process.setProcessCode("pde-construction-approval");
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(process);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(List.of(item));
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("agentValidationGate");
    activity.setProcessDefinition(process);
    gate.setId(910258L);
    gate.setActivityDefinition(activity);
    gate.setOccurrenceNumber(1);
    gate.setStatus("COMPLETED");
    gate.setObjectiveAchieved(true);
    pde.putObject("lineage")
        .put("learningCycleId", cycleId)
        .put("productId", productId)
        .put("experimentId", experimentId)
        .put("strategyTaskId", 359L)
        .put("economicsTaskId", 361L)
        .put("architectureTaskId", 362L);
    pde.putObject("marketStrategy")
        .put("contractVersion", "MARKET_STRATEGY_V3")
        .put("valueMechanism", "Primeiro resultado aplicável e retomável.");
    pde.putObject("economics").put("commercialSpendAuthorized", false);
    pde.putObject("harness").put("prototypeObjective", "Resultado útil sem conhecimento de IA.");
    pde.putObject("privatePrototypeAcceptance")
        .put("prototypeVersion", VERSION)
        .put("privateAccessUrl", "https://local.example/vega-private");
    pde.putObject("inheritedLearning")
        .put("sourceExperimentId", 91091)
        .put("limitation", "Amostra pequena sem causa comprovada.");
    var target =
        new AgentTaskTargetResponse(
            source,
            experimentId,
            productId,
            "vega-qa",
            "PDE sintético",
            "Vega QA",
            VERSION,
            "https://local.example/vega-private",
            null,
            null,
            null,
            null,
            pde);
    proof
        .put("evidenceType", "PDE_AGENT_VALIDATION_GATE_V1")
        .put("productId", productId)
        .put("productSlug", "vega-qa")
        .put("sourceReference", source)
        .put("prototypeVersion", VERSION)
        .put("publicUrl", target.publicUrl())
        .put("trafficClass", "AGENT_VALIDATION")
        .put("internalMarker", "mh_internal_test")
        .put("humanEvidenceClaimed", false)
        .put("commercialEvidenceClaimed", false)
        .put("paymentEnabled", false)
        .put("publicationAuthorized", false)
        .put("campaignAuthorized", false)
        .put("mediaSpendAuthorizedBrl", 0);
    var evidence = proof.putArray("taskEvidence");
    long id = 395;
    for (String code :
        List.of(
            "technicalHomologation",
            "psiqueAdherent",
            "psiqueRecovery",
            "psiqueSafety",
            "commercialIntegrityReview")) {
      String result =
          "{\"decision\":\"APPROVED\",\"synthetic\":true,\"activity\":\"" + code + "\"}";
      history.add(
          new PdeValidationTaskSnapshot(id, 70L, code, "COMPLETED", null, null, result, null));
      evidence
          .addObject()
          .put("taskId", id)
          .put("activityId", code)
          .put("agentKey", id == 399 ? "meta-ad-approver" : "customer-agent")
          .put(
              "resultSha256",
              HexFormat.of()
                  .formatHex(
                      MessageDigest.getInstance("SHA-256")
                          .digest(result.getBytes(StandardCharsets.UTF_8))));
      id++;
    }
    history.add(
        new PdeValidationTaskSnapshot(
            394L, 70L, "prototypeCorrection", "COMPLETED", null, null, "{}", null));
    when(cycles.findByExperimentId(experimentId)).thenReturn(Optional.of(cycle));
    when(experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    when(chains.findById(chainId)).thenReturn(Optional.of(chain));
    when(construction.resolve(source, experiment, "pde-construction-approval"))
        .thenReturn(Optional.of(target));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                70L, source))
        .thenAnswer(
            ignored -> {
              gate.setObjectiveEvidenceJson(proof.toString());
              return List.of(gate);
            });
    when(tasks.findPdeValidationTaskSnapshots(source, "pde-construction-approval"))
        .thenReturn(history);
  }

  /** Preserva leitura de provas nas etapas audiovisuais sem conceder autorização comercial. */
  @ParameterizedTest
  @ValueSource(strings = {"VIDEO_BRIEF", "CAMPAIGN_VIDEO", "PDE_ENTRY_VIDEO", "VIDEO_APPROVAL"})
  void exposesCurrentProofThroughoutPrivateVideoPreparation(String stage) {
    cycle.setStage(stage);
    var result = provider.resolve(SOURCE).orElseThrow();
    assertThat(result.get("inputReadiness")).isEqualTo("READY");
    assertThat(result.get("publicationAuthorized")).isEqualTo(false);
    assertThat(result.get("externalMediaSpendAuthorized")).isEqualTo(false);
    assertThat(result.get("paymentEnabled")).isEqualTo(false);
    verify(tasks, never()).save(any());
    verify(instances, never()).save(any());
  }

  /** Liga produtor, adaptador e consumidor reais sem simular a autorização visual do ciclo. */
  @ParameterizedTest
  @CsvSource({
    "7,12,105,26,ADJUSTMENT",
    "7,12,105,26,VIDEO_APPROVAL",
    "91007,91012,91105,91026,ADJUSTMENT",
    "91007,91012,91105,91026,VIDEO_APPROVAL"
  })
  void passesSupplementalCaptureFromCycleThroughVideoContract(
      long productId, long cycleId, long experimentId, long chainId, String stage)
      throws Exception {
    fixture(productId, cycleId, experimentId, chainId);
    cycle.setStage(stage);
    String reference = "experiment:" + experimentId;
    var captured =
        json.createObjectNode()
            .put("contractVersion", "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1")
            .put("captureMomentsContract", "PDE_KIT_CAPTURE_MOMENTS_V1")
            .put("mode", "TECHNICAL")
            .put("decision", "APPROVED")
            .put("sourceReference", reference + "@video-proof-v1")
            .put("productId", productId)
            .put("prototypeVersion", VERSION)
            .put("trafficClass", "AGENT_VALIDATION")
            .put("commercialEvidenceClaimed", false);
    captured
        .putArray("artifacts")
        .addObject()
        .put("artifactId", 91722L)
        .put("sha256", "d".repeat(64));
    var evidence = image(91733L, 91722L, "FULL_PAGE", "d".repeat(64));
    evidence.getTask().setStatus("COMPLETED");
    evidence.getTask().setSourceReference(reference + "@video-proof-v1");
    evidence.getTask().setResultJson(captured.toString());
    var images = mock(AgentTaskVisualEvidenceRepository.class);
    when(images.findByIdAndTaskId(91722L, 91733L)).thenReturn(Optional.of(evidence));
    var storage = mock(AgentTaskVisualEvidenceService.class);
    when(storage.read(91733L, 91722L))
        .thenReturn(
            new AgentTaskVisualEvidenceService.EvidenceContent("image/png", new byte[] {1, 2, 3}));
    var bridge =
        new com.marketinghub.agenttask.AgentTaskVideoProductProofSource(
            images, storage, provider::resolve, json);
    var projects = mock(com.marketinghub.repository.jpa.salesvideo.VideoProjectRepository.class);
    var project =
        com.marketinghub.salesvideo.VideoProject.builder()
            .id(91011L)
            .tenantId("cycle-proof-fixture")
            .productId(productId)
            .experimentId(experimentId)
            .campaignKey(VERSION)
            .build();
    when(projects.findById(91011L)).thenReturn(Optional.of(project));
    var reader =
        new com.marketinghub.salesvideo.service.VideoProductProofService(bridge, projects, json);
    var shot =
        new com.marketinghub.salesvideo.service.postproduction.ProductProofShot(
            "internal://agent-tasks/91733/visual-evidence/91722#crop=72,5020,1035,580", 0, 2);
    try {
      com.marketinghub.salesvideo.tenant.TenantContextHolder.set(
          new com.marketinghub.salesvideo.tenant.TenantContext(
              "cycle-proof-fixture", "fixture", false));
      var sequence = reader.sequence(91011L, List.of(shot), "Veja a legenda.");
      assertThat(sequence.getFirst())
          .containsEntry("taskId", 91733L)
          .containsEntry("sha256", "d".repeat(64))
          .containsEntry("productId", productId)
          .containsEntry("experimentId", experimentId)
          .containsEntry(
              "contentPath", "/api/sales-videos/projects/91011/product-proof/91733/91722")
          .containsEntry("commercialEvidenceClaimed", false);
      assertThat(reader.read(91011L, 91733L, 91722L)).containsExactly(1, 2, 3);
      project.setProductId(productId + 1);
      assertThatThrownBy(() -> reader.sequence(91011L, List.of(shot), "Veja."))
          .hasMessageContaining("mesma versão");
      project.setProductId(productId);
      project.setCampaignKey("outra-versao");
      assertThatThrownBy(() -> reader.sequence(91011L, List.of(shot), "Veja."))
          .hasMessageContaining("mesma versão");
      project.setCampaignKey(VERSION);
      project.setExperimentId(experimentId + 1);
      assertThatThrownBy(() -> reader.sequence(91011L, List.of(shot), "Veja."))
          .hasMessageContaining("mesma versão");
      project.setExperimentId(experimentId);
      gate.setObjectiveAchieved(false);
      assertThatThrownBy(() -> reader.sequence(91011L, List.of(shot), "Veja."))
          .hasMessageContaining("mesma versão");
      gate.setObjectiveAchieved(true);
      cycle.setStatus("CLOSED");
      assertThatThrownBy(() -> reader.sequence(91011L, List.of(shot), "Veja."))
          .hasMessageContaining("mesma versão");
    } finally {
      com.marketinghub.salesvideo.tenant.TenantContextHolder.clear();
    }
    verify(tasks, never()).save(any());
    verify(instances, never()).save(any());
  }

  /** Reproduz produtor de contexto e leitura real dos pixels antes e depois da etapa de vídeo. */
  @ParameterizedTest
  @CsvSource({
    "10,9,102,26,false",
    "91004,91002,91092,91014,false",
    "10,9,102,26,true",
    "91004,91002,91092,91014,true"
  })
  void servesSameApprovedCreativeToVideoReviewerAcrossStageTransition(
      long productId, long cycleId, long experimentId, long chainId, boolean refreshedReport)
      throws Exception {
    fixture(productId, cycleId, experimentId, chainId);
    String source = "experiment:" + experimentId;
    String technical =
        json.writeValueAsString(
            Map.of(
                "contractVersion",
                "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1",
                "productId",
                productId,
                "publicUrl",
                "https://local.example/vega-private",
                "decision",
                "APPROVED",
                "prototypeVersion",
                VERSION,
                "sourceReference",
                source,
                "artifacts",
                List.of(Map.of("artifactId", 910118L, "sha256", "a".repeat(64)))));
    history.set(
        0,
        new PdeValidationTaskSnapshot(
            395L, 70L, "technicalHomologation", "COMPLETED", null, null, technical, null));
    ((ObjectNode) proof.path("taskEvidence").get(0)).put("resultSha256", fixtureHash(technical));
    var process = new BusinessProcessDefinition();
    process.setId(121L);
    process.setProcessCode("creative-production-approval");
    var review = new AgentTask();
    review.setId(723L);
    review.setProcessDefinition(process);
    review.setSourceReference(source);
    review.setProcessActivityId("customer");
    review.setStatus("IN_PROGRESS");
    review.setAssignedAgent(Agent.builder().agentKey("customer-agent").build());
    when(tasks.findById(723L)).thenReturn(Optional.of(review));
    String rendered =
        json.writeValueAsString(
            Map.of(
                "sourceReference",
                source,
                "functionalOutput",
                Map.of(
                    "staticAssets",
                    List.of(Map.of()),
                    "renderedAssets",
                    List.of(
                        Map.of(
                            "artifactId",
                            569L,
                            "sha256",
                            "b".repeat(64),
                            "sourceTaskId",
                            395L,
                            "sourceArtifactId",
                            910118L,
                            "sourceSha256",
                            "a".repeat(64),
                            "prototypeVersion",
                            VERSION,
                            "templateVersion",
                            "PROOF_CARD_V1",
                            "crop",
                            Map.of("x", 0, "y", 0, "width", 800, "height", 500))))));
    when(tasks.findFunctionalSnapshotsByProcessSince(121L, source, null))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    654L,
                    121L,
                    "creative-production-approval",
                    "nonAudiovisual",
                    "communication-director",
                    "COMPLETED",
                    null,
                    null,
                    rendered)));
    var images = mock(AgentTaskVisualEvidenceRepository.class);
    when(images.findByIdAndTaskId(910118L, 395L))
        .thenReturn(Optional.of(image(395L, 910118L, "FULL_PAGE", "a".repeat(64))));
    when(images.findByIdAndTaskId(569L, 654L))
        .thenReturn(Optional.of(image(654L, 569L, "CREATIVE_RENDER", "b".repeat(64))));
    var storage = mock(AgentTaskVisualEvidenceService.class);
    var bytes = new byte[] {1, 2, 3};
    when(storage.read(654L, 569L))
        .thenReturn(new AgentTaskVisualEvidenceService.EvidenceContent("image/png", bytes));
    var reader =
        new CreativeVisualEvidenceService(
            tasks,
            images,
            storage,
            provider::resolve,
            json,
            mock(com.marketinghub.agenttask.CreativeIllustrationSources.class));
    var before = reader.inputs("customer-agent", 723L);
    if (refreshedReport) {
      evidenceSupplementFixture();
      refreshOperationalProof();
    }
    cycle.setStage("VIDEO_APPROVAL");
    assertThat(reader.inputs("customer-agent", 723L)).isEqualTo(before);
    assertThat(reader.read("customer-agent", 723L, 654L, 569L).bytes()).isEqualTo(bytes);
    assertThat(cycle.getStage()).isEqualTo("VIDEO_APPROVAL");
    cycle.setStatus("CLOSED");
    assertThatThrownBy(() -> reader.inputs("customer-agent", 723L))
        .hasMessageContaining("aprovação dos predecessores");
    verify(tasks, never()).save(any());
    verify(instances, never()).save(any());
  }

  /** Mantém autoria e hash da imagem em um armazenamento substituto local. */
  private AgentTaskVisualEvidence image(long taskId, long id, String type, String hash) {
    var owner = new AgentTask();
    owner.setId(taskId);
    var image = new AgentTaskVisualEvidence();
    image.setTask(owner);
    image.setId(id);
    image.setEvidenceType(type);
    image.setSha256(hash);
    return image;
  }

  /** Não reabre leitura de preparação privada nas fases comerciais ou fora do ciclo vigente. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "LEARNING",
        "PLANNING",
        "AUTHORIZATION",
        "PUBLICATION",
        "MEASUREMENT",
        "DECISION",
        "SCALE_AUTHORIZATION"
      })
  void rejectsNonPreparationStage(String stage) {
    cycle.setStage(stage);
    assertThat(provider.resolve(SOURCE).orElseThrow().get("inputReadiness")).isEqualTo("BLOCKED");
  }

  /** Preserva a geração econômica vigente na projeção entregue a Íris. */
  @Test
  void preservesAgentEconomicsContractVersion() {
    pde.put("economicsContractVersion", "PDE_AGENT_ECONOMICS_V1");
    pde.withObject("/marketStrategy").put("contractVersion", "MARKET_STRATEGY_V4");
    var result = provider.resolve(SOURCE).orElseThrow();
    assertThat(result.get("approvedUpstreamArtifacts").toString())
        .contains("PDE_AGENT_ECONOMICS_V1")
        .doesNotContain("PDE_PRIVATE_ECONOMICS_V1");
  }

  /** Entrega V3 nativo, prova atual e limites privados ao mesmo gate que o worker receberá. */
  @Test
  void exposesApprovedPrivateCycleWithoutCommercialPlanAndExportsWorkerInput() throws Exception {
    var result = provider.resolve(SOURCE).orElseThrow();
    assertThat(result.get("inputReadiness")).isEqualTo("READY");
    assertThat(result).doesNotContainKeys("commercialPlanId", "commercialPlanSnapshot");
    assertThat(result.get("publicationAuthorized")).isEqualTo(false);
    var destination = json.valueToTree(result.get("approvedDestination"));
    assertThat(destination.path("type").asText()).isEqualTo("APPROVED_PRIVATE_PDE");
    assertThat(destination.path("prototypeVersion").asText()).isEqualTo(VERSION);
    assertThat(destination.path("url").asText()).isEqualTo("https://local.example/vega-private");
    assertThat(destination.path("requiresLandingGeneration").asBoolean(true)).isFalse();
    var strategy = json.valueToTree(result.get("marketStrategicContract"));
    assertThat(strategy.path("contractVersion").asText()).isEqualTo("MARKET_STRATEGY_V3");
    assertThat(strategy.path("strategistTaskId").asLong()).isEqualTo(359);
    assertThat(strategy.path("contentHash").asText()).matches("[0-9a-f]{64}");
    var normal = mock(com.marketinghub.agenttask.MarketStrategicContextProvider.class);
    var communication =
        mock(com.marketinghub.agenttask.CommunicationMaterializationContextProvider.class);
    when(communication.resolve(SOURCE)).thenReturn(Optional.of(result));
    var readiness =
        new IrisProductProcessActivityReadinessProvider(normal, communication)
            .readiness(null, null, null, SOURCE);
    assertThat(readiness.ready()).isTrue();
    verifyNoInteractions(normal);
    String output = System.getenv("VEGA_IRIS_INPUT_FILE");
    if (output != null) {
      var context =
          Map.of(
              "marketStrategicContract",
              result.get("marketStrategicContract"),
              "communicationMaterializationContext",
              result);
      Files.writeString(
          Path.of(output),
          json.writeValueAsString(
              Map.of(
                  "taskId",
                  910399L,
                  "sourceReference",
                  SOURCE,
                  "processCode",
                  "pde-communication-sales-journey",
                  "activityId",
                  "communicationContract",
                  "processContextJson",
                  json.writeValueAsString(context))));
    }
    verify(tasks, never()).save(any());
    verify(instances, never()).save(any());
  }

  /** Preserva autoria e bytes das provas vigentes sem hidratar a auditoria extensa. */
  @Test
  void communicatesLatestFunctionalProofWithoutHydratingAudit() throws Exception {
    var now = java.time.Instant.parse("2026-09-12T08:00:00Z");
    String result =
        "{\"contractVersion\":\"IRIS_COMMUNICATION_V1\",\"sourceReference\":\"" + SOURCE + "\"}";
    var contract =
        new AgentTaskFunctionalSnapshot(
            402L,
            63L,
            "pde-communication-sales-journey",
            "communicationContract",
            "communication-director",
            "COMPLETED",
            now,
            now,
            result);
    var superseded =
        new AgentTaskFunctionalSnapshot(
            403L,
            64L,
            "creative-production-approval",
            "nonAudiovisual",
            "communication-director",
            "COMPLETED",
            now,
            now,
            result);
    var blocked =
        new AgentTaskFunctionalSnapshot(
            404L,
            64L,
            "creative-production-approval",
            "nonAudiovisual",
            "communication-director",
            "BLOCKED",
            now,
            null,
            null);
    when(tasks.findFunctionalSnapshots(eq(SOURCE), anyCollection(), isNull()))
        .thenReturn(List.of(contract, superseded, blocked));
    var context = provider.resolve(SOURCE).orElseThrow();
    assertThat(context.get("inputReadiness")).isEqualTo("READY");
    var artifacts = json.valueToTree(context.get("communicationArtifacts"));
    assertThat(artifacts).hasSize(1);
    assertThat(artifacts.get(0).path("taskId").asLong()).isEqualTo(402L);
    assertThat(artifacts.get(0).path("agentKey").asText()).isEqualTo(contract.agentKey());
    assertThat(artifacts.get(0).path("processDefinitionId").asLong())
        .isEqualTo(contract.processDefinitionId());
    assertThat(artifacts.get(0).path("resultSha256").asText()).isEqualTo(fixtureHash(result));
    verify(tasks, never()).findBySourceReferenceOrderByCreatedAtAscIdAsc(anyString());
  }

  /**
   * Rejeita identidades divergentes e autorização comercial indevida também na revisão de vídeo.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "productId",
        "prototypeVersion",
        "sourceReference",
        "publicUrl",
        "trafficClass",
        "publicationAuthorized",
        "humanEvidenceClaimed"
      })
  void rejectsChangedGateIdentityOrBoundary(String field) {
    cycle.setStage("VIDEO_APPROVAL");
    if (field.endsWith("Authorized") || field.endsWith("Claimed")) proof.put(field, true);
    else proof.put(field, "divergente");
    assertThat(provider.resolve(SOURCE).orElseThrow().get("availability")).isEqualTo("MISSING");
  }

  /** Nem a revisão audiovisual reutiliza gate após nova tentativa, conteúdo ou planejamento. */
  @ParameterizedTest
  @ValueSource(strings = {"pending", "blocked", "changedResult", "correction", "strategy", "gate"})
  void rejectsSupersededEvidence(String change) {
    cycle.setStage("VIDEO_APPROVAL");
    switch (change) {
      case "pending", "blocked" ->
          history.add(
              new PdeValidationTaskSnapshot(
                  401L, 70L, "psiqueSafety", change.toUpperCase(), null, null, "{}", null));
      case "changedResult" ->
          history.set(
              0,
              new PdeValidationTaskSnapshot(
                  395L,
                  70L,
                  "technicalHomologation",
                  "COMPLETED",
                  null,
                  null,
                  "{\"changed\":true}",
                  null));
      case "correction" ->
          history.add(
              new PdeValidationTaskSnapshot(
                  401L, 70L, "prototypeCorrection", "COMPLETED", null, null, "{}", null));
      case "strategy" -> ((ObjectNode) pde.path("lineage")).put("strategyTaskId", 401L);
      case "gate" -> gate.setObjectiveAchieved(false);
      default -> throw new AssertionError(change);
    }
    assertThat(provider.resolve(SOURCE).orElseThrow().get("inputReadiness")).isEqualTo("BLOCKED");
  }

  /** Preserva provas anteriores somente quando o recibo do gate concilia o novo aceite de Têmis. */
  @Test
  void acceptsEvidenceOnlyResolutionFromGate() throws Exception {
    evidenceSupplementFixture();
    var context = provider.resolve(SOURCE).orElseThrow();
    assertThat(context.get("inputReadiness")).isEqualTo("READY");
    assertThat(context.get("paymentEnabled")).isEqualTo(false);
    assertThat(context.get("publicationAuthorized")).isEqualTo(false);
    assertThat(
            json.valueToTree(context.get("operationalControlEvidence"))
                .path("reportSha256")
                .asText())
        .isEqualTo("a".repeat(64));
    assertThat(
            history.stream().filter(task -> task.id() == 402L).findFirst().orElseThrow().status())
        .isEqualTo("BLOCKED");
  }

  /** Preserva o recibo já aprovado quando outra rodada de testes atualiza o relatório da versão. */
  @ParameterizedTest
  @CsvSource({"10,9,102,26", "91004,91002,91092,91014"})
  void preservesApprovedReceiptAfterCurrentReportRefresh(
      long productId, long cycleId, long experimentId, long chainId) throws Exception {
    fixture(productId, cycleId, experimentId, chainId);
    evidenceSupplementFixture();
    cycle.setStage("VIDEO_APPROVAL");
    refreshOperationalProof();
    var result = provider.resolve("experiment:" + experimentId).orElseThrow();
    assertThat(result.get("inputReadiness")).isEqualTo("READY");
    assertThat(
            json.valueToTree(result.get("operationalControlEvidence"))
                .path("reportSha256")
                .asText())
        .isEqualTo("b".repeat(64));
    assertThat(proof.path("evidenceCorrectionResolution").path("reportSha256").asText())
        .isEqualTo("a".repeat(64));
    verify(tasks, never()).save(any());
    verify(instances, never()).save(any());
  }

  /** Simula catálogo com relatório atual novo e versão anterior imutável referida pelo gate. */
  private void refreshOperationalProof() {
    var reviewed = pde.path("operationalControlEvidence").deepCopy();
    var evidence =
        mock(com.marketinghub.product.service.agentvalidation.PdeOperationalControlEvidence.class);
    when(evidence.resolveReviewed("vega-qa", VERSION, "a".repeat(64)))
        .thenReturn(Optional.of(reviewed));
    org.springframework.test.util.ReflectionTestUtils.setField(
        provider, "operationalEvidence", evidence);
    ((ObjectNode) pde.path("operationalControlEvidence"))
        .put("reportSha256", "b".repeat(64))
        .put("generatedAt", "2026-10-10T08:00:00Z");
  }

  /** Bloqueia recibos ausentes ou divergentes, novas correções e alterações do histórico. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "missing",
        "report",
        "correctionId",
        "correctionHash",
        "reviewId",
        "rejectionHash",
        "activeCorrection",
        "functionalCorrection",
        "timestamp"
      })
  void rejectsUnboundEvidenceCorrectionReceipt(String change) throws Exception {
    evidenceSupplementFixture();
    var resolution = (ObjectNode) proof.path("evidenceCorrectionResolution");
    switch (change) {
      case "missing" -> proof.remove("evidenceCorrectionResolution");
      case "report" ->
          ((ObjectNode) pde.path("operationalControlEvidence")).put("reportSha256", "b".repeat(64));
      case "correctionId" -> resolution.put("correctionTaskId", 401L);
      case "correctionHash" -> resolution.put("correctionResultSha256", "b".repeat(64));
      case "reviewId" -> resolution.put("independentReviewTaskId", 399L);
      case "rejectionHash" -> resolution.put("rejectedResultSha256", "b".repeat(64));
      case "timestamp" -> resolution.put("reportGeneratedAt", "2026-09-12T09:00:00Z");
      case "activeCorrection", "functionalCorrection" ->
          history.add(
              new PdeValidationTaskSnapshot(
                  404L,
                  70L,
                  "prototypeCorrection",
                  change.equals("activeCorrection") ? "IN_PROGRESS" : "COMPLETED",
                  null,
                  null,
                  "{}",
                  null));
      default -> throw new AssertionError(change);
    }
    assertThat(provider.resolve(SOURCE).orElseThrow().get("inputReadiness")).isEqualTo("BLOCKED");
  }

  /**
   * Monta somente dados sintéticos do suplemento, mantendo hashes ligados às tarefas da fixture.
   */
  private void evidenceSupplementFixture() throws Exception {
    String rejected = "{\"decision\":\"BLOCKED\",\"prototypeVersion\":\"" + VERSION + "\"}";
    String correction = "{\"decision\":\"BLOCKED\",\"correctionPlan\":{\"sourceTaskId\":401}}";
    history.add(
        new PdeValidationTaskSnapshot(
            401L,
            70L,
            "commercialIntegrityReview",
            "BLOCKED",
            "FUNCTIONAL_ADJUSTMENT",
            null,
            rejected,
            null));
    history.add(
        new PdeValidationTaskSnapshot(
            402L,
            70L,
            "prototypeCorrection",
            "BLOCKED",
            "FUNCTIONAL_ADJUSTMENT",
            null,
            correction,
            null));
    String review = "{\"decision\":\"APPROVED\",\"prototypeVersion\":\"" + VERSION + "\"}";
    history.add(
        new PdeValidationTaskSnapshot(
            403L, 70L, "commercialIntegrityReview", "COMPLETED", null, null, review, null));
    for (var item : proof.path("taskEvidence")) {
      if ("commercialIntegrityReview".equals(item.path("activityId").asText())) {
        ((ObjectNode) item).put("taskId", 403L).put("resultSha256", fixtureHash(review));
      }
    }
    pde.putObject("operationalControlEvidence")
        .put("prototypeVersion", VERSION)
        .put("reportSha256", "a".repeat(64))
        .put("generatedAt", "2026-09-12T08:00:00Z");
    proof
        .putObject("evidenceCorrectionResolution")
        .put("contractVersion", "PDE_EVIDENCE_CORRECTION_RESOLUTION_V1")
        .put("sourceReference", "experiment:" + cycle.getExperimentId())
        .put("prototypeVersion", VERSION)
        .put("correctionTaskId", 402L)
        .put("correctionResultSha256", fixtureHash(correction))
        .put("rejectedTaskId", 401L)
        .put("rejectedResultSha256", fixtureHash(rejected))
        .put("independentReviewTaskId", 403L)
        .put("reportSha256", "a".repeat(64))
        .put("reportGeneratedAt", "2026-09-12T08:00:00Z");
  }

  /** Calcula o hash exato dos resultados sintéticos que o gate referencia. */
  private String fixtureHash(String result) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(result.getBytes(StandardCharsets.UTF_8)));
  }

  /** Preserva o regime anterior para referências que não são sucessores privados. */
  @Test
  void leavesLegacyReferencesToTheExistingProvider() {
    assertThat(provider.resolve("commercial-plan:3@v1")).isEmpty();
    assertThat(provider.resolve("experiment:1")).isEmpty();
    cycle.setStatus("CLOSED");
    assertThat(provider.resolve(SOURCE).orElseThrow().get("availability")).isEqualTo("MISSING");
  }
}
