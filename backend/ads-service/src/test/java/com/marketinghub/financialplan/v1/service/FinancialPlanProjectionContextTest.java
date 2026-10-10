package com.marketinghub.financialplan.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTaskResponse;
import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.financialagent.FinancialAgentExecution;
import com.marketinghub.financialagent.service.*;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.getplan.PlanView;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import jakarta.validation.Validation;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Responsabilidade: proteger o contexto completo do plano na integração real com a fila de Plutus.
 */
class FinancialPlanProjectionContextTest {
  /**
   * Mantém a personalização comercial desconhecida e encaminha a hipótese econômica dos dois
   * produtos privados, preservando a identidade e a necessidade de confirmação da entrega.
   */
  @Test
  void contratoPrivadoLegadoPermiteSomenteProjecaoDaHipotese() throws Exception {
    for (long id : List.of(95111L, 95222L)) {
      var result = privateProjection(id, privateContract(id));
      assertThat(result.ready()).isTrue();
      var contract = (Map<?, ?>) result.context().get("deliveryContract");
      assertThat(contract.get("personalization")).isNull();
      assertThat(contract.get("proposedPersonalizedAi")).isEqualTo(true);
      assertThat(contract.get("contractScope")).isEqualTo("PRIVATE_AGENT_VALIDATION");
      assertThat(contract.get("commercialDeliveryConfirmationRequired")).isEqualTo(true);
      assertThat(contract.get("sourceReference"))
          .isEqualTo("product:" + id + "@PDE_AGENT_VALIDATED_V1:validation-contract");
    }
  }

  /** Bloqueia contradição explícita, outra identidade e qualquer relaxamento do escopo privado. */
  @Test
  void declaracaoContraditoriaOuOrigemIncompativelContinuaBloqueada() throws Exception {
    var json = new ObjectMapper();
    for (String change : List.of("personalization", "sourceReference", "campaign", "marker")) {
      var contract = (ObjectNode) json.readTree(privateContract(95111L));
      switch (change) {
        case "personalization" ->
            ((ObjectNode) contract.path("delivery")).put("personalization", false);
        case "sourceReference" ->
            ((ObjectNode) contract.path("agentValidationPlan"))
                .put("sourceReference", "product:95222@agent-validation-v1");
        case "campaign" ->
            ((ObjectNode) contract.path("agentValidationPlan")).put("campaignAuthorized", true);
        default -> ((ObjectNode) contract.path("agentValidationPlan")).remove("internalMarker");
      }
      assertThat(privateProjection(95111L, contract.toString()).blockers())
          .anySatisfy(blocker -> assertThat(blocker).contains("personalização financeira diverge"));
    }
  }

  /** Preserva o caminho comercial anteriormente válido e recusa o campo ausente nesse regime. */
  @Test
  void contratoComercialExigeDeclaracaoCanonica() throws Exception {
    var json = new ObjectMapper();
    var contract = (ObjectNode) json.readTree(privateContract(95111L));
    contract.remove("agentValidationPlan");
    ((ObjectNode) contract.path("delivery")).put("personalization", true);
    var approved = privateProjection(95111L, contract.toString());
    assertThat(approved.ready()).isTrue();
    assertThat(((Map<?, ?>) approved.context().get("deliveryContract")).get("personalization"))
        .isEqualTo(true);
    ((ObjectNode) contract.path("delivery")).remove("personalization");
    assertThat(privateProjection(95111L, contract.toString()).ready()).isFalse();
  }

  /**
   * Reproduz a forma do contrato privado persistido, com identificadores exclusivamente sintéticos.
   */
  private String privateContract(long id) {
    return """
        {"format":"Experiência web restrita e mobile-first",
         "delivery":{"privatePrototype":{"checkoutMode":"SIMULATED_NO_CHARGE"}},
         "agentValidationPlan":{"contractVersion":"PDE_AGENT_VALIDATION_V1",
           "sourceReference":"product:%d@agent-validation-v1",
           "trafficClass":"AGENT_VALIDATION","internalMarker":"mh_internal_test",
           "paymentEnabled":false,"campaignAuthorized":false}}
        """
        .formatted(id);
  }

  /**
   * Monta a projeção determinística da preparação, sem consultar serviços ou consumidores externos.
   */
  private FinancialProjectionContext.Result privateProjection(long id, String contract)
      throws Exception {
    var json = new ObjectMapper().findAndRegisterModules();
    var input =
        (ObjectNode)
            json.readTree(getClass().getResourceAsStream("/financial-plan/assumptions.json"));
    input.put("productVersion", "PDE_AGENT_VALIDATED_V1");
    input.putObject("preparation").put("supportDays", 7).put("personalizedAi", true);
    var assumptions = json.treeToValue(input, PlanAssumptions.class);
    var product = new Product();
    product.setId(id);
    product.setValidationDefinitionVersion("PDE_AGENT_VALIDATED_V1");
    product.setValidationDefinitionJson(contract);
    var plan = new CommercialPlan();
    plan.setId(id + 100);
    var view =
        new PlanView(
            id + 200,
            "PRODUCT",
            id,
            Environment.LIVE,
            "Projeção sintética",
            1,
            null,
            plan.getId(),
            1,
            "Fixture local",
            Instant.now(),
            assumptions,
            FinancialPlanCalculator.evaluate(assumptions),
            false,
            List.of(),
            true,
            null);
    return FinancialProjectionContext.build(product, plan, 1, view, json);
  }

  /**
   * Preserva fontes extensas, cálculos e revisão na tarefa e no request persistido sem truncamento.
   */
  @Test
  void preservaContextoCompletoNaFilaExistente() throws Exception {
    var json = new ObjectMapper().findAndRegisterModules();
    var input =
        (ObjectNode)
            json.readTree(getClass().getResourceAsStream("/financial-plan/assumptions.json"));
    input.put("evidence", "Fonte sintética e premissa documentada. ".repeat(150));
    var assumptions = json.treeToValue(input, PlanAssumptions.class);
    var context =
        json.writeValueAsString(
            Map.of(
                "financialPlanId",
                95111L,
                "financialPlanRevision",
                2,
                "productId",
                95102L,
                "assumptions",
                assumptions,
                "deterministicEvaluation",
                FinancialPlanCalculator.evaluate(assumptions)));
    var request = new StartRevenueProjectionRequest(context);
    assertThat(context.length()).isGreaterThan(4000);
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(assumptions)).isEmpty();
      assertThat(factory.getValidator().validate(request)).isEmpty();
    }
    var repository = mock(FinancialAgentExecutionRepository.class);
    var plans = mock(CommercialPlanService.class);
    var versions = mock(CommercialPlanVersionService.class);
    var tasks = mock(AgentTaskService.class);
    var plan = new CommercialPlan();
    plan.setId(95102L);
    plan.setName("Plano comercial sintético");
    when(plans.getPlan(95102L)).thenReturn(plan);
    when(versions.current(95102L))
        .thenReturn(new CommercialPlanVersionDto(1L, 95102L, 3, "{}", "USER", "fixture", null));
    var task = mock(AgentTaskResponse.class);
    when(task.id()).thenReturn(501L);
    when(tasks.createByHuman(any())).thenReturn(task);
    when(repository.save(any(FinancialAgentExecution.class)))
        .thenAnswer(
            i -> {
              FinancialAgentExecution e = i.getArgument(0);
              e.setId(502L);
              return e;
            });
    var service =
        new FinancialAgentService(
            repository, plans, json, mock(StudioCostLedgerService.class), versions, tasks);
    var response = service.startRevenueProjection(95102L, request);
    var captured = ArgumentCaptor.forClass(FinancialAgentExecution.class);
    verify(repository).save(captured.capture());
    assertThat(captured.getValue().getProjectionRequest()).isEqualTo(context);
    assertThat(response.commercialPlanVersion()).isEqualTo(3);
    assertThat(response.agentTaskId()).isEqualTo(501L);
    verify(tasks).createByHuman(argThat(t -> t.description().contains(context)));
  }

  /** Rejeita contexto ilimitado preservando um limite explícito para entradas administrativas. */
  @Test
  void limitaContextoSemCortarFontesSilenciosamente() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(
              factory.getValidator().validate(new StartRevenueProjectionRequest("x".repeat(64001))))
          .isNotEmpty();
    }
  }
}
