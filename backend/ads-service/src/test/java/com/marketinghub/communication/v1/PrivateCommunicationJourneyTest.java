package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
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
    parent.setDiagramJson(
        """
        {"nodes":[{"id":"start","type":"START"},{"id":"communicationContract","type":"TASK"},
        {"id":"creatives","type":"TASK"},{"id":"destination","type":"TASK"},{"id":"integration","type":"TASK"}],
        "flows":[{"from":"start","to":"communicationContract"},{"from":"communicationContract","to":"creatives"},
        {"from":"creatives","to":"destination"},{"from":"destination","to":"integration"}]}
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
            .put("resultSha256", "b".repeat(64));
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
    persisted.add(completed(activity(parent, 910637L, "communicationContract"), "{}"));
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
}
