package com.marketinghub.businessprocess.execution;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.businessprocess.execution.controller.BusinessProcessActivityExecutionController;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionHistoryResponse;
import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: comprovar o contrato HTTP do histórico recente das atividades BPM. */
class BusinessProcessActivityExecutionControllerTest {

  /** Expõe processo, atividade e auditoria da tarefa mais recente em contrato estruturado. */
  @Test
  void getsRecentBusinessProcessActivityExecutions() throws Exception {
    var service = mock(BusinessProcessActivityExecutionService.class);
    var execution =
        new BusinessProcessActivityExecutionResponse(
            126L,
            22L,
            1,
            "Comprovar dor e demanda · rodada 4",
            "COMPLETED",
            "pde-opportunity:round-4",
            "market-radar",
            "Argos",
            "{\"decision\":\"APPROVE\"}",
            "{\"sources\":2}",
            null,
            2834L,
            2304L,
            5861L,
            new BigDecimal("0.01347240"),
            "ESTIMATED",
            Instant.parse("2026-08-20T21:40:00Z"),
            Instant.parse("2026-08-20T21:40:00Z"),
            Instant.parse("2026-08-20T21:41:24Z"),
            "gpt-5.4-mini-2026-03-17",
            "high",
            null,
            "Comprove a dor.");
    when(service.recentExecutions(37L, "evidence"))
        .thenReturn(
            new BusinessProcessActivityExecutionHistoryResponse(
                37L,
                "pde-opportunity-discovery",
                "Descoberta e priorização da oportunidade PDE",
                4,
                "RETIRED",
                "evidence",
                "Comprovar dor e demanda",
                "Argos",
                List.of(execution)));
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new BusinessProcessActivityExecutionController(service))
            .build();

    mockMvc
        .perform(get("/api/business-processes/37/activities/evidence/executions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.processName").value("Descoberta e priorização da oportunidade PDE"))
        .andExpect(jsonPath("$.activityName").value("Comprovar dor e demanda"))
        .andExpect(jsonPath("$.executions[0].assignedAgentNickname").value("Argos"))
        .andExpect(jsonPath("$.executions[0].modelCode").value("gpt-5.4-mini-2026-03-17"))
        .andExpect(jsonPath("$.executions[0].reasoningEffort").value("high"))
        .andExpect(jsonPath("$.executions[0].promptSent").value("Comprove a dor."));
  }

  /** Expõe atividades e tarefas segregadas pelo produto e processo selecionados. */
  @Test
  void getsProductProcessActivityExecutions() throws Exception {
    var service = mock(BusinessProcessActivityExecutionService.class);
    var execution =
        new BusinessProcessActivityExecutionResponse(
            243L,
            18L,
            4,
            "Construir e homologar a landing",
            "COMPLETED",
            "commercial-plan:4@v3:journey",
            "landing-generator",
            "Dédalo",
            "{\"decision\":\"APPROVE\"}",
            "{\"sources\":4}",
            null,
            947056L,
            796288L,
            25323L,
            new BigDecimal("1.42804720"),
            "ESTIMATED",
            Instant.parse("2026-08-27T03:26:19Z"),
            Instant.parse("2026-08-27T03:26:45Z"),
            Instant.parse("2026-08-27T03:35:14Z"),
            "gpt-5.6-sol",
            "high",
            "Rigel",
            "Construa a landing.");
    var activity =
        new ProductProcessActivityExecutionGroupResponse(
            119L,
            "select",
            "Selecionar provas reais da entrega",
            "Selecionar ativos aprovados.",
            "Dédalo",
            1,
            true,
            1,
            List.of(execution));
    when(service.productProcessExecutions(18L, 9L))
        .thenReturn(
            new ProductProcessActivityExecutionHistoryResponse(
                9L,
                "Kit WhatsApp Pronto",
                "Rigel",
                18L,
                "landing-page-generation",
                "Geração de landing page",
                4,
                "PUBLISHED",
                8,
                6,
                3,
                new BigDecimal("1.61762400"),
                "PARTIAL",
                List.of(activity)));
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new BusinessProcessActivityExecutionController(service))
            .build();

    mockMvc
        .perform(get("/api/business-processes/18/products/9/activity-executions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productInternalName").value("Rigel"))
        .andExpect(jsonPath("$.processName").value("Geração de landing page"))
        .andExpect(jsonPath("$.uniqueTaskCount").value(3))
        .andExpect(jsonPath("$.activities[0].activityId").value("select"))
        .andExpect(jsonPath("$.activities[0].tasks[0].taskId").value(243));
  }
}
