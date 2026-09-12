package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
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
    cycle.setId(91002L);
    cycle.setProductId(91004L);
    cycle.setExperimentId(91092L);
    cycle.setChainDefinitionId(91014L);
    cycle.setProductVersion(VERSION);
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    var product =
        Product.builder()
            .id(91004L)
            .name("PDE sintético")
            .internalName("Vega QA")
            .slug("vega-qa")
            .build();
    var experiment = new Experiment();
    experiment.setId(91092L);
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
        .put("learningCycleId", 91002L)
        .put("productId", 91004L)
        .put("experimentId", 91092L)
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
            SOURCE,
            91092L,
            91004L,
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
        .put("productId", 91004L)
        .put("productSlug", "vega-qa")
        .put("sourceReference", SOURCE)
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
    when(cycles.findByExperimentId(91092L)).thenReturn(Optional.of(cycle));
    when(experiments.findById(91092L)).thenReturn(Optional.of(experiment));
    when(chains.findById(91014L)).thenReturn(Optional.of(chain));
    when(construction.resolve(SOURCE, experiment, "pde-construction-approval"))
        .thenReturn(Optional.of(target));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                70L, SOURCE))
        .thenAnswer(
            ignored -> {
              gate.setObjectiveEvidenceJson(proof.toString());
              return List.of(gate);
            });
    when(tasks.findPdeValidationTaskSnapshots(SOURCE, "pde-construction-approval"))
        .thenReturn(history);
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

  /** Expõe apenas provas vigentes e mantém auditoria extensa fora da leitura de prontidão. */
  @Test
  void communicatesLatestFunctionalProofWithoutHydratingAudit() {
    var now = java.time.Instant.parse("2026-09-12T08:00:00Z");
    String result =
        "{\"contractVersion\":\"IRIS_COMMUNICATION_V1\",\"sourceReference\":\"" + SOURCE + "\"}";
    var contract =
        new com.marketinghub.agenttask.AgentTaskFunctionalSnapshot(
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
        new com.marketinghub.agenttask.AgentTaskFunctionalSnapshot(
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
        new com.marketinghub.agenttask.AgentTaskFunctionalSnapshot(
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
    verify(tasks, never()).findBySourceReferenceOrderByCreatedAtAscIdAsc(anyString());
  }

  /** Rejeita identidades divergentes e qualquer autorização comercial indevida na prova. */
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
    if (field.endsWith("Authorized") || field.endsWith("Claimed")) proof.put(field, true);
    else proof.put(field, "divergente");
    assertThat(provider.resolve(SOURCE).orElseThrow().get("availability")).isEqualTo("MISSING");
  }

  /** Nenhuma tentativa posterior, alteração de conteúdo ou novo planejamento reutiliza o gate. */
  @ParameterizedTest
  @ValueSource(strings = {"pending", "blocked", "changedResult", "correction", "strategy", "gate"})
  void rejectsSupersededEvidence(String change) {
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

  /** Preserva o regime anterior para referências que não são sucessores privados. */
  @Test
  void leavesLegacyReferencesToTheExistingProvider() {
    assertThat(provider.resolve("commercial-plan:3@v1")).isEmpty();
    assertThat(provider.resolve("experiment:1")).isEmpty();
    cycle.setStatus("CLOSED");
    assertThat(provider.resolve(SOURCE).orElseThrow().get("availability")).isEqualTo("MISSING");
  }
}
