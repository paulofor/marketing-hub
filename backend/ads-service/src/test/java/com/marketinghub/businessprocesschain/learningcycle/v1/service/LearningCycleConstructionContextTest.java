package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: reproduzir sucessor sobre produto legado sem perder contratos ou isolamento.
 */
class LearningCycleConstructionContextTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final LearningCycleConstructionContext resolver =
      new LearningCycleConstructionContext(cycles, chains, tasks, mapper);
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final Product product =
      Product.builder()
          .id(4L)
          .slug("vega-test")
          .validationDefinitionVersion("v1")
          .pdeExperienceJson("{\"experienceVersion\":\"historical-v7\"}")
          .publicUrl("https://historical.invalid")
          .build();
  private final Experiment experiment = Experiment.builder().id(92L).product(product).build();
  private List<AgentTask> approved;

  /**
   * Prepara aprovações sintéticas e uma referência histórica que jamais deve substituir o sucessor.
   */
  @BeforeEach
  void setup() {
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setChainDefinitionId(14L);
    cycle.setPreviousCycleId(1L);
    cycle.setProductVersion("successor-v8");
    cycle.setStatus("OPEN");
    cycle.setCreatedAt(Instant.parse("2026-09-09T00:00:00Z"));
    cycle.setInheritedLearningJson(
        "{\"cycleId\":1,\"experimentId\":91,\"limitation\":\"Amostra pequena; sem causa comprovada\"}");
    cycle.setBriefJson("{\"hypothesis\":\"Melhorar primeiro resultado útil\"}");
    when(cycles.findByExperimentId(92L)).thenReturn(Optional.of(cycle));
    var process = new BusinessProcessDefinition();
    process.setId(67L);
    process.setProcessCode("pde-commercial-plan-offer");
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(process);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(new ArrayList<>(List.of(item)));
    when(chains.findById(14L)).thenReturn(Optional.of(chain));
    approved =
        new ArrayList<>(
            List.of(
                task(
                    362,
                    "productArchitecture",
                    "landing-generator",
                    "{\"decision\":\"APPROVE\",\"productArchitecture\":{\"privatePrototype\":{\"simpleInput\":\"Ocasião e peça\"}}}"),
                task(
                    361,
                    "economics",
                    "financial-agent",
                    "{\"contractVersion\":\"PDE_PRIVATE_ECONOMICS_V1\",\"decision\":\"APPROVE\",\"economics\":{\"commercialSpendAuthorized\":false},\"metrics\":{\"primary\":\"Uso\"}}"),
                task(
                    359,
                    "marketStrategy",
                    "experiment-strategist",
                    "{\"decision\":\"APPROVE\",\"marketStrategicContract\":{\"contractVersion\":\"MARKET_STRATEGY_V3\",\"status\":\"READY_FOR_PRIVATE_VALIDATION\",\"privateValidationPlan\":{}}}")));
    when(tasks.findFunctionalSnapshotsByProcessSince(67L, "experiment:92", cycle.getCreatedAt()))
        .thenAnswer(
            ignored ->
                approved.stream()
                    .map(
                        value ->
                            new com.marketinghub.agenttask.AgentTaskFunctionalSnapshot(
                                value.getId(),
                                67L,
                                "pde-commercial-plan-offer",
                                value.getProcessActivityId(),
                                value.getAssignedAgent().getAgentKey(),
                                value.getStatus(),
                                value.getCreatedAt(),
                                value.getDeliveredAt(),
                                value.getResultJson()))
                    .toList());
  }

  /** Cria uma entrega pertencente ao especialista e com ordem temporal verificável. */
  private AgentTask task(long id, String activity, String agent, String result) {
    var task = new AgentTask();
    task.setId(id);
    task.setStatus("COMPLETED");
    task.setProcessActivityId(activity);
    task.setAssignedAgent(Agent.builder().agentKey(agent).build());
    task.setResultJson(result);
    task.setDeliveredAt(cycle.getCreatedAt().plusSeconds(id));
    return task;
  }

  /**
   * Confirma que a versão e as provas vêm do ciclo sem modificar produto, checkout ou histórico.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "pde-construction-approval",
        "pde-communication-sales-journey",
        "creative-production-approval"
      })
  void buildsSuccessorWithExactApprovalsAndInheritedLearning(String processCode) {
    var result = resolver.resolve("experiment:92", experiment, processCode).orElseThrow();
    assertThat(result.experienceVersion()).isEqualTo("successor-v8");
    var plan = result.pdeContext().path("agentValidationPlan");
    assertThat(plan.path("sourceReference").asText()).isEqualTo("experiment:92");
    assertThat(plan.path("contractVersion").asText()).isEqualTo("PDE_AGENT_VALIDATION_V1");
    assertThat(plan.path("requiredScenarios").size()).isEqualTo(3);
    assertThat(plan.path("requiredDevices").size()).isEqualTo(3);
    assertThat(plan.path("humanEvidenceClaimed").asBoolean()).isFalse();
    assertThat(result.publicUrl()).isNull();
    assertThat(result.commercialCheckoutUrl()).isNull();
    assertThat(result.pdeContext().path("lineage").path("architectureTaskId").asLong())
        .isEqualTo(362);
    assertThat(result.pdeContext().path("inheritedLearning").path("experimentId").asLong())
        .isEqualTo(91);
    assertThat(result.pdeContext().path("cycleBrief").path("hypothesis").asText())
        .contains("primeiro resultado");
    assertThat(product.getPdeExperienceJson()).contains("historical-v7");
    verify(cycles, never()).save(any());
  }

  /** Entrega a prova compilada por versão, preservando a identidade corrente e o limite local. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {10L, 97001L})
  void carriesOperationalEvidenceWithoutClaimingAgentApproval(long productId) {
    product.setId(productId);
    product.setSlug("pde-planejado-36");
    cycle.setProductId(productId);
    cycle.setProductVersion("mira-private-candidate-v3");
    var evidence =
        new com.marketinghub.product.service.agentvalidation.PdeOperationalControlEvidence(
            mapper, new org.springframework.core.io.DefaultResourceLoader());
    org.springframework.test.util.ReflectionTestUtils.setField(
        resolver, "operationalEvidence", evidence);

    var result =
        resolver.resolve("experiment:92", experiment, "pde-construction-approval").orElseThrow();
    var proof = result.pdeContext().path("operationalControlEvidence");
    assertThat(proof.path("criteria").size()).isEqualTo(7);
    assertThat(proof.path("prototypeVersion").asText()).isEqualTo(cycle.getProductVersion());
    assertThat(proof.path("origin").asText()).isEqualTo("LOCAL_MYSQL57_WITH_CONTEXT_TEST_DOUBLES");
    assertThat(proof.path("reportSha256").asText()).matches("[a-f0-9]{64}");
    assertThat(proof.path("agentApprovalClaimed").asBoolean()).isFalse();
    assertThat(proof.path("commercialEvidenceClaimed").asBoolean()).isFalse();
    assertThat(result.pdeContext().path("lineage").path("productId").asLong()).isEqualTo(productId);
    verify(cycles, never()).save(any());
  }

  /** Reproduz a perda de tipo da tarefa 595 e exporta duas identidades ao executor local real. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "7, Capella, Agenda Cheia Nail Design, LOW_TICKET_DIGITAL_PRODUCT, Quartzo",
    "97001, Outra identidade QA, Outro resultado QA, PDE, Opala"
  })
  void preservesCatalogIdentityWithoutInventingPrototype(
      long productId, String internalName, String commercialName, String typeCode, String typeName)
      throws Exception {
    product.setId(productId);
    product.setInternalName(internalName);
    product.setName(commercialName);
    product.setProductTypeDefinition(
        com.marketinghub.producttype.ProductTypeDefinition.builder()
            .id(123L)
            .code(typeCode)
            .internalName(typeName)
            .build());
    cycle.setProductId(productId);
    var fixture =
        mapper.readTree(getClass().getResourceAsStream("/learningcycle/successor-planning.json"));
    approved.get(0).setResultJson(fixture.path("architecture").toString());
    approved.get(1).setResultJson(fixture.path("economics").toString());
    approved.get(2).setResultJson(fixture.path("strategy").toString());

    var result =
        resolver.resolve("experiment:92", experiment, "pde-construction-approval").orElseThrow();
    var identity = result.pdeContext().path("product");
    assertThat(identity.path("id").asLong()).isEqualTo(productId);
    assertThat(identity.path("internalName").asText()).isEqualTo(internalName);
    assertThat(identity.path("commercialName").asText()).isEqualTo(commercialName);
    assertThat(identity.path("productTypeId").asLong()).isEqualTo(123L);
    assertThat(identity.path("productTypeCode").asText()).isEqualTo(typeCode);
    assertThat(identity.path("productTypeInternalName").asText()).isEqualTo(typeName);
    assertThat(result.pdeContext().path("status").asText()).isEqualTo("PLANNED");
    assertThat(result.pdeContext().has("privatePrototypeAcceptance")).isFalse();
    assertThat(result.publicUrl()).isNull();
    assertThat(result.experienceVersion()).isEqualTo(cycle.getProductVersion());
    String output = System.getProperty("pde.specification.context.output");
    if (output != null) {
      var directory = java.nio.file.Path.of(output);
      java.nio.file.Files.createDirectories(directory);
      java.nio.file.Files.writeString(
          directory.resolve(productId + ".json"),
          mapper.writeValueAsString(Map.of("taskTarget", result)));
    }
  }

  /** Não inventa tipo a partir de um mineral mencionado no texto livre da arquitetura. */
  @Test
  void keepsMissingCatalogTypeUnknown() {
    var result =
        resolver.resolve("experiment:92", experiment, "pde-construction-approval").orElseThrow();
    assertThat(result.pdeContext().path("product").path("productTypeCode").isNull()).isTrue();
    assertThat(result.pdeContext().path("product").path("productTypeInternalName").isNull())
        .isTrue();
  }

  /**
   * Exporta o contrato produzido pelo backend para a validação real do consumidor na matriz local.
   */
  @Test
  void exportsCompleteContractForWorkerValidation() throws Exception {
    var fixture =
        mapper.readTree(getClass().getResourceAsStream("/learningcycle/successor-planning.json"));
    approved.get(0).setResultJson(fixture.path("architecture").toString());
    approved.get(1).setResultJson(fixture.path("economics").toString());
    approved.get(2).setResultJson(fixture.path("strategy").toString());
    var result =
        resolver.resolve("experiment:92", experiment, "pde-construction-approval").orElseThrow();
    assertThat(result.pdeContext()).isNotNull();
    assertThat(
            result
                .pdeContext()
                .path("harness")
                .path("privatePrototype")
                .path("maxValueTimeMinutes")
                .asInt())
        .isEqualTo(5);
    String output = System.getProperty("vega.context.output");
    if (output != null)
      java.nio.file.Files.writeString(
          java.nio.file.Path.of(output), mapper.writeValueAsString(Map.of("taskTarget", result)));
  }

  /** Valida o encadeamento atual e permite homologar os callbacks reais dos dois executores. */
  @Test
  void exportsAgentPlanningWithoutDowngradingEconomics() throws Exception {
    var fixture =
        mapper.readTree(getClass().getResourceAsStream("/learningcycle/successor-planning.json"));
    var strategy = (com.fasterxml.jackson.databind.node.ObjectNode) fixture.path("strategy");
    var contract = strategy.withObject("/marketStrategicContract");
    contract
        .put("contractVersion", "MARKET_STRATEGY_V4")
        .put("status", "READY_FOR_AGENT_VALIDATION");
    var historicalPlan = contract.path("privateValidationPlan").deepCopy();
    contract.remove("privateValidationPlan");
    contract.set(
        "agentValidationPlan",
        mapper.readTree(
            getClass().getResourceAsStream("/contracts/pde-agent-validation-plan-v1.json")));
    contract
        .withObject("/agentValidationPlan")
        .set("purchaseScene", historicalPlan.path("purchaseScene"));
    contract
        .withObject("/agentValidationPlan")
        .set("customerValueDelivery", historicalPlan.path("humanValueDelivery"));
    var economics = (com.fasterxml.jackson.databind.node.ObjectNode) fixture.path("economics");
    economics.put("contractVersion", "PDE_AGENT_ECONOMICS_V1");
    approved.get(0).setResultJson(fixture.path("architecture").toString());
    approved.get(1).setResultJson(economics.toString());
    approved.get(2).setResultJson(strategy.toString());
    String handoff = System.getProperty("pde.planning.handoff.input");
    if (handoff != null) {
      var actual = mapper.readTree(java.nio.file.Files.readString(java.nio.file.Path.of(handoff)));
      approved.get(0).setResultJson(actual.path("architecture").toString());
      approved.get(1).setResultJson(actual.path("economics").toString());
      approved.get(2).setResultJson(actual.path("strategy").toString());
    }
    var result =
        resolver.resolve("experiment:92", experiment, "pde-construction-approval").orElseThrow();
    assertThat(result.pdeContext()).isNotNull();
    assertThat(result.pdeContext().path("economicsContractVersion").asText())
        .isEqualTo("PDE_AGENT_ECONOMICS_V1");
    assertThat(
            result
                .pdeContext()
                .path("strategyAgentValidationPlan")
                .path("contractVersion")
                .asText())
        .isEqualTo("PDE_AGENT_VALIDATION_V1");
    assertThat(
            result
                .pdeContext()
                .path("agentValidationPlan")
                .path("purchaseScene")
                .path("trigger")
                .asText())
        .isNotBlank();
    assertThat(
            result
                .pdeContext()
                .path("agentValidationPlan")
                .path("customerValueDelivery")
                .path("readyMadeOutcome")
                .asText())
        .isNotBlank();
    assertThat(
            result
                .pdeContext()
                .path("agentValidationPlan")
                .path("humanEvidenceClaimed")
                .asBoolean(true))
        .isFalse();
    String output = System.getProperty("pde.agent.context.output");
    if (output != null)
      java.nio.file.Files.writeString(
          java.nio.file.Path.of(output), mapper.writeValueAsString(Map.of("taskTarget", result)));
    approved
        .get(1)
        .setResultJson(economics.put("contractVersion", "PDE_PRIVATE_ECONOMICS_V1").toString());
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext())
        .isNull();
  }

  /** A ausência de uma aprovação não autoriza usar silenciosamente a versão comercial anterior. */
  @Test
  void missingApprovalKeepsTargetVersionButBlocksContext() {
    approved.remove(0);
    var result =
        resolver.resolve("experiment:92", experiment, "pde-construction-approval").orElseThrow();
    assertThat(result.experienceVersion()).isEqualTo("successor-v8");
    assertThat(result.pdeContext()).isNull();
  }

  /** Não aceita parecer de outro agente nem aprovação substituída por rejeição. */
  @Test
  void rejectsWrongAgentAndLatestRejectedContract() {
    approved.getFirst().getAssignedAgent().setAgentKey("other");
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext())
        .isNull();
    approved.getFirst().getAssignedAgent().setAgentKey("landing-generator");
    approved.getFirst().setResultJson("{\"decision\":\"ADJUST\"}");
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext())
        .isNull();
  }

  /**
   * Mudança posterior na economia exige arquitetura compatível, sem herdar autorização comercial.
   */
  @Test
  void rejectsStaleArchitectureAndCommercialSpend() {
    approved.get(1).setDeliveredAt(approved.getFirst().getDeliveredAt().plusSeconds(1));
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext())
        .isNull();
    setup();
    approved
        .get(1)
        .setResultJson(
            "{\"contractVersion\":\"PDE_PRIVATE_ECONOMICS_V1\",\"decision\":\"APPROVE\",\"economics\":{\"commercialSpendAuthorized\":true}}");
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext())
        .isNull();
  }

  /**
   * Uma nova tentativa bloqueada ou em execução invalida o reaproveitamento da aprovação anterior.
   */
  @Test
  void latestAttemptMustBeCompletedEvenWhenOlderApprovalExists() {
    var previous = approved.getFirst();
    var replacement =
        task(400, "productArchitecture", "landing-generator", previous.getResultJson());
    approved.addFirst(replacement);
    for (var state : List.of("BLOCKED", "IN_PROGRESS", "PENDING", "CANCELLED")) {
      replacement.setStatus(state);
      assertThat(
              resolver
                  .resolve("experiment:92", experiment, "pde-construction-approval")
                  .orElseThrow()
                  .pdeContext())
          .as(state)
          .isNull();
    }
    replacement.setStatus("COMPLETED");
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext()
                .path("lineage")
                .path("architectureTaskId")
                .asLong())
        .isEqualTo(400);
  }

  /** Não expõe dados de outro produto ou da construção em processos comerciais diferentes. */
  @Test
  void isolatesProductProcessAndClosedCycle() {
    assertThat(resolver.resolve("experiment:92", experiment, "landing-page-generation")).isEmpty();
    cycle.setStatus("ADJUSTED");
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext())
        .isNull();
    cycle.setProductId(10L);
    assertThatThrownBy(
            () -> resolver.resolve("experiment:92", experiment, "pde-construction-approval"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("produtos diferentes");
  }

  /** Entrega ao executor a URL somente após aceitação da mesma versão do ciclo. */
  @Test
  void includesAcceptedImplementationWithoutChangingHistoricalProduct() throws Exception {
    var prototype = mock(LearningCyclePrototypeContext.class);
    var acceptance =
        mapper.readTree(
            "{\"status\":\"READY\",\"prototypeVersion\":\"successor-v8\",\"privateAccessUrl\":\"https://private.invalid/vega-private\"}");
    when(prototype.resolve(cycle)).thenReturn(Optional.of(acceptance));
    org.springframework.test.util.ReflectionTestUtils.setField(
        resolver, "prototypeContext", prototype);
    var result =
        resolver.resolve("experiment:92", experiment, "pde-construction-approval").orElseThrow();
    assertThat(result.publicUrl()).isEqualTo("https://private.invalid/vega-private");
    assertThat(result.pdeContext().path("status").asText()).isEqualTo("PRIVATE_PROTOTYPE_READY");
    assertThat(result.pdeContext().path("inheritedLearning").path("experimentId").asLong())
        .isEqualTo(91);
    assertThat(product.getPdeExperienceJson()).contains("historical-v7");
  }

  /**
   * Reproduz a falta do registro em Vega e outra identidade: somente a prova da mesma versão
   * entrega URL e linhagem válidas à homologação, preservando as aprovações históricas.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "4, 10, 103, musa-pde-entry-v13-primeiro-ajuste-aplicavel",
    "97004, 97010, 97103, another-private-v17"
  })
  void registeredProofConnectsConstructionToCurrentHomologation(
      long productId, long cycleId, long experimentId, String version) {
    product.setId(productId);
    cycle.setId(cycleId);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    cycle.setProductVersion(version);
    experiment.setId(experimentId);
    String reference = "experiment:" + experimentId;
    when(cycles.findByExperimentId(experimentId)).thenReturn(Optional.of(cycle));
    when(tasks.findFunctionalSnapshotsByProcessSince(67L, reference, cycle.getCreatedAt()))
        .thenAnswer(
            ignored ->
                tasks.findFunctionalSnapshotsByProcessSince(
                    67L, "experiment:92", cycle.getCreatedAt()));
    var eventRepository =
        mock(com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository.class);
    var history =
        new ArrayList<com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent>();
    when(eventRepository.findByCycleIdOrderByRevisionAsc(cycleId)).thenAnswer(ignored -> history);
    var prototype = new LearningCyclePrototypeContext(eventRepository, mapper);
    org.springframework.test.util.ReflectionTestUtils.setField(
        resolver, "prototypeContext", prototype);
    com.marketinghub.agenttask.AgentTaskTargetContextProvider targets =
        source -> resolver.resolve(source, experiment, "pde-construction-approval");
    var readiness =
        new com.marketinghub.product.service.agentvalidation.PdeTechnicalHomologationReadinessProvider(
            targets);
    var process = new BusinessProcessDefinition();
    process.setProcessCode("pde-construction-approval");
    process.setVersionNumber(8);
    var activity = new com.marketinghub.businessprocess.BusinessProcessActivityDefinition();
    activity.setActivityId("technicalHomologation");
    assertThat(readiness.readiness(process, activity, product, reference).ready()).isFalse();
    var predecessor =
        new com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent();
    predecessor.setAction("REGISTER_PROTOTYPE");
    predecessor.setCreatedAt(Instant.now());
    var historicalEvidence = mapper.createObjectNode().put("productVersion", "historical-v7");
    historicalEvidence
        .putObject("privatePrototype")
        .put("prototypeVersion", "historical-v7")
        .put("privateAccessUrl", "https://historical.invalid/agent-validation");
    predecessor.setEvidenceJson(historicalEvidence.toString());
    history.add(predecessor);
    assertThat(readiness.readiness(process, activity, product, reference).ready()).isFalse();
    var proof = mapper.createObjectNode();
    proof.put("prototypeVersion", version);
    proof.put("privateAccessUrl", "https://private.invalid/agent-validation");
    proof.put("image", "repo/fixture:validated");
    proof.put("evidenceReference", "Relatório local sintético: desktop, celular, recuperação e segurança.");
    proof.put("observedAt", Instant.now().toString());
    for (var check :
        List.of(
            "desktopValidated",
            "mobileValidated",
            "firstResultValidated",
            "resumeValidated",
            "failuresValidated",
            "testDataExcluded",
            "noExternalSideEffects")) proof.put(check, true);
    prototype.validate(proof, version);
    var registration =
        new com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent();
    registration.setCycleId(cycleId);
    registration.setAction("REGISTER_PROTOTYPE");
    registration.setCreatedAt(Instant.now());
    var evidence = mapper.createObjectNode().put("productVersion", version);
    evidence.set("privatePrototype", proof);
    registration.setEvidenceJson(evidence.toString());
    history.add(registration);
    assertThat(readiness.readiness(process, activity, product, reference).ready()).isTrue();
    var target = targets.resolve(reference).orElseThrow();
    assertThat(target.publicUrl()).isEqualTo("https://private.invalid/agent-validation");
    assertThat(target.experienceVersion()).isEqualTo(version);
    assertThat(target.pdeContext().path("lineage").path("learningCycleId").asLong())
        .isEqualTo(cycleId);
    assertThat(target.pdeContext().path("lineage").path("productId").asLong()).isEqualTo(productId);
    assertThat(target.pdeContext().path("lineage").path("experimentId").asLong())
        .isEqualTo(experimentId);
    assertThat(product.getPdeExperienceJson()).contains("historical-v7");
    verify(cycles, never()).save(any());
  }
}
