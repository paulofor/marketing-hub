package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleConstructionContext;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleExecutionContext;
import com.marketinghub.communication.v1.IrisLearningCycleContext;
import com.marketinghub.communication.v1.IrisProductProcessActivityReadinessProvider;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.valuechainposition.ProductProcessPeriodService;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/** Comprova o gate e suas revisões compatíveis sem fabricar venda ou autorização de mídia. */
class PdeAgentValidationGateActivityExecutorTest {
  private static final Instant NOW = Instant.parse("2026-09-06T12:00:00Z");
  private static final String SOURCE = "product:10@agent-validation-v1";
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ProductProcessPeriodService periods = mock(ProductProcessPeriodService.class);
  private final ObjectMapper json = new ObjectMapper();
  private PdeAgentValidationGateActivityExecutor executor;
  private BusinessProcessDefinition process;
  private BusinessProcessActivityDefinition gate;
  private Product product;
  private List<AgentTask> completedTasks;

  /** Monta uma ocorrência completa e cronologicamente válida da mesma versão de Mira. */
  @BeforeEach
  void setUp() {
    executor =
        new PdeAgentValidationGateActivityExecutor(
            tasks, instances, products, periods, json, Clock.fixed(NOW, ZoneOffset.UTC));
    process = new BusinessProcessDefinition();
    process.setId(70L);
    process.setProcessCode("pde-construction-approval");
    process.setVersionNumber(8);
    process.setStatus("PUBLISHED");
    gate = new BusinessProcessActivityDefinition();
    gate.setId(710L);
    gate.setProcessDefinition(process);
    gate.setActivityId("agentValidationGate");
    product =
        Product.builder()
            .id(10L)
            .slug("orientacao-digital-rotina-pele-madura")
            .internalName("Mira")
            .commercialStatus("PLANNED")
            .automaticExecutionEnabled(true)
            .validationDefinitionVersion("PDE_AGENT_VALIDATION_V1")
            .validationDefinitionJson(validationContract())
            .pdeExperienceJson(
                "{\"experienceVersion\":\"private-validation-v1\",\"status\":\"AGENT_VALIDATION_READY\"}")
            .build();
    completedTasks = new ArrayList<>();
    completedTasks.add(
        task(
            100L,
            "technicalHomologation",
            "customer-agent",
            "DETERMINISTIC",
            "pde-agent-validation-harness-v1",
            technicalResult(),
            NOW.minusSeconds(500)));
    completedTasks.add(
        task(
            101L,
            "psiqueAdherent",
            "customer-agent",
            "MODEL",
            "gpt-5.6-sol",
            psiqueResult("ADHERENT"),
            NOW.minusSeconds(400)));
    completedTasks.add(
        task(
            102L,
            "psiqueRecovery",
            "customer-agent",
            "MODEL",
            "gpt-5.6-sol",
            psiqueResult("RECOVERY"),
            NOW.minusSeconds(350)));
    completedTasks.add(
        task(
            103L,
            "psiqueSafety",
            "customer-agent",
            "MODEL",
            "gpt-5.6-sol",
            psiqueResult("SAFETY"),
            NOW.minusSeconds(300)));
    completedTasks.add(
        task(
            104L,
            "commercialIntegrityReview",
            "meta-ad-approver",
            "MODEL",
            "gpt-5.6-sol",
            temisResult(),
            NOW.minusSeconds(100)));
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(70L, SOURCE))
        .thenReturn(completedTasks);
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            710L, SOURCE))
        .thenReturn(Optional.empty());
    when(instances.saveAndFlush(any(BusinessProcessActivityInstance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  /** Libera somente comunicação em revisões compatíveis e preserva mercado, pagamento e mídia. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {8, 9, 42})
  void approvesCompleteAgentValidationWithoutCommercialSideEffects(int version) throws Exception {
    process.setVersionNumber(version);
    for (var source :
        json.readTree(
            java.nio.file.Path.of("../../infra/testing/pde-commercial-principles/sources.json")
                .toFile()))
      if (process.getProcessCode().equals(source.path("processCode").asText()))
        process.setDiagramJson(source.path("diagram").toString());
    var readiness = executor.readiness(process, gate, product, SOURCE);
    var result = executor.execute(process, gate, product, SOURCE);

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.requirements()).allMatch(requirement -> requirement.satisfied());
    assertThat(result.operationalState()).isEqualTo("COMPLETED");
    assertThat(product.getCommercialStatus()).isEqualTo("COMUNICACAO_E_JORNADA");
    assertThat(product.getAutomaticExecutionEnabled()).isFalse();
    assertThat(product.getValidationDefinitionVersion()).isEqualTo("PDE_AGENT_VALIDATED_V1");
    var validation = json.readTree(product.getValidationDefinitionJson());
    assertThat(validation.path("purchaseMomentStatus").asText())
        .isEqualTo("WAITING_MARKET_VALIDATION");
    assertThat(validation.path("finalCommercialPrioritizationEligible").asBoolean()).isFalse();
    assertThat(validation.path("communicationPreparationEligible").asBoolean()).isTrue();
    assertThat(validation.path("agentValidation").path("humanEvidenceClaimed").asBoolean())
        .isFalse();
    verify(products).save(product);
    verify(periods).recordTransition(product, "PLANNED");
    ArgumentCaptor<BusinessProcessActivityInstance> saved =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getObjectiveEvidenceJson())
        .contains("PDE_AGENT_VALIDATION_GATE_V1")
        .contains("\"publicationAuthorized\":false")
        .contains("\"campaignAuthorized\":false")
        .contains("\"mediaSpendAuthorizedBrl\":0")
        .contains("\"humanEvidenceClaimed\":false");
  }

  /** Renova evidências do ciclo sem regredir o produto comercial nem pausar sua operação. */
  @Test
  void revalidatesOpenLearningCycleWithoutResettingCommercialState() throws Exception {
    var cycleContext = mock(LearningCycleExecutionContext.class);
    ReflectionTestUtils.setField(executor, "learningCycleContext", cycleContext);
    product.setCommercialStatus("EXPERIMENTING");
    product.setPdeExperienceJson("{\"status\":\"PUBLIC\"}");
    when(cycleContext.permitsRevalidation(product)).thenReturn(false);
    assertThat(executor.readiness(process, gate, product, SOURCE).ready()).isFalse();
    when(cycleContext.permitsRevalidation(product)).thenReturn(true);
    assertThat(executor.readiness(process, gate, product, SOURCE).ready()).isTrue();
    assertThat(executor.execute(process, gate, product, SOURCE).objectiveAchieved()).isTrue();
    assertThat(product.getCommercialStatus()).isEqualTo("EXPERIMENTING");
    assertThat(product.getAutomaticExecutionEnabled()).isTrue();
    assertThat(json.readTree(product.getPdeExperienceJson()).path("status").asText())
        .isEqualTo("PUBLIC");
    verify(periods, never()).recordTransition(any(), any());
  }

  /** Preserva a prova imutável para processos posteriores sem reabrir operacionalmente o gate. */
  @Test
  void verifiesHistoricalEvidenceAfterProductAdvancesToCommercialValidation() {
    product.setValidationDefinitionVersion("PDE_AGENT_VALIDATED_V1");
    product.setCommercialStatus("VALIDACAO_COMERCIAL");

    assertThat(executor.readiness(process, gate, product, SOURCE).ready()).isFalse();
    assertThat(executor.historicalEvidenceReadiness(process, gate, product, SOURCE).ready())
        .isTrue();

    completedTasks.getLast().setStatus("BLOCKED");
    assertThat(executor.historicalEvidenceReadiness(process, gate, product, SOURCE).ready())
        .isFalse();
  }

  /** Aprova o ciclo sem alterar o produto comercial nem trocar a referência das tarefas. */
  @Test
  void approvesExperimentCycleAndKeepsItsAuditIdempotent() throws Exception {
    var cycles = mock(PdeAgentValidationCycleContract.class);
    ReflectionTestUtils.setField(executor, "cycleContracts", cycles);
    String reference = "experiment:92";
    var contract = json.readTree(validationContract().replace(SOURCE, reference));
    when(cycles.resolve(product, process, reference)).thenReturn(contract);
    product.setValidationDefinitionVersion("v1");
    product.setCommercialStatus("EXPERIMENTING");
    String originalDefinition = product.getValidationDefinitionJson();
    String originalExperience = product.getPdeExperienceJson();
    for (var task : completedTasks) {
      task.setSourceReference(reference);
      task.setResultJson(task.getResultJson().replace(SOURCE, reference));
    }
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(70L, reference))
        .thenReturn(completedTasks);
    assertThat(executor.readiness(process, gate, product, reference).ready()).isTrue();
    assertThat(executor.execute(process, gate, product, reference).objectiveAchieved()).isTrue();
    ArgumentCaptor<BusinessProcessActivityInstance> saved =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getSourceReference()).isEqualTo(reference);
    assertThat(saved.getValue().getObjectiveEvidenceJson())
        .contains(reference)
        .contains("\"productExecutionState\":\"PLAY\"");
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            710L, reference))
        .thenReturn(Optional.of(saved.getValue()));
    assertThat(executor.execute(process, gate, product, reference).objectiveAchieved()).isTrue();
    verify(instances).saveAndFlush(any());
    verify(products, never()).save(any());
    assertThat(product.getCommercialStatus()).isEqualTo("EXPERIMENTING");
    assertThat(product.getAutomaticExecutionEnabled()).isTrue();
    assertThat(product.getValidationDefinitionVersion()).isEqualTo("v1");
    assertThat(product.getValidationDefinitionJson()).isEqualTo(originalDefinition);
    assertThat(product.getPdeExperienceJson()).isEqualTo(originalExperience);
    // Uma nova prova exige outra ocorrência mesmo quando o protótipo mantém a versão.
    completedTasks.getLast().setId(105L);
    executor.execute(process, gate, product, reference);
    verify(instances, org.mockito.Mockito.times(2)).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getOccurrenceNumber()).isEqualTo(2);
  }

  /** Não usa aprovação antiga quando existe tentativa posterior bloqueada ou ainda em execução. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"BLOCKED", "IN_PROGRESS", "PENDING"})
  void latestAttemptMustBeApproved(String status) {
    var pending =
        task(
            105L,
            "commercialIntegrityReview",
            "meta-ad-approver",
            "MODEL",
            "gpt-5.6-sol",
            temisResult(),
            NOW.minusSeconds(10));
    pending.setStatus(status);
    completedTasks.add(pending);
    assertThat(executor.readiness(process, gate, product, SOURCE).ready()).isFalse();
  }

  /** Exige técnica posterior à última correção, mesmo se houver aprovações antigas completas. */
  @Test
  void correctionRequiresFreshTechnicalEvidence() {
    var correction =
        task(
            106L,
            "prototypeCorrection",
            "landing-generator",
            "MODEL",
            "gpt-5.6-sol",
            correctionResult(),
            NOW.minusSeconds(10));
    completedTasks.add(correction);
    assertThat(executor.readiness(process, gate, product, SOURCE).ready()).isFalse();
    correction.setDeliveredAt(NOW.minusSeconds(600));
    assertThat(executor.readiness(process, gate, product, SOURCE).ready()).isTrue();
    correction.setStatus("BLOCKED");
    assertThat(executor.readiness(process, gate, product, SOURCE).ready()).isFalse();
  }

  /**
   * Liga prova adicional, gate real e prontidão de Íris sem refazer revisões ou declarar entrega.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {10L, 110L})
  void acceptsSupplementOnlyAfterIndependentIntegrityApproval(long productId) throws Exception {
    String reference = "experiment:" + (productId + 92);
    String version = "mira-private-candidate-v3";
    product.setId(productId);
    product.setSlug("synthetic-" + productId);
    product.setValidationDefinitionJson(
        validationContract().replace(SOURCE, reference).replace("mira-private-v2", version));
    var cycleContracts = mock(PdeAgentValidationCycleContract.class);
    when(cycleContracts.resolve(product, process, reference))
        .thenReturn(json.readTree(product.getValidationDefinitionJson()));
    ReflectionTestUtils.setField(executor, "cycleContracts", cycleContracts);
    completedTasks.get(4).setCreatedAt(NOW.minusSeconds(270));
    completedTasks.get(4).setDeliveredAt(NOW.minusSeconds(250));
    var rejected =
        task(
            631,
            "commercialIntegrityReview",
            "meta-ad-approver",
            "MODEL",
            "gpt-5.6-sol",
            "{\"decision\":\"BLOCKED\",\"prototypeVersion\":\"mira-private-v2\"}",
            NOW.minusSeconds(200));
    rejected.setStatus("BLOCKED");
    rejected.setDeliveredAt(null);
    rejected.setUpdatedAt(NOW.minusSeconds(200));
    rejected.setBlockerCategory("FUNCTIONAL_ADJUSTMENT");
    var correction =
        task(
            632,
            "prototypeCorrection",
            "landing-generator",
            "MODEL",
            "gpt-5.6-sol",
            """
        {"decision":"BLOCKED","correctionPlan":{"sourceTaskId":631,
         "rejectedActivityId":"commercialIntegrityReview","previousPrototypeVersion":"mira-private-v2",
         "verification":{"noExternalSideEffects":true}}}
        """,
            NOW.minusSeconds(180));
    correction.setStatus("BLOCKED");
    correction.setDeliveredAt(null);
    correction.setUpdatedAt(NOW.minusSeconds(180));
    completedTasks.add(rejected);
    completedTasks.add(correction);
    var evidence = mock(PdeOperationalControlEvidence.class);
    var report =
        json.createObjectNode()
            .put("generatedAt", NOW.minusSeconds(160).toString())
            .put("reportSha256", "a".repeat(64))
            .put("prototypeVersion", version);
    when(evidence.resolve(product.getSlug(), version)).thenReturn(Optional.of(report));
    ReflectionTestUtils.setField(executor, "operationalEvidence", evidence);
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(70L, reference))
        .thenReturn(completedTasks);
    for (var task : completedTasks) {
      task.setSourceReference(reference);
      var payload =
          (com.fasterxml.jackson.databind.node.ObjectNode)
              json.readTree(
                  task.getResultJson()
                      .replace(SOURCE, reference)
                      .replace("mira-private-v2", version));
      if (payload.has("productId")) payload.put("productId", productId);
      if (payload.has("productSlug")) payload.put("productSlug", product.getSlug());
      task.setResultJson(payload.toString());
    }
    assertThat(executor.readiness(process, gate, product, reference).ready()).isFalse();

    var reviewed =
        task(
            633,
            "commercialIntegrityReview",
            "meta-ad-approver",
            "MODEL",
            "gpt-5.6-sol",
            temisResult().replace(SOURCE, reference).replace("mira-private-v2", version),
            NOW.minusSeconds(100));
    var reviewPayload =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(reviewed.getResultJson());
    reviewPayload.put("productId", productId).put("productSlug", product.getSlug());
    reviewed.setResultJson(reviewPayload.toString());
    reviewed.setSourceReference(reference);
    completedTasks.add(reviewed);
    var accepted = executor.readiness(process, gate, product, reference);
    assertThat(accepted.ready()).withFailMessage("%s", accepted).isTrue();
    assertThat(executor.execute(process, gate, product, reference).objectiveAchieved()).isTrue();
    var saved = ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(saved.capture());
    var resolution =
        json.readTree(saved.getValue().getObjectiveEvidenceJson())
            .path("evidenceCorrectionResolution");
    assertThat(resolution.path("contractVersion").asText())
        .isEqualTo("PDE_EVIDENCE_CORRECTION_RESOLUTION_V1");
    assertThat(resolution.path("correctionTaskId").asLong()).isEqualTo(632);
    assertThat(resolution.path("rejectedTaskId").asLong()).isEqualTo(631);
    assertThat(resolution.path("independentReviewTaskId").asLong()).isEqualTo(633);
    assertThat(resolution.path("reportSha256").asText()).isEqualTo("a".repeat(64));
    assertCommunicationReadyAfterRealGate(productId, reference, report, saved.getValue());
    assertThat(rejected.getStatus()).isEqualTo("BLOCKED");
    assertThat(correction.getStatus()).isEqualTo("BLOCKED");

    report.put("generatedAt", NOW.minusSeconds(300).toString());
    assertThat(executor.readiness(process, gate, product, reference).ready()).isFalse();
    report.put("generatedAt", NOW.minusSeconds(160).toString());
    reviewed.setDeliveredAt(NOW.minusSeconds(190));
    assertThat(executor.readiness(process, gate, product, reference).ready()).isFalse();
    reviewed.setDeliveredAt(NOW.minusSeconds(100));
    correction.setStatus("IN_PROGRESS");
    assertThat(executor.readiness(process, gate, product, reference).ready()).isFalse();
    correction.setStatus("BLOCKED");
    reviewed.setAssignedAgent(Agent.builder().agentKey("landing-generator").build());
    assertThat(executor.readiness(process, gate, product, reference).ready()).isFalse();
  }

  /** Consome o recibo real no contexto e na prontidão de Íris; somente persistência usa doubles. */
  private void assertCommunicationReadyAfterRealGate(
      long productId, String reference, JsonNode report, BusinessProcessActivityInstance savedGate)
      throws Exception {
    long experimentId = productId + 92;
    var cycle = new LearningSalesCycle();
    cycle.setId(productId + 1000);
    cycle.setProductId(productId);
    cycle.setExperimentId(experimentId);
    cycle.setChainDefinitionId(26L);
    cycle.setProductVersion(report.path("prototypeVersion").asText());
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    var experiment = new Experiment();
    experiment.setId(experimentId);
    experiment.setProduct(product);
    experiment.setStatus(ExperimentStatus.PLANNED);
    var chain = new BusinessProcessChainDefinition();
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(process);
    chain.setItems(List.of(item));
    var context =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(product.getValidationDefinitionJson());
    context
        .putObject("lineage")
        .put("learningCycleId", cycle.getId())
        .put("productId", productId)
        .put("experimentId", experimentId)
        .put("strategyTaskId", 90)
        .put("economicsTaskId", 91)
        .put("architectureTaskId", 92);
    context.putObject("marketStrategy").put("contractVersion", "MARKET_STRATEGY_V4");
    context.putObject("economics").put("commercialSpendAuthorized", false);
    context.put("economicsContractVersion", "PDE_AGENT_ECONOMICS_V1");
    context.putObject("harness").put("prototypeObjective", "Organização documental consultável.");
    context.set("operationalControlEvidence", report);
    var target =
        new AgentTaskTargetResponse(
            reference,
            experimentId,
            productId,
            product.getSlug(),
            "Produto sintético",
            "Produto sintético",
            cycle.getProductVersion(),
            context.path("privatePrototypeAcceptance").path("privateAccessUrl").asText(),
            null,
            null,
            null,
            null,
            context);
    var cycles = mock(LearningSalesCycleRepository.class);
    var experiments = mock(ExperimentRepository.class);
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var construction = mock(LearningCycleConstructionContext.class);
    when(cycles.findByExperimentId(experimentId)).thenReturn(Optional.of(cycle));
    when(experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    when(chains.findById(26L)).thenReturn(Optional.of(chain));
    when(construction.resolve(reference, experiment, process.getProcessCode()))
        .thenReturn(Optional.of(target));
    savedGate.setId(productId + 2000);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                process.getId(), reference))
        .thenReturn(List.of(savedGate));
    when(tasks.findPdeValidationTaskSnapshots(reference, process.getProcessCode()))
        .thenReturn(
            completedTasks.stream()
                .map(
                    task ->
                        new PdeValidationTaskSnapshot(
                            task.getId(),
                            process.getId(),
                            task.getProcessActivityId(),
                            task.getStatus(),
                            task.getBlockerCategory(),
                            task.getBlockerAction(),
                            task.getResultJson(),
                            task.getExecutionError()))
                .toList());
    var input =
        new IrisLearningCycleContext(
                cycles, experiments, chains, construction, instances, tasks, json)
            .resolve(reference)
            .orElseThrow();
    assertThat(input.get("inputReadiness")).isEqualTo("READY");
    assertThat(input.get("publicationAuthorized")).isEqualTo(false);
    var strategy = mock(com.marketinghub.agenttask.MarketStrategicContextProvider.class);
    var communication =
        mock(com.marketinghub.agenttask.CommunicationMaterializationContextProvider.class);
    when(communication.resolve(reference)).thenReturn(Optional.of(input));
    assertThat(
            new IrisProductProcessActivityReadinessProvider(strategy, communication)
                .readiness(null, null, product, reference)
                .ready())
        .isTrue();
    var output = System.getenv("MIRA_EVIDENCE_IRIS_INPUT_FILE");
    if (productId == 10 && output != null) {
      java.nio.file.Files.writeString(
          java.nio.file.Path.of(output),
          json.writeValueAsString(
              java.util.Map.of(
                  "taskId",
                  990633L,
                  "sourceReference",
                  reference,
                  "processCode",
                  "pde-communication-sales-journey",
                  "activityId",
                  "communicationContract",
                  "processContextJson",
                  json.writeValueAsString(
                      java.util.Map.of(
                          "marketStrategicContract",
                          input.get("marketStrategicContract"),
                          "communicationMaterializationContext",
                          input)))));
    }
  }

  /** Mantém a correção válida quando uma tentativa condicional posterior foi cancelada. */
  @Test
  void cancelledCorrectionDoesNotSupersedeValidCheckpoint() {
    completedTasks.add(
        task(
            106L,
            "prototypeCorrection",
            "landing-generator",
            "MODEL",
            "gpt-5.6-sol",
            correctionResult(),
            NOW.minusSeconds(600)));
    var cancelled =
        task(
            107L,
            "prototypeCorrection",
            "landing-generator",
            "MODEL",
            "gpt-5.6-sol",
            "{}",
            NOW.minusSeconds(50));
    cancelled.setStatus("CANCELLED");
    completedTasks.add(cancelled);

    var readiness = executor.readiness(process, gate, product, SOURCE);

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.requirements()).allMatch(requirement -> requirement.satisfied());
  }

  /** Bloqueia uma conclusão sem versão sucessora mesmo quando sua data precede a homologação. */
  @Test
  void invalidCompletedCorrectionCannotReleaseGate() {
    completedTasks.add(
        task(
            106L,
            "prototypeCorrection",
            "landing-generator",
            "MODEL",
            "gpt-5.6-sol",
            "{}",
            NOW.minusSeconds(600)));

    var readiness = executor.readiness(process, gate, product, SOURCE);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("última correção");
  }

  /** Mantém o gate fechado quando falta um dos três cenários independentes. */
  @Test
  void blocksWhenOnePsiqueScenarioIsMissing() {
    completedTasks.removeIf(task -> "psiqueRecovery".equals(task.getProcessActivityId()));

    var readiness = executor.readiness(process, gate, product, SOURCE);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("RECOVERY");
    assertThat(readiness.requirements())
        .anySatisfy(
            requirement -> {
              assertThat(requirement.code()).isEqualTo("PSIQUE_SCENARIOS");
              assertThat(requirement.satisfied()).isFalse();
            });
    verify(products, never()).save(any());
  }

  /** Rejeita parecer que tenta converter uma simulação em evidência humana. */
  @Test
  void blocksForgedHumanEvidenceClaim() throws Exception {
    AgentTask psique =
        completedTasks.stream()
            .filter(task -> "psiqueAdherent".equals(task.getProcessActivityId()))
            .findFirst()
            .orElseThrow();
    var forged =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(psique.getResultJson());
    forged.put("humanEvidenceClaimed", true);
    psique.setResultJson(forged.toString());

    var readiness = executor.readiness(process, gate, product, SOURCE);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("ADHERENT");
    verify(products, never()).save(any());
  }

  /** Aprova a matriz nova somente quando os três cenários cobrem os três dispositivos. */
  @Test
  void approvesExtendedNineScenarioDeviceMatrix() throws Exception {
    AgentTask technical =
        completedTasks.stream()
            .filter(task -> "technicalHomologation".equals(task.getProcessActivityId()))
            .findFirst()
            .orElseThrow();
    technical.setResultJson(extendedTechnicalResult(false));

    assertThat(executor.readiness(process, gate, product, SOURCE).ready()).isTrue();

    technical.setResultJson(extendedTechnicalResult(true));
    assertThat(executor.readiness(process, gate, product, SOURCE).ready()).isFalse();
  }

  /** Aceita a comparação explícita de dezoito provas e bloqueia inventários não equivalentes. */
  @Test
  void approvesDocumentedInputComparisonWithoutChangingLegacyContracts() throws Exception {
    AgentTask technical = completedTasks.get(0);
    var result = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(technicalResult());
    var comparison = PdeInputComparisonScenarioMatrixV1Test.matrix("mira-private-v2");
    result.set("scenarios", comparison.path("scenarios"));
    result.set("fixtureContract", comparison.path("fixtureContract"));
    result.set("generationMode", comparison.path("generationMode"));
    result.set("providerCalls", comparison.path("providerCalls"));
    technical.setResultJson(result.toString());

    assertThat(executor.readiness(process, gate, product, SOURCE).ready()).isTrue();

    ((com.fasterxml.jackson.databind.node.ObjectNode)
            result.path("scenarios").get(1).path("products").get(0))
        .put("name", "Inventário divergente");
    technical.setResultJson(result.toString());
    assertThat(executor.readiness(process, gate, product, SOURCE).ready()).isFalse();
  }

  /** Impede que uma versão anterior reutilize silenciosamente o executor e o contrato do v8. */
  @Test
  void preservesVersionEightAndRejectsPreviousContracts() {
    assertThat(executor.supports(process, gate)).isTrue();

    process.setVersionNumber(7);

    assertThat(executor.supports(process, gate)).isFalse();
  }

  /** Monta a predeclaração completa e a aceitação histórica imutável do protótipo. */
  private String validationContract() {
    return """
        {
          "purchaseMomentStatus":"WAITING_MARKET_VALIDATION",
          "privatePrototypeAcceptance":{
            "status":"READY",
            "privateAccessUrl":"https://v7.clubemusa.com.br/mira-private",
            "prototypeVersion":"mira-private-v2"
          },
          "agentValidationPlan":{
            "contractVersion":"PDE_AGENT_VALIDATION_V1",
            "sourceReference":"product:10@agent-validation-v1",
            "trafficClass":"AGENT_VALIDATION",
            "internalMarker":"mh_internal_test",
            "requiredScenarios":["ADHERENT","RECOVERY","SAFETY"],
            "requiredDevices":["DESKTOP_1440","IPHONE_15_PRO","PIXEL_7"],
            "maxReadyResultSeconds":600,
            "humanEvidenceClaimed":false,
            "commercialEvidenceClaimed":false,
            "paymentEnabled":false,
            "publicationAuthorized":false,
            "campaignAuthorized":false,
            "mediaSpendAuthorizedBrl":0,
            "status":"READY"
          }
        }
        """;
  }

  /** Produz o resultado integral do harness nos três dispositivos e cenários. */
  private String technicalResult() {
    return """
        {
          "contractVersion":"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1",
          "mode":"TECHNICAL",
          "decision":"APPROVED",
          "sourceReference":"product:10@agent-validation-v1",
          "productId":10,
          "productSlug":"orientacao-digital-rotina-pele-madura",
          "publicUrl":"https://v7.clubemusa.com.br/mira-private",
          "prototypeVersion":"mira-private-v2",
          "trafficClass":"AGENT_VALIDATION",
          "internalMarker":"mh_internal_test",
          "humanEvidenceClaimed":false,
          "commercialEvidenceClaimed":false,
          "checks":{
            "sameVersion":true,"desktopAndMobile":true,"happyResultWithinTenMinutes":true,
            "recoveryPreserved":true,"safetyBlocked":true,"accessibilityBasic":true,
            "responsiveLayout":true,"privacyPreserved":true,"internalTrafficSegregated":true,
            "paymentDisabled":true,"publicationDisabled":true,"campaignDisabled":true,
            "zeroMediaSpend":true
          },
          "devices":[
            {"deviceProfile":"DESKTOP_1440","status":"PASS"},
            {"deviceProfile":"IPHONE_15_PRO","status":"PASS"},
            {"deviceProfile":"PIXEL_7","status":"PASS"}
          ],
          "scenarios":[
            {"scenarioCode":"ADHERENT","status":"PASS","resultReadySeconds":35},
            {"scenarioCode":"ADHERENT","status":"PASS","resultReadySeconds":40},
            {"scenarioCode":"ADHERENT","status":"PASS","resultReadySeconds":45},
            {"scenarioCode":"RECOVERY","status":"PASS","resultReadySeconds":70},
            {"scenarioCode":"SAFETY","status":"PASS","resultReadySeconds":0}
          ],
          "sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0}
        }
        """;
  }

  /** Produz a matriz estendida e, quando solicitado, duplica um par para provar o bloqueio. */
  private String extendedTechnicalResult(boolean duplicatePair) throws Exception {
    var result = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(technicalResult());
    var checks = (com.fasterxml.jackson.databind.node.ObjectNode) result.path("checks");
    checks.put("staticFixturesValid", true);
    checks.put("providerCallsZero", true);
    checks.put("nineScenarioDeviceGates", true);
    result.put("fixtureContract", "PDE_STATIC_RESULT_FIXTURES_V1");
    var scenarios = result.putArray("scenarios");
    for (String scenario : List.of("ADHERENT", "RECOVERY", "SAFETY")) {
      for (String device : List.of("DESKTOP_1440", "IPHONE_15_PRO", "PIXEL_7")) {
        String effectiveDevice =
            duplicatePair && "SAFETY".equals(scenario) && "PIXEL_7".equals(device)
                ? "IPHONE_15_PRO"
                : device;
        var value = scenarios.addObject();
        value.put("scenarioCode", scenario);
        value.put("deviceProfile", effectiveDevice);
        value.put("status", "PASS");
        value.put("resultReadySeconds", "SAFETY".equals(scenario) ? 0 : 1);
        value.put("trafficClass", "AGENT_VALIDATION");
        value.put("mhInternalTest", true);
        value.put("providerCalls", 0);
        value.put("humanEvidenceClaimed", false);
        value.put("commercialEvidenceClaimed", false);
        var effects = value.putObject("sideEffects");
        effects.put("paymentEnabled", false);
        effects.put("published", false);
        effects.put("campaignCreated", false);
        effects.put("mediaSpendBrl", 0);
      }
    }
    return result.toString();
  }

  /** Produz um parecer sintético completo para o cenário informado. */
  private String psiqueResult(String scenario) {
    return """
        {
          "contractVersion":"PDE_PSIQUE_AGENT_SCENARIO_V1",
          "decision":"APPROVED",
          "scenarioCode":"%s",
          "sourceReference":"product:10@agent-validation-v1",
          "productId":10,
          "productSlug":"orientacao-digital-rotina-pele-madura",
          "prototypeVersion":"mira-private-v2",
          "trafficClass":"AGENT_VALIDATION",
          "internalMarker":"mh_internal_test",
          "syntheticEvaluation":true,
          "humanEvidenceClaimed":false,
          "commercialEvidenceClaimed":false,
          "sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0},
          "experienceAssessment":{"evidenceBoundary":"Simulação explícita limitada ao harness."},
          "checks":{
            "sameProductAndVersion":true,"isolatedFreshSession":true,
            "functionalOutcomeMatchesScenario":true,"lowEffortNoPrompting":true,
            "accessibilityAndResponsive":true,"privacyPreserved":true,
            "internalTrafficSegregated":true,"safeLimits":true,"noExternalSideEffects":true
          },
          "visualAudit":{"evidenceIds":[901]},
          "evidence":["Evidência sintética persistida"],
          "requiredChanges":[],
          "rootCause":"O mecanismo observado sustenta o resultado do cenário."
        }
        """
        .formatted(scenario);
  }

  /** Produz a auditoria independente final com todos os gates verdadeiros. */
  private String temisResult() {
    return """
        {
          "contractVersion":"PDE_TEMIS_AGENT_VALIDATION_V1",
          "decision":"APPROVED",
          "commercialRationale":"A validação está segregada e não afirma resposta humana.",
          "rootCause":"Os contratos preservam verdade, privacidade e efeitos externos nulos.",
          "sourceReference":"product:10@agent-validation-v1",
          "productId":10,
          "productSlug":"orientacao-digital-rotina-pele-madura",
          "prototypeVersion":"mira-private-v2",
          "trafficClass":"AGENT_VALIDATION",
          "internalMarker":"mh_internal_test",
          "humanEvidenceClaimed":false,
          "commercialEvidenceClaimed":false,
          "sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0},
          "agentValidationChecks":{
            "sameProductAndVersion":true,"criteriaPredeclared":true,"technicalHarnessPassed":true,
            "threeScenarioReviewsApproved":true,"syntheticEvidenceLabeled":true,
            "internalTrafficSegregated":true,"privacyPreserved":true,"paymentDisabled":true,
            "publicationDisabled":true,"campaignDisabled":true,"zeroMediaSpend":true,
            "noHumanOrCommercialClaim":true,"strategyFidelity":true
          },
          "evidence":["harness","aderente","recuperação","segurança"],
          "requiredChanges":[]
        }
        """;
  }

  /** Produz o checkpoint válido que liga a versão corrigida à nova homologação técnica. */
  private String correctionResult() {
    return """
        {
          "decision":"READY",
          "correctionPlan":{
            "previousPrototypeVersion":"mira-private-v1",
            "correctedPrototypeVersion":"mira-private-v2",
            "nextActivityId":"technicalHomologation",
            "verification":{
              "technicalRevalidationRequired":true,
              "noExternalSideEffects":true
            }
          }
        }
        """;
  }

  /** Monta uma tarefa concluída com auditoria e custo persistidos. */
  private AgentTask task(
      long id,
      String activityId,
      String agentKey,
      String executionMode,
      String model,
      String result,
      Instant deliveredAt) {
    Agent agent = new Agent();
    agent.setAgentKey(agentKey);
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setAssignedAgent(agent);
    task.setProcessDefinition(process);
    task.setProcessActivityId(activityId);
    task.setSourceReference(SOURCE);
    task.setStatus("COMPLETED");
    task.setExecutionMode(executionMode);
    task.setExecutionModelCode(model);
    task.setResultJson(result);
    task.setEstimatedCostUsd(BigDecimal.ZERO.setScale(8));
    task.setCreatedAt(deliveredAt.minusSeconds(20));
    task.setDeliveredAt(deliveredAt);
    return task;
  }
}
