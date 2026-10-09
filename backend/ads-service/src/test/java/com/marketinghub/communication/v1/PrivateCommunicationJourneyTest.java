package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.agenttask.CommunicationMaterializationContextProvider;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.execution.service.humanactivity.StandardHumanProductProcessActivityExecutor;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.planning.service.CommercialPlanApprovedProcessAssetService;
import com.marketinghub.planning.service.CreativeSelectionHumanActivityHandler;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: homologar a jornada privada completa, seus bloqueios e a rota comercial. */
class PrivateCommunicationJourneyTest {
  private String REFERENCE = "experiment:91092";
  private static final String VERSION = "private-pde-v12-test";
  private static final String URL = "https://local.example/private";
  private final ObjectMapper json = new ObjectMapper();
  private final IrisLearningCycleContext context = mock(IrisLearningCycleContext.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final List<BusinessProcessActivityInstance> persisted = new ArrayList<>();
  private final List<AgentTaskFunctionalSnapshot> creativeTasks = new ArrayList<>();
  private final ObjectNode input = json.createObjectNode();
  private final BusinessProcessDefinition parent = new BusinessProcessDefinition();
  private final BusinessProcessDefinition child = new BusinessProcessDefinition();
  private final Product product = Product.builder().id(91004L).slug("local-pde").build();
  private AgentTask communicationTask;
  private final PrivateCommunicationJourney journey =
      new PrivateCommunicationJourney(
          context,
          cycles,
          new ProductProcessActivityPredecessorService(tasks, instances, json),
          instances,
          tasks,
          new PrivateCommunicationCreativeProof(tasks, instances, json),
          json);
  private final CommunicationDestinationActivityExecutor destination =
      new CommunicationDestinationActivityExecutor(journey, processes);
  private BusinessProcessActivityDefinition destinationActivity;
  private BusinessProcessActivityDefinition integrationActivity;

  /**
   * Prepara contratos sintéticos equivalentes à entrada aprovada e um histórico persistível
   * isolado.
   */
  @BeforeEach
  void fixture() throws Exception {
    parent.setId(91063L);
    parent.setProcessCode("pde-communication-sales-journey");
    parent.setVersionNumber(7);
    parent.setDiagramJson(
        """
        {"nodes":[{"id":"start","type":"START"},
        {"id":"communicationContract","type":"TASK","responsibleAgentKeys":["communication-director"],
        "responsibilityDomain":"COMMUNICATION_MATERIALIZATION","executionResourceCode":"iris-communication-worker"},
        {"id":"creatives","type":"TASK","subprocessCode":"creative-production-approval"},
        {"id":"destination","type":"TASK","subprocessCode":"landing-page-generation"},
        {"id":"integration","type":"TASK"},{"id":"gate","type":"GATEWAY"},{"id":"end","type":"END"}],
        "flows":[{"from":"start","to":"communicationContract"},{"from":"communicationContract","to":"creatives"},
        {"from":"creatives","to":"destination"},{"from":"destination","to":"integration"},
        {"from":"integration","to":"gate"},{"from":"gate","to":"end"}]}
        """);
    child.setId(91064L);
    child.setProcessCode("creative-production-approval");
    cycle.setId(91002L);
    cycle.setProductId(product.getId());
    cycle.setExperimentId(91092L);
    cycle.setBaseline(false);
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    when(cycles.findByExperimentId(91092L)).thenReturn(Optional.of(cycle));
    destinationActivity = activity(parent, 910639L, "destination");
    destinationActivity.setSubprocessCode("landing-page-generation");
    integrationActivity = activity(parent, 910640L, "integration");
    input
        .put("availability", "AVAILABLE")
        .put("inputReadiness", "READY")
        .put("sourceReference", REFERENCE)
        .put("prototypeVersion", VERSION)
        .put("mode", IrisLearningCycleContext.MODE)
        .put("cycleId", 91002L)
        .put("chainDefinitionId", 91014L)
        .put("gateInstanceId", 910258L)
        .put("publicationAuthorized", false)
        .put("paymentEnabled", false)
        .put("externalMediaSpendAuthorized", false);
    input.putObject("product").put("id", product.getId()).put("publicUrl", URL);
    input.putObject("validationGate").put("publicUrl", URL).put("prototypeVersion", VERSION);
    input
        .putObject("approvedDestination")
        .put("contractVersion", "PRIVATE_PDE_DESTINATION_V1")
        .put("type", "APPROVED_PRIVATE_PDE")
        .put("url", URL)
        .put("prototypeVersion", VERSION)
        .put("requiresLandingGeneration", false);
    input.putObject("marketStrategicContract").put("contentHash", "a".repeat(64));
    var communication =
        input
            .putArray("communicationArtifacts")
            .addObject()
            .put("taskId", 910402L)
            .put("processDefinitionId", parent.getId())
            .put("activityId", "communicationContract")
            .put("agentKey", "communication-director");
    var output =
        communication
            .putObject("result")
            .put("contractVersion", "IRIS_COMMUNICATION_V1")
            .put("outputType", "COMMUNICATION_PACKAGE")
            .put("executionStatus", "COMPLETED")
            .put("sourceReference", REFERENCE);
    output.putObject("strategicContractReference").put("contentHash", "a".repeat(64));
    output
        .putObject("functionalOutput")
        .put("messageStrategy", "Primeiro ajuste útil e retomável.")
        .putArray("channelBriefings")
        .add("Peça privada com destino homologado.");
    communication.put("resultSha256", sha(output.toString()));
    var technical =
        input
            .putArray("approvedUpstreamArtifacts")
            .addObject()
            .put("taskId", 910395L)
            .put("activityId", "technicalHomologation")
            .put("resultSha256", "c".repeat(64))
            .putObject("result")
            .put("contractVersion", "PDE_AGENT_TECHNICAL_HOMOLOGATION_V1")
            .put("decision", "APPROVED")
            .put("sourceReference", REFERENCE)
            .put("prototypeVersion", VERSION)
            .put("publicUrl", URL);
    var checks = technical.putObject("checks");
    for (String check :
        List.of(
            "sameVersion",
            "desktopAndMobile",
            "happyResultWithinTenMinutes",
            "recoveryPreserved",
            "safetyBlocked",
            "accessibilityBasic",
            "responsiveLayout",
            "privacyPreserved",
            "internalTrafficSegregated",
            "paymentDisabled",
            "publicationDisabled",
            "campaignDisabled",
            "zeroMediaSpend")) checks.put(check, true);
    var producer =
        json.createObjectNode()
            .put("sourceReference", REFERENCE)
            .put("contractVersion", "IRIS_COMMUNICATION_V1")
            .put("outputType", "NON_AUDIOVISUAL_PACKAGE")
            .put("executionStatus", "COMPLETED");
    producer
        .putObject("functionalOutput")
        .putArray("renderedAssets")
        .addObject()
        .put("artifactId", 910126L)
        .put("sha256", "d".repeat(64))
        .put("prototypeVersion", VERSION)
        .put("privateValidation", true);
    creativeTasks.add(
        task(
            910406L, "nonAudiovisual", "communication-director", "COMPLETED", producer.toString()));
    var review = json.createObjectNode().put("decision", "APPROVED");
    review.putArray("requiredChanges");
    review
        .putArray("renderedAssetAudit")
        .addObject()
        .put("artifactId", 910126L)
        .put("sha256", "d".repeat(64));
    creativeTasks.add(task(910407L, "customer", "customer-agent", "COMPLETED", review.toString()));
    creativeTasks.add(
        task(910408L, "commercial", "meta-ad-approver", "COMPLETED", review.toString()));
    var communicationInstance = completed(activity(parent, 910637L, "communicationContract"), "{}");
    persisted.add(communicationInstance);
    communicationTask = new AgentTask();
    communicationTask.setId(910402L);
    communicationTask.setAssignedAgent(
        Agent.builder().id(910401L).agentKey("communication-director").build());
    communicationTask.setProcessDefinition(parent);
    communicationTask.setProcessActivityId("communicationContract");
    communicationTask.setSourceReference(REFERENCE);
    communicationTask.setStatus("COMPLETED");
    communicationTask.setResultJson(output.toString());
    communicationTask.setActivityInstance(communicationInstance);
    persisted.add(
        completed(
            activity(parent, 910638L, "creatives"),
            json.createObjectNode()
                .put("evidenceType", "SUBPROCESS_OBJECTIVE_ACHIEVED_V1")
                .put("childProcessDefinitionId", child.getId())
                .put("sourceReference", REFERENCE)
                .toString()));
    persisted.add(completed(activity(child, 910646L, "human"), "{\"decision\":\"APPROVE\"}"));
    when(context.resolve(REFERENCE))
        .thenAnswer(i -> Optional.of(json.convertValue(input, Map.class)));
    when(tasks.findFunctionalSnapshotsByProcessSince(child.getId(), REFERENCE, null))
        .thenReturn(creativeTasks);
    when(tasks.findById(communicationTask.getId())).thenReturn(Optional.of(communicationTask));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                anyLong(), eq(REFERENCE)))
        .thenAnswer(
            i ->
                persisted.stream()
                    .filter(
                        p ->
                            p.getActivityDefinition()
                                .getProcessDefinition()
                                .getId()
                                .equals(i.getArgument(0)))
                    .toList());
    org.mockito.stubbing.Answer<Optional<BusinessProcessActivityInstance>> latestOccurrence =
        i ->
            persisted.stream()
                .filter(p -> p.getActivityDefinition().getId().equals(i.getArgument(0)))
                .max(Comparator.comparing(BusinessProcessActivityInstance::getOccurrenceNumber));
    when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), eq(REFERENCE)))
        .thenAnswer(latestOccurrence);
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), eq(REFERENCE)))
        .thenAnswer(latestOccurrence);
    when(instances.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              var value = (BusinessProcessActivityInstance) i.getArgument(0);
              value.setId(910500L + persisted.size());
              persisted.add(value);
              return value;
            });
  }

  /**
   * Comprova o caminho completo sem criar landing, repetir tarefas ou usar plano comercial
   * histórico.
   */
  @Test
  void completesDestinationThenIntegrationWithoutDuplicatingArtifacts() throws Exception {
    assertThat(journey.readiness(parent, integrationActivity, product, REFERENCE).ready())
        .isFalse();
    var route = destination.readiness(parent, destinationActivity, product, REFERENCE);
    assertThat(route.ready()).isTrue();
    assertThat(route.targetProcessDefinitionId()).isNull();
    assertThat(route.navigationUrl()).isEqualTo(URL);
    assertThat(
            destination
                .execute(parent, destinationActivity, product, REFERENCE)
                .objectiveAchieved())
        .isTrue();
    assertThat(
            journey.complete(parent, integrationActivity, product, REFERENCE).objectiveAchieved())
        .isTrue();
    journey.complete(parent, integrationActivity, product, REFERENCE);
    assertThat(persisted).hasSize(5);
    var proof = json.readTree(persisted.getLast().getObjectiveEvidenceJson());
    assertThat(proof.path("checkoutMode").asText()).isEqualTo("SIMULATED");
    assertThat(proof.path("creativeApproval").path("producerTaskId").asLong()).isEqualTo(910406L);
    assertThat(proof.path("commercialEvidenceClaimed").asBoolean()).isFalse();
    assertThat(proof.path("publicationAuthorized").asBoolean()).isFalse();
    assertThat(journey.stale(parent, integrationActivity, product, REFERENCE)).isFalse();
    verifyNoInteractions(processes);
    verify(tasks, never()).save(any());
  }

  /** Consome a projeção real do ciclo até a integração, sem acrescentar campos na fixture. */
  @ParameterizedTest
  @ValueSource(strings = {"MARKET_STRATEGY_V3", "MARKET_STRATEGY_V4"})
  void completesJourneyUsingTheActualCycleContextProducer(String strategyVersion) throws Exception {
    var provider = projectedCycleContext(strategyVersion);
    when(context.resolve(REFERENCE)).thenAnswer(ignored -> provider.resolve(REFERENCE));
    var route = destination.readiness(parent, destinationActivity, product, REFERENCE);
    assertThat(route.ready()).as(route.reason()).isTrue();
    assertThat(route.navigationUrl()).isEqualTo(URL);
    assertThat(route.targetProcessDefinitionId()).isNull();
    assertThat(
            destination
                .execute(parent, destinationActivity, product, REFERENCE)
                .objectiveAchieved())
        .isTrue();
    assertThat(
            journey.complete(parent, integrationActivity, product, REFERENCE).objectiveAchieved())
        .isTrue();
    journey.complete(parent, integrationActivity, product, REFERENCE);
    assertThat(persisted).hasSize(5);
    assertThat(persisted.getLast().getKnownCostUsd()).isZero();
    assertThat(journey.stale(parent, integrationActivity, product, REFERENCE)).isFalse();
    var proof = json.readTree(persisted.getLast().getObjectiveEvidenceJson());
    assertThat(proof.path("communicationTaskId").asLong()).isEqualTo(communicationTask.getId());
    assertThat(proof.path("communicationSha256").asText())
        .isEqualTo(sha(communicationTask.getResultJson()));
    assertThat(proof.path("checkoutMode").asText()).isEqualTo("SIMULATED");
    assertThat(proof.path("publicationAuthorized").asBoolean()).isFalse();
    assertThat(proof.path("commercialEvidenceClaimed").asBoolean()).isFalse();
    verifyNoInteractions(processes);
    verify(tasks, never()).save(any());
  }

  /** Monta as fontes do produtor com tarefas persistidas e cinco pareceres do mesmo contexto. */
  private IrisLearningCycleContext projectedCycleContext(String strategyVersion) throws Exception {
    var strategy =
        json.createObjectNode()
            .put("contractVersion", strategyVersion)
            .put("valueMechanism", "Resultado sintético útil e recuperável.");
    var output = (ObjectNode) json.readTree(communicationTask.getResultJson());
    output.withObject("/strategicContractReference").put("contentHash", sha(strategy.toString()));
    communicationTask.setResultJson(output.toString());
    when(tasks.findFunctionalSnapshots(eq(REFERENCE), anyCollection(), isNull()))
        .thenReturn(
            List.of(
                new AgentTaskFunctionalSnapshot(
                    communicationTask.getId(),
                    parent.getId(),
                    parent.getProcessCode(),
                    communicationTask.getProcessActivityId(),
                    communicationTask.getAssignedAgent().getAgentKey(),
                    communicationTask.getStatus(),
                    Instant.parse("2026-09-12T08:00:00Z"),
                    Instant.parse("2026-09-12T08:01:00Z"),
                    communicationTask.getResultJson())));
    var constructionProcess = new BusinessProcessDefinition();
    constructionProcess.setId(91070L);
    constructionProcess.setProcessCode("pde-construction-approval");
    var chain = new com.marketinghub.businessprocesschain.BusinessProcessChainDefinition();
    var item = new com.marketinghub.businessprocesschain.BusinessProcessChainItem();
    item.setProcessDefinition(constructionProcess);
    chain.setItems(List.of(item));
    var gate = completed(activity(constructionProcess, 910650L, "agentValidationGate"), "{}");
    var gateProof =
        json.createObjectNode()
            .put("evidenceType", "PDE_AGENT_VALIDATION_GATE_V1")
            .put("sourceReference", REFERENCE)
            .put("productId", product.getId())
            .put("productSlug", product.getSlug())
            .put("prototypeVersion", VERSION)
            .put("publicUrl", URL)
            .put("trafficClass", "AGENT_VALIDATION")
            .put("internalMarker", "mh_internal_test")
            .put("humanEvidenceClaimed", false)
            .put("commercialEvidenceClaimed", false)
            .put("paymentEnabled", false)
            .put("publicationAuthorized", false)
            .put("campaignAuthorized", false)
            .put("mediaSpendAuthorizedBrl", 0);
    var reviews =
        new ArrayList<com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot>();
    var evidence = gateProof.putArray("taskEvidence");
    long taskId = 910395L;
    for (String code :
        List.of(
            "technicalHomologation",
            "psiqueAdherent",
            "psiqueRecovery",
            "psiqueSafety",
            "commercialIntegrityReview")) {
      String result =
          "technicalHomologation".equals(code)
              ? input.path("approvedUpstreamArtifacts").get(0).path("result").toString()
              : "{\"decision\":\"APPROVED\"}";
      reviews.add(
          new com.marketinghub.product.service.agentvalidation.PdeValidationTaskSnapshot(
              taskId, constructionProcess.getId(), code, "COMPLETED", null, null, result, null));
      evidence
          .addObject()
          .put("taskId", taskId++)
          .put("activityId", code)
          .put(
              "agentKey",
              "commercialIntegrityReview".equals(code) ? "meta-ad-approver" : "customer-agent")
          .put("resultSha256", sha(result));
    }
    gate.setObjectiveEvidenceJson(gateProof.toString());
    var pde = json.createObjectNode();
    pde.set("marketStrategy", strategy);
    pde.putObject("lineage")
        .put("learningCycleId", cycle.getId())
        .put("strategyTaskId", 910359L)
        .put("economicsTaskId", 910361L)
        .put("architectureTaskId", 910362L);
    pde.putObject("economics").put("commercialSpendAuthorized", false);
    pde.putObject("harness").put("prototypeObjective", "Resultado privado.");
    pde.putObject("privatePrototypeAcceptance")
        .put("prototypeVersion", VERSION)
        .put("privateAccessUrl", URL);
    cycle.setChainDefinitionId(91014L);
    cycle.setProductVersion(VERSION);
    var experiment = new com.marketinghub.experiment.Experiment();
    experiment.setId(cycle.getExperimentId());
    experiment.setProduct(product);
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PLANNED);
    var experiments = mock(com.marketinghub.repository.jpa.experiment.ExperimentRepository.class);
    when(experiments.findById(experiment.getId())).thenReturn(Optional.of(experiment));
    var chains =
        mock(
            com.marketinghub.repository.jpa.businessprocesschain
                .BusinessProcessChainDefinitionRepository.class);
    when(chains.findById(cycle.getChainDefinitionId())).thenReturn(Optional.of(chain));
    var construction =
        mock(
            com.marketinghub.businessprocesschain.learningcycle.v1.service
                .LearningCycleConstructionContext.class);
    when(construction.resolve(REFERENCE, experiment, constructionProcess.getProcessCode()))
        .thenReturn(
            Optional.of(
                new AgentTaskTargetResponse(
                    REFERENCE,
                    experiment.getId(),
                    product.getId(),
                    product.getSlug(),
                    "PDE de teste",
                    "Outro produto QA",
                    VERSION,
                    URL,
                    null,
                    null,
                    null,
                    null,
                    pde)));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                constructionProcess.getId(), REFERENCE))
        .thenReturn(List.of(gate));
    when(tasks.findPdeValidationTaskSnapshots(REFERENCE, constructionProcess.getProcessCode()))
        .thenReturn(reviews);
    return new IrisLearningCycleContext(
        cycles, experiments, chains, construction, instances, tasks, json);
  }

  /**
   * A decisão privada real persiste a peça revisada e permite o retorno ao pai sem plano ou modelo.
   */
  @Test
  void privateHumanSelectionCompletesDestinationAndIntegrationWithoutCommercialImport()
      throws Exception {
    persisted.removeIf(i -> "human".equals(i.getActivityDefinition().getActivityId()));
    var human = activity(child, 910646L, "human");
    human.setOwnerName("Operador humano");
    var materialization = mock(CommunicationMaterializationContextProvider.class);
    input
        .putObject(PrivateCreativePreparationContext.FIELD)
        .put("contractVersion", PrivateCreativePreparationContext.VERSION)
        .put("scope", "PRIVATE_PREPARATION")
        .put("sourceReference", REFERENCE)
        .put("prototypeVersion", VERSION)
        .put("nonAudiovisualEvidenceRequired", true)
        .put("publicationAuthorized", false)
        .put("spendAuthorized", false)
        .put("commercialEvidenceClaimed", false);
    when(materialization.resolve(REFERENCE))
        .thenAnswer(i -> Optional.of(json.convertValue(input, Map.class)));
    var proof = new PrivateCommunicationCreativeProof(tasks, instances, json);
    var selection = new PrivateCreativeSelection(materialization, proof, json);
    var assets = mock(CommercialPlanApprovedProcessAssetService.class);
    var handler = new CreativeSelectionHumanActivityHandler(assets, selection);
    var predecessors = mock(ProductProcessActivityPredecessorService.class);
    when(predecessors.readiness(child, human, REFERENCE))
        .thenReturn(
            new com.marketinghub.businessprocess.execution.service.predecessor
                .ProductProcessActivityPredecessorReadiness(true, "Pareceres concluídos."));
    var executor =
        new StandardHumanProductProcessActivityExecutor(
            instances, predecessors, json, List.of(handler));
    assertThat(journey.readiness(parent, destinationActivity, product, REFERENCE).ready())
        .isFalse();
    var readiness = executor.readiness(child, human, product, REFERENCE);
    assertThat(readiness.ready()).isTrue();
    var request =
        new ProductProcessActivityExecutionRequest(
            "APPROVE", null, null, null, readiness.confirmationToken(), Map.of());
    assertThat(executor.execute(child, human, product, REFERENCE, request).objectiveAchieved())
        .isTrue();
    var decision = persisted.getLast();
    assertThat(
            json.readTree(decision.getObjectiveEvidenceJson())
                .path("structuredEvidence")
                .path("evidenceType")
                .asText())
        .isEqualTo("PDE_PRIVATE_CREATIVE_SELECTION_V1");
    assertThat(decision.getKnownCostUsd()).isZero();
    var restarted =
        new StandardHumanProductProcessActivityExecutor(
            instances, predecessors, json, List.of(handler));
    assertThatThrownBy(() -> restarted.execute(child, human, product, REFERENCE, request))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("decisão ativa ou concluída");
    assertThat(
            destination
                .execute(parent, destinationActivity, product, REFERENCE)
                .objectiveAchieved())
        .isTrue();
    assertThat(
            journey.complete(parent, integrationActivity, product, REFERENCE).objectiveAchieved())
        .isTrue();
    assertThat(persisted).hasSize(5);
    assertThat(
            json.readTree(persisted.getLast().getObjectiveEvidenceJson())
                .path("creativeApproval")
                .path("humanDecisionInstanceId")
                .asLong())
        .isEqualTo(decision.getId());
    verifyNoInteractions(assets);
    verify(tasks, never()).save(any());
  }

  /** Recusa lacunas causais antes de persistir qualquer conclusão. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "product",
        "reference",
        "gate",
        "url",
        "version",
        "strategy",
        "events",
        "payment",
        "communication",
        "taskHash",
        "taskResult",
        "taskAgent",
        "artifactAgent",
        "missingArtifactAgent",
        "taskInstance",
        "taskProcess",
        "creativeReview",
        "human"
      })
  void rejectsBrokenLineageAndMissingApprovals(String failure) {
    switch (failure) {
      case "product" -> ((ObjectNode) input.path("product")).put("id", 91099L);
      case "reference" -> input.put("sourceReference", "experiment:91999");
      case "gate" -> input.put("inputReadiness", "BLOCKED");
      case "url" ->
          ((ObjectNode) input.path("approvedDestination")).put("url", "https://other.example");
      case "version" -> input.put("prototypeVersion", "stale-version");
      case "strategy" ->
          ((ObjectNode) input.path("marketStrategicContract")).put("contentHash", "e".repeat(64));
      case "events" ->
          ((ObjectNode)
                  input.path("approvedUpstreamArtifacts").get(0).path("result").path("checks"))
              .put("internalTrafficSegregated", false);
      case "payment" -> input.put("paymentEnabled", true);
      case "communication" -> input.putArray("communicationArtifacts");
      case "taskHash" ->
          ((ObjectNode) input.path("communicationArtifacts").get(0))
              .put("resultSha256", "e".repeat(64));
      case "taskResult" -> communicationTask.setResultJson("{}");
      case "taskAgent" -> communicationTask.getAssignedAgent().setAgentKey("another-agent");
      case "artifactAgent" ->
          ((ObjectNode) input.path("communicationArtifacts").get(0))
              .put("agentKey", "another-agent");
      case "missingArtifactAgent" ->
          ((ObjectNode) input.path("communicationArtifacts").get(0)).remove("agentKey");
      case "taskInstance" -> communicationTask.getActivityInstance().setStatus("BLOCKED");
      case "taskProcess" ->
          communicationTask.getProcessDefinition().setProcessCode("other-process");
      case "creativeReview" ->
          creativeTasks.add(task(910409L, "commercial", "meta-ad-approver", "BLOCKED", "{}"));
      case "human" -> persisted.get(2).setStatus("BLOCKED");
      default -> throw new IllegalArgumentException(failure);
    }
    assertThat(destination.readiness(parent, destinationActivity, product, REFERENCE).ready())
        .isFalse();
    assertThatThrownBy(() -> destination.execute(parent, destinationActivity, product, REFERENCE))
        .isInstanceOf(IllegalStateException.class);
    assertThat(persisted).hasSize(3);
  }

  /** Mantém uma tentativa em curso até sua resposta, mesmo quando a landing não será usada. */
  @Test
  void waitsForInFlightLandingWithoutCancellingIt() {
    when(tasks.findFunctionalSnapshots(eq(REFERENCE), anySet(), isNull()))
        .thenReturn(
            List.of(task(910410L, "select", "communication-director", "IN_PROGRESS", null)));
    assertThat(destination.readiness(parent, destinationActivity, product, REFERENCE).reason())
        .contains("em curso");
    assertThat(persisted).hasSize(3);
  }

  /** Reabre a conclusão quando um parecer posterior invalida a peça utilizada. */
  @Test
  void detectsApprovalSupersededAfterDestinationCompleted() {
    destination.execute(parent, destinationActivity, product, REFERENCE);
    creativeTasks.add(task(910411L, "commercial", "meta-ad-approver", "BLOCKED", "{}"));
    assertThat(journey.stale(parent, destinationActivity, product, REFERENCE)).isTrue();
    assertThat(journey.readiness(parent, integrationActivity, product, REFERENCE).ready())
        .isFalse();
  }

  /** Mantém provas históricas quando o ciclo encerra ou avança, sem autorizar novas escritas. */
  @ParameterizedTest
  @ValueSource(strings = {"ADJUSTED", "MEASUREMENT"})
  void preservesCompletedEvidenceAfterPrivatePreparationEnds(String next) {
    destination.execute(parent, destinationActivity, product, REFERENCE);
    journey.complete(parent, integrationActivity, product, REFERENCE);
    if ("ADJUSTED".equals(next)) {
      cycle.setStatus(next);
      cycle.setClosedAt(Instant.parse("2026-09-12T13:00:00Z"));
    } else cycle.setStage(next);
    when(context.resolve(REFERENCE))
        .thenReturn(
            Optional.of(
                Map.of(
                    "mode",
                    IrisLearningCycleContext.MODE,
                    "availability",
                    "MISSING",
                    "inputReadiness",
                    "BLOCKED",
                    "reason",
                    "Ciclo privado não aberto.")));
    assertThat(journey.stale(parent, destinationActivity, product, REFERENCE)).isFalse();
    assertThat(journey.stale(parent, integrationActivity, product, REFERENCE)).isFalse();
    var archived = journey.readiness(parent, integrationActivity, product, REFERENCE);
    assertThat(archived.ready()).isFalse();
    assertThat(archived.navigationUrl()).isEqualTo(URL);
    assertThatThrownBy(() -> journey.complete(parent, integrationActivity, product, REFERENCE))
        .hasMessageContaining("Ciclo privado não aberto");
    assertThat(persisted).hasSize(5);
  }

  /** Preserva a geração de landing para o contrato comercial e recusa execução como atalho. */
  @Test
  void delegatesCommercialLandingAndNeverFallsBackFromBlockedPrivateCycle() {
    var landing = new BusinessProcessDefinition();
    landing.setId(91065L);
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "landing-page-generation", "PUBLISHED"))
        .thenReturn(Optional.of(landing));
    assertThat(
            destination
                .readiness(parent, destinationActivity, product, "commercial-plan:3@v7")
                .targetProcessDefinitionId())
        .isEqualTo(91065L);
    assertThatThrownBy(
            () -> destination.execute(parent, destinationActivity, product, "commercial-plan:3@v7"))
        .hasMessageContaining("subprocesso oficial");
    input.put("availability", "MISSING");
    var privateRoute = destination.readiness(parent, destinationActivity, product, REFERENCE);
    assertThat(privateRoute.ready()).isFalse();
    assertThat(privateRoute.targetProcessDefinitionId()).isNull();
  }

  /** O produto validado conclui destino e integração sem fabricar ciclo ou experimento. */
  @Test
  void completesPrivateProductJourneyAndRejectsRevokedApproval() throws Exception {
    persisted.clear();
    creativeTasks.clear();
    input.removeAll();
    reset(instances, tasks);
    REFERENCE = "product:" + product.getId() + "@agent-validation-v1";
    fixture();
    input.put("mode", IrisPrivateProductContext.MODE);
    input.remove(List.of("cycleId", "chainDefinitionId"));
    var privateProducts = mock(IrisPrivateProductContext.class);
    when(privateProducts.resolve(REFERENCE))
        .thenAnswer(i -> Optional.of(json.convertValue(input, Map.class)));
    org.springframework.test.util.ReflectionTestUtils.setField(
        journey, "privateProducts", privateProducts);
    assertThat(journey.applies(REFERENCE)).isTrue();
    assertThat(destination.readiness(parent, destinationActivity, product, REFERENCE).ready())
        .isTrue();
    destination.execute(parent, destinationActivity, product, REFERENCE);
    journey.complete(parent, integrationActivity, product, REFERENCE);
    journey.complete(parent, integrationActivity, product, REFERENCE);
    assertThat(persisted).hasSize(5);
    var evidence = json.readTree(persisted.getLast().getObjectiveEvidenceJson());
    assertThat(evidence.path("mode").asText()).isEqualTo("PRODUCT_PRIVATE");
    assertThat(evidence.path("sourceReference").asText()).isEqualTo(REFERENCE);
    assertThat(evidence.path("cycleId").isMissingNode() || evidence.path("cycleId").isNull())
        .isTrue();
    input.put("inputReadiness", "BLOCKED");
    assertThat(journey.stale(parent, integrationActivity, product, REFERENCE)).isTrue();
    assertThat(journey.readiness(parent, integrationActivity, product, REFERENCE).ready())
        .isFalse();
  }

  /** Reutiliza a comunicação v7 na revisão editorial v8 com origem e compatibilidade explícitas. */
  @Test
  void reusesCompatibleCommunicationRevisionWithoutRepeatingAgentTask() throws Exception {
    persisted.clear();
    creativeTasks.clear();
    input.removeAll();
    reset(instances, tasks);
    REFERENCE = "product:" + product.getId() + "@agent-validation-v1";
    fixture();
    input.put("mode", IrisPrivateProductContext.MODE);
    input.remove(List.of("cycleId", "chainDefinitionId"));
    var source = new BusinessProcessDefinition();
    source.setId(91063L);
    source.setProcessCode(parent.getProcessCode());
    source.setVersionNumber(7);
    source.setDiagramJson(parent.getDiagramJson());
    communicationTask.setProcessDefinition(source);
    communicationTask.getActivityInstance().getActivityDefinition().setProcessDefinition(source);
    ((ObjectNode) input.path("communicationArtifacts").get(0))
        .put("processDefinitionId", source.getId());
    parent.setId(91085L);
    parent.setVersionNumber(8);
    ObjectNode targetDiagram = (ObjectNode) json.readTree(parent.getDiagramJson());
    targetDiagram.put("commercialCriteriaVersion", "PDE_COMMERCIAL_PRINCIPLES_V1");
    parent.setDiagramJson(targetDiagram.toString());
    var privateProducts = mock(IrisPrivateProductContext.class);
    when(privateProducts.resolve(REFERENCE))
        .thenAnswer(i -> Optional.of(json.convertValue(input, Map.class)));
    org.springframework.test.util.ReflectionTestUtils.setField(
        journey, "privateProducts", privateProducts);

    assertThat(destination.readiness(parent, destinationActivity, product, REFERENCE).ready())
        .isTrue();
    destination.execute(parent, destinationActivity, product, REFERENCE);
    journey.complete(parent, integrationActivity, product, REFERENCE);

    var destinationEvidence =
        json.readTree(
            persisted.stream()
                .filter(
                    instance ->
                        "destination".equals(instance.getActivityDefinition().getActivityId()))
                .findFirst()
                .orElseThrow()
                .getObjectiveEvidenceJson());
    assertThat(destinationEvidence.path("communicationTaskId").asLong()).isEqualTo(910402L);
    assertThat(destinationEvidence.path("predecessors").get(0).path("reuseContract").asText())
        .isEqualTo("PDE_COMMUNICATION_COMPATIBLE_REVISION_V1");
    assertThat(
            destinationEvidence
                .path("predecessors")
                .get(0)
                .path("sourceProcessDefinitionId")
                .asLong())
        .isEqualTo(91063L);
    assertThat(
            destinationEvidence
                .path("predecessors")
                .get(0)
                .path("targetProcessDefinitionId")
                .asLong())
        .isEqualTo(91085L);
    verify(tasks, never()).save(any());
  }

  /**
   * Preserva as provas privadas, mas as recusa como prontidão da revisão comercial do Instagram.
   */
  @Test
  void rejectsPrivateJourneyAfterPaidInstagramPolicyBecomesCurrent() throws Exception {
    REFERENCE = "product:" + product.getId() + "@agent-validation-v1";
    var privateProducts = mock(IrisPrivateProductContext.class);
    when(privateProducts.resolve(REFERENCE))
        .thenAnswer(i -> Optional.of(json.convertValue(input, Map.class)));
    org.springframework.test.util.ReflectionTestUtils.setField(
        journey, "privateProducts", privateProducts);
    ObjectNode diagram = (ObjectNode) json.readTree(parent.getDiagramJson());
    diagram.put("commercialAcquisitionPolicyVersion", "PAID_INSTAGRAM_ONLY_V1");
    parent.setDiagramJson(diagram.toString());

    var readiness = journey.readiness(parent, destinationActivity, product, REFERENCE);
    var provider = new PrivateCommunicationActivityReadinessProvider(journey);

    assertThat(journey.incompatibleCommercialPolicy(parent, REFERENCE)).isTrue();
    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("histórica", "Instagram Ads");
    assertThat(provider.supports(parent, activity(parent, 910638L, "communicationContract")))
        .isTrue();
    assertThat(provider.supports(parent, activity(parent, 910641L, "creatives"))).isTrue();
    assertThat(provider.readiness(parent, integrationActivity, product, REFERENCE).ready())
        .isFalse();
    assertThat(provider.requiresFreshExecution(parent, integrationActivity, product, REFERENCE))
        .isTrue();
  }

  /** Cria uma definição isolada mantendo o processo proprietário. */
  private BusinessProcessActivityDefinition activity(
      BusinessProcessDefinition process, long id, String code) {
    var value = new BusinessProcessActivityDefinition();
    value.setId(id);
    value.setProcessDefinition(process);
    value.setActivityId(code);
    return value;
  }

  /** Representa uma prova anterior concluída e auditável na referência da fixture. */
  private BusinessProcessActivityInstance completed(
      BusinessProcessActivityDefinition activity, String proof) {
    var value = new BusinessProcessActivityInstance();
    value.setId(activity.getId());
    value.setActivityDefinition(activity);
    value.setSourceReference(REFERENCE);
    value.setOccurrenceNumber(1);
    value.setStatus("COMPLETED");
    value.setObjectiveAchieved(true);
    value.setObjectiveEvidenceJson(proof);
    value.setExitedAt(Instant.parse("2026-09-12T12:00:00Z"));
    return value;
  }

  /** Representa uma tarefa sintética de produção ou revisão preservando identidade e horários. */
  private AgentTaskFunctionalSnapshot task(
      long id, String activity, String agent, String status, String result) {
    return new AgentTaskFunctionalSnapshot(
        id,
        child.getId(),
        child.getProcessCode(),
        activity,
        agent,
        status,
        Instant.parse("2026-09-12T11:00:00Z"),
        Instant.parse("2026-09-12T11:10:00Z"),
        result);
  }

  /** Calcula a identidade dos bytes persistidos usados pela projeção do contexto privado. */
  private String sha(String value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
