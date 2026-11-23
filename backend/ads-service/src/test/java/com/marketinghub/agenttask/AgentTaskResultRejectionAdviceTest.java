package com.marketinghub.agenttask;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketinghub.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: garantir recusa semântica HTTP 422 mesmo com o tratador global de produção. */
class AgentTaskResultRejectionAdviceTest {
  /**
   * Prioriza o contrato recuperável nas duas rotas sem depender da ordem de descoberta de beans.
   */
  @Test
  void semanticRejectionPrecedesGlobalUnexpectedErrorHandler() throws Exception {
    var service = mock(AgentTaskService.class);
    var rejection =
        new AgentTaskResultRejectedException(new IllegalArgumentException("Referência inválida"));
    doThrow(rejection).when(service).completeClaimedProcessTask(anyString(), anyLong(), any());
    doThrow(rejection).when(service).replayRejectedResult(42001L);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new InternalAgentTaskExecutionController(service, null),
                new AgentTaskController(service, null))
            .setControllerAdvice(new ApiExceptionHandler(), new AgentTaskResultRejectionAdvice())
            .build();
    mvc.perform(
            post("/api/internal/agent-tasks/landing-generator/stage-executions/42001/result")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultJson\":\"{}\",\"evidenceJson\":\"{}\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value(AgentTaskResultRejectedException.CODE));
    mvc.perform(post("/api/agent-tasks/42001/replay-result"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value(AgentTaskResultRejectedException.CODE));
  }
}
