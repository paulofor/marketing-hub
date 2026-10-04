package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Responsabilidade: homologar plano, relatório e renovação técnica pelo mesmo fluxo BPM local. */
class TechnicalPreflightCurrentEvidenceFlowTest {
  private final ObjectMapper json = new ObjectMapper();
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessActivityDefinitionRepository definitions =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final AgentTaskService paidTasks = mock(AgentTaskService.class);
  private final ExperimentTechnicalPreflightEvidenceService evidence =
      mock(ExperimentTechnicalPreflightEvidenceService.class);
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final Map<String, BusinessProcessActivityDefinition> stages = new LinkedHashMap<>();
  private final List<BusinessProcessActivityInstance> history = new ArrayList<>();
  private final Map<String, ExperimentTechnicalPreflightEvidenceService.Evidence> current =
      new HashMap<>();
  private final Product product =
      Product.builder().id(96001L).internalName("Produto local").build();
  private final BusinessProcessDefinition process = new BusinessProcessDefinition();
  private final ExperimentTechnicalPreflightActivityExecutor executor =
      new ExperimentTechnicalPreflightActivityExecutor(predecessors, evidence, instances, json);
  private final BusinessProcessActivityExecutionService service =
      new BusinessProcessActivityExecutionService(
          processes,
          definitions,
          mock(AgentTaskRepository.class),
          mock(AgentTaskActivityCoverageRepository.class),
          instances,
          plans,
          null,
          products,
          experiments,
          paidTasks,
          json,
          List.of(executor),
          List.of(),
          List.of());
  private String reference = "experiment:96002";

  /** Prepara três provas válidas e uma pendência financeira sem recursos externos. */
  @BeforeEach
  void setup() {
    process.setId(96003L);
    process.setName("Homologação técnica local");
    process.setProcessCode("experiment-homologation-activation");
    process.setVersionNumber(1);
    process.setStatus("PUBLISHED");
    process.setDiagramJson(
        """
        {"nodes":[{"id":"surfaces","type":"TASK","label":"Superfícies"},
        {"id":"transaction","type":"TASK","label":"Compra"},
        {"id":"measurement","type":"TASK","label":"Medição"},
        {"id":"financialGuardrails","type":"TASK","label":"Limites financeiros"}],
        "flows":[{"from":"surfaces","to":"transaction"},
        {"from":"transaction","to":"measurement"},
        {"from":"measurement","to":"financialGuardrails"}]}
        """);
    when(processes.findById(process.getId())).thenReturn(Optional.of(process));
    bindProductAndExperiment(product.getId(), 96002L);
    var governingPlan = plan(96004L, "Plano da referência local");
    when(plans.findByProductId(product.getId()))
        .thenReturn(List.of(plan(96006L, "Outra candidata local"), governingPlan));
    when(plans.findByExperimentReference(96002L)).thenReturn(List.of(governingPlan));
    for (String code : List.of("surfaces", "transaction", "measurement", "financialGuardrails")) {
      var stage = new BusinessProcessActivityDefinition();
      stage.setId(96100L + stages.size());
      stage.setActivityId(code);
      stage.setName(
          switch (code) {
            case "surfaces" -> "Validar superfícies candidatas";
            case "transaction" -> "Testar compra, acesso e entrega";
            case "measurement" -> "Validar eventos e deduplicação";
            default -> "Validar limites financeiros persistidos";
          });
      stage.setProcessDefinition(process);
      stage.setDefinitionJson("{\"responsibleAgentKeys\":[]}");
      stages.put(code, stage);
      when(definitions.findByProcessDefinitionIdAndActivityId(process.getId(), code))
          .thenReturn(Optional.of(stage));
      if (!"financialGuardrails".equals(code)) {
        var proof = proof(code, "original-" + code);
        current.put(code, proof);
        var instance = new BusinessProcessActivityInstance();
        instance.setId(96200L + history.size());
        instance.setActivityDefinition(stage);
        instance.setSourceReference(reference);
        instance.setStatus("COMPLETED");
        instance.setObjectiveAchieved(true);
        instance.setOccurrenceNumber(1);
        instance.setObjectiveEvidenceJson(proof.objectiveEvidence().toString());
        instance.setCreatedAt(Instant.parse("2026-10-02T18:00:00Z"));
        history.add(instance);
      }
    }
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(process.getId()))
        .thenReturn(new ArrayList<>(stages.values()));
    when(predecessors.readiness(eq(process), any(), anyString()))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Ordem local conferida."));
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), anyString()))
        .thenAnswer(
            inv ->
                history.stream()
                    .filter(i -> i.getActivityDefinition().getId().equals(inv.getArgument(0)))
                    .filter(i -> i.getSourceReference().equals(inv.getArgument(1)))
                    .max(
                        Comparator.comparingInt(
                            BusinessProcessActivityInstance::getOccurrenceNumber)));
    when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), anyString()))
        .thenAnswer(
            inv ->
                history.stream()
                    .filter(i -> i.getActivityDefinition().getId().equals(inv.getArgument(0)))
                    .filter(i -> i.getSourceReference().equals(inv.getArgument(1)))
                    .max(
                        Comparator.comparingInt(
                            BusinessProcessActivityInstance::getOccurrenceNumber)));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                eq(process.getProcessCode()), anyString()))
        .thenAnswer(
            inv ->
                history.stream()
                    .filter(i -> i.getSourceReference().equals(inv.getArgument(1)))
                    .toList());
    when(evidence.evaluate(anyString(), eq(product), anyString()))
        .thenAnswer(
            inv -> {
              String stage = inv.getArgument(0);
              var proof = current.get(stage);
              if (proof == null)
                throw new IllegalStateException(
                    "financialGuardrails".equals(stage)
                        ? "Plano financeiro local vencido; nenhuma análise paga autorizada."
                        : "A publicação, o HTML ou o contrato Quartzo mudaram após a homologação.");
              return proof;
            });
    when(instances.saveAndFlush(any()))
        .thenAnswer(
            inv -> {
              var saved = inv.<BusinessProcessActivityInstance>getArgument(0);
              saved.setId(96300L + history.size());
              history.add(saved);
              return saved;
            });
  }

  /**
   * O relatório deixa de indicar andamento quando a referência encerrou e mantém as três provas.
   */
  @Test
  void reportsClosedReferenceWithoutLosingPartialProgress() throws Exception {
    when(evidence.executionBlockReason(product, reference))
        .thenReturn("Experimento encerrado; preserve as provas.");
    var result =
        service.productProcessExecutions(
            process.getId(), product.getId(), null, null, false, reference);
    assertThat(result.operationalState()).isEqualTo("CLOSED");
    assertThat(result.objectiveAchieved()).isFalse();
    assertThat(result.completedActivityCount()).isEqualTo(3);
    assertThat(result.remainingActivityCount()).isEqualTo(1);
    assertThat(result.currentActivityStateReason()).contains("encerrado");
    String output = System.getProperty("closed-history.fixture-output");
    if (output != null) Files.writeString(Path.of(output), json.writeValueAsString(result));
    verifyNoInteractions(paidTasks);
  }

  /** O relatório usa o plano do experimento mesmo com outra candidata mais recente do produto. */
  @ParameterizedTest
  @CsvSource({"7,88,2,35", "97001,97002,97003,97004"})
  void selectsOnlyThePlanOfTheExplicitExperiment(
      long productId, long experimentId, long planId, long otherId) {
    bindProductAndExperiment(productId, experimentId);
    var governing = plan(planId, "Plano da referência");
    var other = plan(otherId, "Outro experimento");
    when(plans.findByProductId(productId)).thenReturn(List.of(other, governing));
    when(plans.findByExperimentReference(experimentId)).thenReturn(List.of(governing));

    var report =
        service.productProcessExecutions(process.getId(), productId, null, null, false, reference);

    assertThat(report.commercialPlanId()).isEqualTo(planId);
    assertThat(report.commercialPlanName()).isEqualTo("Plano da referência");
    assertThat(report.currentExecutionReference()).isEqualTo(reference);
    verifyNoInteractions(paidTasks);
  }

  /** Referência sem plano não herda a economia de outra candidata nem de outro produto. */
  @Test
  void neverFallsBackToUnrelatedProductPlan() {
    when(plans.findByProductId(product.getId()))
        .thenReturn(List.of(plan(97004L, "Outra candidata")));
    when(plans.findByExperimentReference(96002L))
        .thenReturn(List.of(plan(98001L, "Outro produto")));

    assertThat(report().commercialPlanId()).isNull();
    verifyNoInteractions(paidTasks);
  }

  /** Uma prova vigente continua concluída e o bloqueio financeiro não causa nova execução paga. */
  @Test
  void preservesCurrentEvidenceAndFinancialWait() throws Exception {
    var report = report();

    assertThat(report.completedActivityCount()).isEqualTo(3);
    assertThat(report.objectiveAchieved()).isFalse();
    assertThat(report.currentActivityId()).isEqualTo("financialGuardrails");
    assertThat(report.currentActivityStateReason()).contains("financeiro local vencido");
    assertThat(report.activities().getFirst().objectiveAchieved()).isTrue();
    verify(instances, never()).saveAndFlush(any());
    verifyNoInteractions(paidTasks);
    writeReport("current-report.json", report);
  }

  /** Fonte inválida retira somente a conclusão vigente e preserva a tentativa anterior intacta. */
  @Test
  void staleSurfaceDoesNotCountAsCurrentObjective() throws Exception {
    var original = history.getFirst();
    var originalEvidence = original.getObjectiveEvidenceJson();
    current.remove("surfaces");

    var report = report();

    assertThat(report.completedActivityCount()).isEqualTo(2);
    assertThat(report.currentActivityId()).isEqualTo("surfaces");
    assertThat(report.activities().getFirst().objectiveAchieved()).isFalse();
    assertThat(report.activities().getFirst().executionRequestAvailable()).isFalse();
    assertThat(report.currentActivityStateReason()).contains("mudaram após a homologação");
    assertThat(original.getStatus()).isEqualTo("COMPLETED");
    assertThat(original.isObjectiveAchieved()).isTrue();
    assertThat(original.getObjectiveEvidenceJson()).isEqualTo(originalEvidence);
    assertThat(report.activities().get(1).objectiveAchieved()).isTrue();
    assertThat(report.activities().get(2).objectiveAchieved()).isTrue();
    verify(instances, never()).saveAndFlush(any());
    verifyNoInteractions(paidTasks);
    writeReport("stale-report.json", report);
  }

  /** A tentativa pendente preserva o workspace mesmo quando predecessoras impedem o comando. */
  @Test
  void pendingRenewalKeepsWorkspaceAndHistoryWithoutCompletingActivities() throws Exception {
    current.clear();
    for (String code : List.of("transaction", "measurement", "financialGuardrails")) {
      when(predecessors.readiness(process, stages.get(code), reference))
          .thenReturn(
              new ProductProcessActivityPredecessorReadiness(
                  false, "Conclua a atividade anterior."));
    }

    var report = report();

    assertThat(report.completedActivityCount()).isZero();
    assertThat(report.objectiveAchieved()).isFalse();
    assertThat(report.activities())
        .allSatisfy(
            a -> {
              assertThat(a.executionControl().workspaceCode()).isEqualTo("EXPERIMENT_PREFLIGHT");
              assertThat(a.executionControl().workspaceReferenceId()).isEqualTo(96002L);
              assertThat(a.executionRequestAvailable()).isFalse();
              assertThat(a.objectiveAchieved()).isFalse();
            });
    assertThat(history)
        .hasSize(3)
        .allSatisfy(i -> assertThat(i.getStatus()).isEqualTo("COMPLETED"));
    verify(instances, never()).saveAndFlush(any());
    verifyNoInteractions(paidTasks);
    writeReport("pending-report.json", report);
  }

  /** Prova renovada passa pelo comando oficial e repetição conserva uma única nova ocorrência. */
  @Test
  void renewsThroughCanonicalCommandAndRemainsIdempotent() {
    var original = history.getFirst();
    current.put("surfaces", proof("surfaces", "renewed-surfaces"));
    assertThat(report().activities().getFirst().executionRequestAvailable()).isTrue();

    var result =
        service.requestProductActivityExecution(
            process.getId(), product.getId(), "surfaces", null, null, reference);
    executor.execute(process, stages.get("surfaces"), product, reference);

    assertThat(result.objectiveAchieved()).isTrue();
    assertThat(history).hasSize(4);
    assertThat(history.getLast().getOccurrenceNumber()).isEqualTo(2);
    assertThat(history.getLast().getKnownCostUsd()).isZero();
    assertThat(history.getFirst()).isSameAs(original);
    assertThat(report().completedActivityCount()).isEqualTo(3);
    assertThat(report().currentActivityId()).isEqualTo("financialGuardrails");
    verify(instances, times(1)).saveAndFlush(any());
    verifyNoInteractions(paidTasks);
  }

  /** Mantém produto e experimento explicitamente vinculados nos seletores da consulta local. */
  private void bindProductAndExperiment(long productId, long experimentId) {
    product.setId(productId);
    reference = "experiment:" + experimentId;
    var experiment = Experiment.builder().id(experimentId).product(product).build();
    when(products.findById(productId)).thenReturn(Optional.of(product));
    when(experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    when(evidence.referencedExperimentId(product, reference)).thenReturn(experimentId);
    when(experiments.existsByIdAndProductId(experimentId, productId)).thenReturn(true);
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(productId))
        .thenReturn(List.of(experiment));
  }

  /** Consulta o mesmo contrato que alimenta a tela e o conciliador de processos. */
  private com.marketinghub
          .businessprocess
          .execution
          .service
          .productProcessExecutions
          .ProductProcessActivityExecutionHistoryResponse
      report() {
    return service.productProcessExecutions(
        process.getId(), product.getId(), null, null, false, reference);
  }

  /** Cria evidência estruturada com impressão estável e identidade segregada. */
  private ExperimentTechnicalPreflightEvidenceService.Evidence proof(
      String activity, String fingerprint) {
    var data =
        json.createObjectNode()
            .put("runId", 96005L)
            .put("activityId", activity)
            .put("inputFingerprint", fingerprint);
    return new ExperimentTechnicalPreflightEvidenceService.Evidence(
        96002L, 96005L, fingerprint, data);
  }

  /** Cria uma referência comercial local sem autorização nem credencial externa. */
  private CommercialPlan plan(long id, String name) {
    var plan = new CommercialPlan();
    plan.setId(id);
    plan.setName(name);
    return plan;
  }

  /** Exporta a resposta real do serviço local para homologação visual sem reconstruir estados. */
  private void writeReport(String filename, Object report) throws Exception {
    String output = System.getProperty("preflight.output");
    if (output == null) return;
    Files.createDirectories(Path.of(output));
    Files.writeString(Path.of(output, filename), json.writeValueAsString(report));
  }
}
