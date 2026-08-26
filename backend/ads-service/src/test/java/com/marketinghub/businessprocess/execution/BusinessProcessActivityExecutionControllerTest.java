package com.marketinghub.businessprocess.execution;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.businessprocess.execution.controller.BusinessProcessActivityExecutionController;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
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
}
