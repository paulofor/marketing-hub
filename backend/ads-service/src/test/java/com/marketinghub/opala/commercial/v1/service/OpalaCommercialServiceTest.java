package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleCommercialReadiness;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleCommercialPreparation;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: provar materialização governada e recusa de callbacks inconsistentes. */
class OpalaCommercialServiceTest {
  private final ObjectMapper json = new ObjectMapper();
  private final OpalaCommercialContext context = mock(OpalaCommercialContext.class);
  private final OpalaCommercialMaterialization materialization =
      mock(OpalaCommercialMaterialization.class);
  private final LearningCycleCommercialReadiness readiness =
      mock(LearningCycleCommercialReadiness.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final OpalaCommercialService service =
      new OpalaCommercialService(
          context, materialization, readiness, tasks, mock(OpalaCommercialRouting.class));
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final Experiment experiment =
      Experiment.builder().id(92L).unitPrice(new BigDecimal("67")).build();
  private final AgentTask task = new AgentTask();
  private final String identity =
      """
      {"productId":4,"experimentId":92,"cycleId":2,"productVersion":"fixture-v12",
       "priceBrl":67,"budgetLimitBrl":100,"windowEnd":"2099-10-31T23:59:59Z",
       "productContract":{},
       "financialPlan":{"id":1,"revision":1,"status":"READY","assumptions":{"validUntil":"2099-10-31",
         "priceBrl":67,"maximumCacBrl":15,"costs":{"refundPercent":12}},
         "deterministicEvaluation":{"scenarios":[{"code":"BASE","contributionBeforeCacBrl":25,"contributionAfterCacBrl":10}]}}}
      """;

  /** Monta uma ocorrência sintética com a mesma correlação exigida no callback real. */
  @BeforeEach
  void setup() throws Exception {
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setProductVersion("fixture-v12");
    cycle.setVersionChangedAt(Instant.now().minusSeconds(60));
    cycle.setBudgetLimitBrl(new BigDecimal("100"));
    cycle.setWindowEnd(Instant.parse("2099-10-31T23:59:59Z"));
    var process = new BusinessProcessDefinition();
    process.setId(100L);
    process.setProcessCode(OpalaCommercialContext.CODE);
    task.setProcessDefinition(process);
    task.setSourceReference("experiment:92");
    task.setCreatedAt(Instant.now());
    when(context.scope("experiment:92"))
        .thenReturn(new OpalaCommercialContext.Scope(cycle, experiment));
    when(context.read(anyString())).thenAnswer(i -> json.readTree((String) i.getArgument(0)));
    when(context.snapshot("experiment:92"))
        .thenReturn((com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(identity));
  }

  /** Cada atividade de preparação chama somente a materialização correspondente. */
  @ParameterizedTest
  @ValueSource(strings = {"entry", "creative", "checkout", "targeting"})
  void materializesOnlyItsOwnActivity(String activity) throws Exception {
    task.setProcessActivityId(activity);
    assertThat(service.apply(task, request("{\"decision\":\"READY\",\"instruction\":{}}")))
        .isEqualTo(AgentTaskCompletionHook.CompletionDisposition.COMPLETE);
    verify(materialization).apply(eq(activity), any(), eq(json.readTree("{}")));
  }

  /** Outra versão não pode alterar vínculos comerciais desta ocorrência. */
  @Test
  void rejectsOldVersionBeforeMaterialization() {
    task.setProcessActivityId("entry");
    var request =
        new CompleteAgentTaskRequest(
            "{\"decision\":\"READY\"}",
            "{\"opalaScope\":" + identity.replace("fixture-v12", "fixture-v11") + "}");
    assertThatThrownBy(() -> service.apply(task, request)).hasMessageContaining("outra ocorrência");
    verifyNoInteractions(materialization);
  }

  /** Uma tarefa anterior à versão atual não recebe aprovação retroativa. */
  @Test
  void rejectsTaskCreatedBeforeVersionChange() {
    task.setCreatedAt(cycle.getVersionChangedAt().minusSeconds(1));
    assertThatThrownBy(() -> service.apply(task, request("{\"decision\":\"READY\"}")))
        .hasMessageContaining("antes da versão");
  }

  /** Parecer sem margem positiva não conclui economia nem concede orçamento. */
  @Test
  void rejectsNonPositiveContribution() {
    task.setProcessActivityId("economics");
    assertThatThrownBy(
            () ->
                service.apply(
                    task,
                    request(
                        "{\"decision\":\"APPROVE\",\"economics\":{\"offerPriceBrl\":67,\"variableCostPerSaleBrl\":70,\"contributionPerSaleBrl\":-3}}")))
        .hasMessageContaining("economia aprovada");
    verifyNoInteractions(materialization);
  }

  /** Aceita o parecer arredondado que separa contribuição unitária e CAC máximo. */
  @Test
  void acceptsRoundedContributionBeforeCac() {
    task.setProcessActivityId("economics");
    var result =
        """
        {"decision":"APPROVE","scenarios":[{},{},{}],"economics":{
          "offerPriceBrl":67,"variableCostPerSaleBrl":42,"contributionPerSaleBrl":25,
          "contributionMarginPercent":37.31,"maxCacBrl":15,"maxBudgetBrl":100,
          "expectedRefundPercent":12,"deadline":"2099-10-31"}}
        """;

    assertThat(service.apply(task, request(result)))
        .isEqualTo(AgentTaskCompletionHook.CompletionDisposition.COMPLETE);
  }

  /** Rejeita parecer que converte CAC em custo variável e ainda o mantém como limite separado. */
  @Test
  void rejectsContributionAfterCacAsUnitContribution() {
    task.setProcessActivityId("economics");
    var result =
        """
        {"decision":"APPROVE","scenarios":[{},{},{}],"economics":{
          "offerPriceBrl":67,"variableCostPerSaleBrl":57,"contributionPerSaleBrl":10,
          "contributionMarginPercent":14.93,"maxCacBrl":15,"maxBudgetBrl":100,
          "expectedRefundPercent":12,"deadline":"2099-10-31"}}
        """;

    assertThatThrownBy(() -> service.apply(task, request(result))).hasMessageContaining("diverge");
  }

  /** Mantém a conclusão quando a mesma revisão volta do banco com escala numérica diferente. */
  @Test
  void keepsCompletedEconomicsForSameImmutableFinancialPlanRevision() throws Exception {
    task.setProcessActivityId("economics");
    task.setStatus("COMPLETED");
    task.setEvidenceJson("{\"opalaScope\":" + identity + "}");
    task.setResultJson("{\"economics\":{\"deadline\":\"2099-10-31\"}}");
    when(tasks.findCompletedActivitySnapshots(
            eq(100L),
            eq("experiment:92"),
            eq("economics"),
            any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(
            List.of(
                new AgentTaskActivityCompletionSnapshot(
                    task.getId(), task.getEvidenceJson(), task.getResultJson())));
    var current =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(
                identity
                    .replace("\"priceBrl\":67", "\"priceBrl\":67.0")
                    .replace("\"budgetLimitBrl\":100", "\"budgetLimitBrl\":100.00"));
    when(context.snapshot("experiment:92")).thenReturn(current);
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("economics");

    assertThat(
            service.requiresFreshExecution(
                task.getProcessDefinition(), activity, null, "experiment:92"))
        .isFalse();
    verify(tasks, never())
        .findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            anyLong(), anyString());
  }

  /** Exige nova análise quando outra revisão financeira imutável substitui a já aprovada. */
  @Test
  void requiresFreshEconomicsForAnotherFinancialPlanRevision() throws Exception {
    task.setProcessActivityId("economics");
    task.setStatus("COMPLETED");
    task.setEvidenceJson("{\"opalaScope\":" + identity + "}");
    task.setResultJson("{\"economics\":{\"deadline\":\"2099-10-31\"}}");
    when(tasks.findCompletedActivitySnapshots(
            eq(100L),
            eq("experiment:92"),
            eq("economics"),
            any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(
            List.of(
                new AgentTaskActivityCompletionSnapshot(
                    task.getId(), task.getEvidenceJson(), task.getResultJson())));
    var current =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(identity.replace("\"revision\":1", "\"revision\":2"));
    when(context.snapshot("experiment:92")).thenReturn(current);
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("economics");

    assertThat(
            service.requiresFreshExecution(
                task.getProcessDefinition(), activity, null, "experiment:92"))
        .isTrue();
    verify(tasks, never())
        .findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            anyLong(), anyString());
  }

  /** Revisão aprovada pelo modelo continua bloqueada quando faltam ativos reais. */
  @ParameterizedTest
  @ValueSource(strings = {"humanExperienceReview", "commercialIntegrityReview"})
  void refusesApprovalWithoutCommercialInputs(String activity) {
    task.setProcessActivityId(activity);
    when(readiness.inspect(cycle))
        .thenReturn(
            new LearningCycleCommercialPreparation(
                false, "Falta publicar a versão", "/experiments/92", List.of()));
    assertThatThrownBy(() -> service.apply(task, request("{\"decision\":\"APPROVED\"}")))
        .hasMessageContaining("condições comerciais mudaram");
  }

  /** Classifica ativos substituídos como conflito funcional, nunca como erro transitório 500. */
  @Test
  void reportsChangedAssetsAsConflict() throws Exception {
    task.setProcessActivityId("humanExperienceReview");
    when(context.snapshot("experiment:92"))
        .thenReturn(
            (com.fasterxml.jackson.databind.node.ObjectNode)
                json.readTree(
                    identity.replace(
                        "\"productVersion\":\"fixture-v12\"",
                        "\"productVersion\":\"fixture-v13\"")));

    assertThatThrownBy(() -> service.apply(task, request("{\"decision\":\"APPROVED\"}")))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
        .hasMessageContaining("ativos avaliados mudaram");
  }

  /** Aceita o callback que preserva os mesmos ativos quando o banco apenas reordena uma coleção. */
  @Test
  void acceptsHumanReviewWhenEquivalentAssetsReturnInAnotherOrder() throws Exception {
    task.setProcessActivityId("humanExperienceReview");
    var initial = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(identity);
    initial.put("contractVersion", "OPALA_COMMERCIAL_PREPARATION_V1");
    initial.put("destinationSource", "EXPERIMENT");
    var initialAudience = initial.putArray("approvedAudienceElements");
    initialAudience.addObject().put("id", 214).put("term", "Fragrances");
    initialAudience.addObject().put("id", 235).put("term", "Personal stylist");
    initial.put("publicationAuthorized", false);
    initial.put("mediaSpendAuthorized", false);
    var current = initial.deepCopy();
    var currentAudience = current.putArray("approvedAudienceElements");
    currentAudience.addObject().put("id", 235).put("term", "Personal stylist");
    currentAudience.addObject().put("id", 214).put("term", "Fragrances");
    when(context.snapshot("experiment:92")).thenReturn(current);
    when(readiness.inspect(cycle))
        .thenReturn(
            new LearningCycleCommercialPreparation(true, "Pronto", "/experiments/92", List.of()));

    assertThat(
            service.apply(
                task,
                request(
                    """
                    {"decision":"APPROVED","gateChecks":[
                      {"status":"PASS"},{"status":"PASS"},{"status":"PASS"},{"status":"PASS"},
                      {"status":"PASS"},{"status":"PASS"},{"status":"PASS"},{"status":"PASS"}],
                     "evidence":["captura:453"],"requiredChanges":[]}
                    """,
                    initial.toString())))
        .isEqualTo(AgentTaskCompletionHook.CompletionDisposition.COMPLETE);
  }

  /** Mantém o conflito quando um elemento de público realmente muda após a revisão. */
  @Test
  void rejectsHumanReviewWhenReviewedAudienceChanges() throws Exception {
    task.setProcessActivityId("humanExperienceReview");
    var initial = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(identity);
    initial.put("contractVersion", "OPALA_COMMERCIAL_PREPARATION_V1");
    initial.put("destinationSource", "EXPERIMENT");
    initial
        .putArray("approvedAudienceElements")
        .addObject()
        .put("id", 214)
        .put("term", "Fragrances");
    initial.put("publicationAuthorized", false);
    initial.put("mediaSpendAuthorized", false);
    var current = initial.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode)
            current.path("approvedAudienceElements").get(0))
        .put("term", "Luxury fragrances");
    when(context.snapshot("experiment:92")).thenReturn(current);
    when(readiness.inspect(cycle))
        .thenReturn(
            new LearningCycleCommercialPreparation(true, "Pronto", "/experiments/92", List.of()));

    assertThatThrownBy(
            () ->
                service.apply(
                    task,
                    request(
                        "{\"decision\":\"APPROVED\",\"gateChecks\":[],\"evidence\":[],\"requiredChanges\":[]}",
                        initial.toString())))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("ativos avaliados mudaram");
  }

  /** Constrói o envelope de callback usando a identidade conhecida antes da execução. */
  private CompleteAgentTaskRequest request(String result) {
    return request(result, identity);
  }

  /** Constrói o envelope de callback com a identidade exata recebida pelo agente. */
  private CompleteAgentTaskRequest request(String result, String opalaScope) {
    return new CompleteAgentTaskRequest(result, "{\"opalaScope\":" + opalaScope + "}");
  }
}
