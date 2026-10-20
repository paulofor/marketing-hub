package com.marketinghub.agenttask;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: validar o comando administrativo de recomposição e sua autoria. */
class CreativeRecompositionControllerTest {
  /** Encaminha apenas pedidos com motivo e operador, preservando o identificador informado. */
  @Test
  void validatesRecompositionCommand() throws Exception {
    var service = mock(AgentTaskService.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new AgentTaskController(service, mock(AgentTaskVisualEvidenceService.class)))
            .build();
    mvc.perform(
            post("/api/agent-tasks/767/creative-recomposition")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestedByName\":\"Operador\",\"reason\":\"Quebra editorial\"}"))
        .andExpect(status().isOk());
    verify(service)
        .recomposeCreative(
            eq(767L), eq(new RecomposeCreativeRequest("Operador", "Quebra editorial")));
    mvc.perform(
            post("/api/agent-tasks/1767/creative-recomposition")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestedByName\":\"\",\"reason\":\"\"}"))
        .andExpect(status().isBadRequest());
    verifyNoMoreInteractions(service);
  }
}
