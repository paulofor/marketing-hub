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
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.dto.CommercialPlanVersionDto;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.planning.service.CommercialPlanVersionService;
import com.marketinghub.repository.jpa.financialagent.FinancialAgentExecutionRepository;
import jakarta.validation.Validation;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Responsabilidade: proteger o contexto completo do plano na integração real com a fila de Plutus.
 */
class FinancialPlanProjectionContextTest {
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
