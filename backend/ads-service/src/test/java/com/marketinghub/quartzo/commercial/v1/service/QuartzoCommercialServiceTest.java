package com.marketinghub.quartzo.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.execution.service.predecessor.*;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.dto.*;
import com.marketinghub.experiment.service.*;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: homologar preparação, callbacks, bloqueios e repetição sem efeitos comerciais
 * externos.
 */
class QuartzoCommercialServiceTest {
  final ObjectMapper json = new ObjectMapper();
  final QuartzoCommercialContext context = mock(QuartzoCommercialContext.class);
  final ExperimentCampaignDestinationPolicy destinations =
      mock(ExperimentCampaignDestinationPolicy.class);
  final ExperimentReadinessService readiness = mock(ExperimentReadinessService.class);
  final QuartzoCommercialChecks checks = new QuartzoCommercialChecks(destinations, readiness);
  final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  final BusinessProcessActivityDefinitionRepository definitions =
      mock(BusinessProcessActivityDefinitionRepository.class);
  final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  final ExperimentRepository experiments = mock(ExperimentRepository.class);
  final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  final QuartzoCommercialService service =
      new QuartzoCommercialService(
          context, checks, instances, definitions, processes, tasks, experiments, predecessors);
  final Map<Long, BusinessProcessActivityInstance> saved = new HashMap<>();
  final Map<String, BusinessProcessActivityDefinition> activities = new LinkedHashMap<>();
  final List<AgentTask> reviews = new ArrayList<>();
  final Product product = Product.builder().id(7L).currentPriceBrl(new BigDecimal("67")).build();
  final Experiment experiment = new Experiment();
  final BusinessProcessDefinition process = new BusinessProcessDefinition();
  ObjectNode snapshot;

  /** Configura fontes sintéticas com repositórios isolados, mantendo checks e serviço reais. */
  @BeforeEach
  void setup() throws Exception {
    experiment.setId(88L);
    experiment.setProduct(product);
    experiment.setStatus(ExperimentStatus.USER_STOPPED);
    experiment.setUnitPrice(new BigDecimal("67"));
    process.setId(80L);
    process.setProcessCode(QuartzoCommercialContext.CODE);
    process.setVersionNumber(1);
    process.setStatus("PUBLISHED");
    var publication =
        GeraSalesPagePublicationAudit.builder()
            .id(27L)
            .experimentId(88L)
            .salesPageUrl("https://example.test/kit")
            .checkoutUrl("https://example.test/pay")
            .build();
    var scope =
        new QuartzoCommercialContext.Scope(experiment, product, "v1", null, null, publication);
    when(context.scope(anyString(), any(), anyBoolean())).thenReturn(scope);
    when(context.read(any())).thenAnswer(i -> json.readTree(i.getArgument(0, String.class)));
    snapshot =
        (ObjectNode)
            json.readTree(
                """
        {"productId":7,"experimentId":88,"productVersion":"v1","fingerprint":"frozen-1",
        "destinationUrl":"https://example.test/kit","checkoutUrl":"https://example.test/pay",
        "deliverable":"Kit utilizável","deliveryMode":"PERSONALIZED","checkoutMonetization":"Compra única",
        "riskReversal":"Reembolso no prazo contratado","validationContract":{"delivery":{"personalization":true}},
        "productProof":[{"assetId":1}],"productProofInPage":true,"creatives":[{"id":2}],"salesProven":false,"mediaSpendAuthorized":false,
        "financialPlan":{"stale":false,"assumptions":{"priceBrl":67},"evaluation":{"status":"PROJECTED_VIABLE"},
        "analysis":{"status":"COMPLETED","result":{"scenarios":[{"name":"CONSERVATIVE"},{"name":"BASE","profitBrl":20,"averagePriceBrl":67},{"name":"OPTIMISTIC"}]}}}}
        """);
    when(context.snapshot("experiment:88")).thenAnswer(i -> snapshot.deepCopy());
    when(destinations.hasCompleteCommercialContract(experiment)).thenReturn(true);
    when(destinations.hasCompletedGeraSalesPagePipeline(88L)).thenReturn(true);
    when(destinations.hasRequiredSalesPageAnalyticsCollectors(publication)).thenReturn(true);
    when(destinations.hasAdDestinationPointingToSalesPage(experiment, publication))
        .thenReturn(true);
    when(readiness.summarize(88L))
        .thenReturn(
            new ExperimentReadinessSummaryDto(
                true,
                1,
                true,
                1,
                true,
                true,
                1,
                1,
                List.of(),
                List.of(),
                false,
                List.of(
                    new ExperimentRunningGateRequirementDto(
                        "CREATIVE_APPROVED", "Criativo", true, "ok", "ok"),
                    new ExperimentRunningGateRequirementDto(
                        "TARGETING_READY", "Público", true, "ok", "ok"))));
    when(predecessors.readiness(any(), any(), anyString()))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "ok"));
    var steps = new ArrayList<>(QuartzoCommercialChecks.PREPARATION);
    steps.addAll(QuartzoCommercialService.REVIEWS);
    steps.add("ready");
    for (String step : steps) {
      var definition = new BusinessProcessActivityDefinition();
      definition.setId((long) activities.size() + 1);
      definition.setActivityId(step);
      definition.setProcessDefinition(process);
      activities.put(step, definition);
      when(definitions.findByProcessDefinitionIdAndActivityId(80L, step))
          .thenReturn(Optional.of(definition));
    }
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), eq("experiment:88")))
        .thenAnswer(i -> Optional.ofNullable(saved.get(i.getArgument(0, Long.class))));
    when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), eq("experiment:88")))
        .thenAnswer(i -> Optional.ofNullable(saved.get(i.getArgument(0, Long.class))));
    when(instances.saveAndFlush(any()))
        .thenAnswer(
            i -> {
              BusinessProcessActivityInstance instance = i.getArgument(0);
              saved.put(instance.getActivityDefinition().getId(), instance);
              return instance;
            });
    when(tasks.findLatestReviewSnapshots(eq(80L), eq("experiment:88"), anyString(), any()))
        .thenAnswer(
            i ->
                reviews.stream()
                    .filter(t -> t.getProcessActivityId().equals(i.getArgument(2)))
                    .reduce((a, b) -> b)
                    .map(
                        t ->
                            List.of(
                                new AgentTaskReviewSnapshot(
                                    t.getId(),
                                    t.getStatus(),
                                    t.getEvidenceJson(),
                                    t.getResultJson())))
                    .orElse(List.of()));

    when(processes.findByProcessCodeAndVersionNumber(QuartzoCommercialContext.CODE, 1))
        .thenReturn(Optional.of(process));
  }

  /** Executa preparação e dois callbacks, consolidando uma única vez sem reativar ou gastar. */
  @Test
  void completesPreparationThroughCallbacksAndPreservesFinancialAndCampaignState()
      throws Exception {
    prepare();
    for (String review : QuartzoCommercialService.REVIEWS) completeReview(review);
    assertThat(
            service
                .execute(process, activities.get("ready"), product, "experiment:88")
                .objectiveAchieved())
        .isTrue();
    assertThat(service.completed(product, "experiment:88")).isTrue();
    service.execute(process, activities.get("ready"), product, "experiment:88");
    verify(tasks, never())
        .findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            anyLong(), anyString());
    assertThat(saved).hasSize(6);
    assertThat(saved.get(8L).getOccurrenceNumber()).isEqualTo(1);
    assertThat(experiment.getFollowUpActionUrl()).isEqualTo("https://example.test/kit");
    assertThat(experiment.getCommercialCheckoutUrl()).isEqualTo("https://example.test/pay");
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.USER_STOPPED);
    assertThat(experiment.getFacebookReleaseRequestedAt()).isNull();
    assertThat(
            json.readTree(saved.get(8L).getObjectiveEvidenceJson()).path("salesProven").asBoolean())
        .isFalse();
  }

  /** Mudança de ativos recusa o callback anterior e exige nova ocorrência das preparações. */
  @Test
  void rejectsLateCallbackAndInvalidatesCompletedPreparation() throws Exception {
    prepare();
    var request = approved();
    var task = task("humanExperienceReview");
    snapshot.put("fingerprint", "changed");
    assertThatThrownBy(() -> service.apply(task, request))
        .hasMessageContaining("renove a atividade");
    prepare();
    assertThatThrownBy(() -> service.apply(task, request)).hasMessageContaining("mudaram");
    assertThat(saved.get(1L).getOccurrenceNumber()).isEqualTo(2);
  }

  /** Um gate omitido ou negativo jamais se transforma em conclusão técnica bem-sucedida. */
  @Test
  void rejectsIncompleteReviewAndFailedPredecessor() throws Exception {
    prepare();
    var request = approved();
    var result = (ObjectNode) json.readTree(request.resultJson());
    result.withArray("gateChecks").remove(0);
    assertThatThrownBy(
            () ->
                service.apply(
                    task("humanExperienceReview"),
                    new CompleteAgentTaskRequest(result.toString(), request.evidenceJson())))
        .hasMessageContaining("omitiu");
    when(predecessors.readiness(any(), any(), anyString()))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(false, "Conclua a revisão anterior"));
    assertThat(
            service.readiness(process, activities.get("ready"), product, "experiment:88").ready())
        .isFalse();
  }

  /** Mantém a reprovação no relatório e impede que ela libere a consolidação comercial. */
  @Test
  void recordsFunctionalRejectionWithoutTreatingItAsTechnicalFailureOrApproval() throws Exception {
    prepare();
    var request = approved();
    var task = task("humanExperienceReview");
    var result = (ObjectNode) json.readTree(request.resultJson());
    result.put("decision", "ADJUST");
    result.withArray("requiredChanges").add("Comprovar entrega do kit");
    var rejected = new CompleteAgentTaskRequest(result.toString(), request.evidenceJson());
    assertThat(service.apply(task, rejected))
        .isEqualTo(AgentTaskCompletionHook.CompletionDisposition.COMPLETE);
    task.setStatus("COMPLETED");
    task.setResultJson(result.toString());
    task.setEvidenceJson(request.evidenceJson());
    reviews.add(task);
    assertThat(
            service.readiness(process, activities.get("ready"), product, "experiment:88").reason())
        .contains("não aprovou");
    result.put("decision", "APPROVED");
    assertThatThrownBy(
            () ->
                service.apply(
                    task, new CompleteAgentTaskRequest(result.toString(), request.evidenceJson())))
        .hasMessageContaining("correção pendente");
  }

  /** Uma nova revisão em execução impede reutilizar a antiga, mesmo com os mesmos ativos. */
  @Test
  void refusesSupersededApprovalWhileNewReviewIsRunning() throws Exception {
    prepare();
    completeReview("humanExperienceReview");
    var newer = task("humanExperienceReview");
    newer.setStatus("IN_PROGRESS");
    reviews.add(newer);
    assertThatThrownBy(
            () ->
                service.currentReview(process, "experiment:88", "humanExperienceReview", snapshot))
        .hasMessageContaining("ainda não foi concluído");
  }

  /** Checkouts divergentes, economia vencida e prova ausente produzem orientação sem escrita. */
  @Test
  void blocksMissingProofConflictingCheckoutAndStaleEconomics() {
    snapshot.put("productProofInPage", false);
    assertThat(
            service.readiness(process, activities.get("entry"), product, "experiment:88").reason())
        .contains("arquivos exatos");
    snapshot.withArray("productProof").removeAll();
    assertThat(
            service.readiness(process, activities.get("entry"), product, "experiment:88").reason())
        .contains("prova real");
    experiment.setCommercialCheckoutUrl("https://other.test/pay");
    assertThat(
            service
                .readiness(process, activities.get("checkout"), product, "experiment:88")
                .reason())
        .contains("diverge");
    snapshot.withObject("/financialPlan").put("stale", true);
    assertThat(
            service
                .readiness(process, activities.get("economics"), product, "experiment:88")
                .reason())
        .contains("vencido");
    verify(instances, never()).saveAndFlush(any());
  }

  /** Exige a preparação antes dos pareceres, evitando consumir revisão paga sem insumos. */
  @Test
  void refusesReviewBeforePreparationAndReadyWithoutReviews() {
    assertThatThrownBy(() -> service.apply(task("humanExperienceReview"), approved()))
        .hasMessageContaining("Conclua");
    prepare();
    assertThat(
            service.readiness(process, activities.get("ready"), product, "experiment:88").reason())
        .contains("Falta o parecer");
  }

  /** Percorre as atividades determinísticas usando as mesmas fontes e referência. */
  private void prepare() {
    QuartzoCommercialChecks.PREPARATION.forEach(
        step -> service.execute(process, activities.get(step), product, "experiment:88"));
  }

  /** Simula somente o modelo externo; o callback e a validação executam o serviço real. */
  private void completeReview(String step) throws Exception {
    var task = task(step);
    var request = approved();
    assertThat(service.apply(task, request))
        .isEqualTo(AgentTaskCompletionHook.CompletionDisposition.COMPLETE);
    task.setStatus("COMPLETED");
    task.setResultJson(request.resultJson());
    task.setEvidenceJson(request.evidenceJson());
    reviews.add(task);
  }

  /** Cria tarefa isolada com o processo e a origem que o backend enviaria ao executor. */
  private AgentTask task(String step) {
    var task = new AgentTask();
    task.setId(100L + reviews.size());
    task.setProcessDefinition(process);
    task.setProcessActivityId(step);
    task.setSourceReference("experiment:88");
    return task;
  }

  /** Produz a resposta sintética dos dez critérios, explicitamente fora de métricas comerciais. */
  private CompleteAgentTaskRequest approved() {
    var result = json.createObjectNode().put("decision", "APPROVED");
    var gates = result.putArray("gateChecks");
    result.putArray("requiredChanges");
    result.putArray("evidence").add("Fontes sintéticas verificadas");
    QuartzoCommercialService.REVIEW_GATES.forEach(
        g ->
            gates
                .addObject()
                .put("gate", g)
                .put("status", "PASS")
                .put("evidence", "Evidência sintética local"));
    var evidence = json.createObjectNode();
    evidence.set("quartzoScope", snapshot.deepCopy());
    return new CompleteAgentTaskRequest(result.toString(), evidence.toString());
  }
}
