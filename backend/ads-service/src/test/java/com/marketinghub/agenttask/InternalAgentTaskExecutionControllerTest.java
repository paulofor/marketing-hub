package com.marketinghub.agenttask;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: validar o transporte HTTP dos contratos versionados da fila de agentes. */
class InternalAgentTaskExecutionControllerTest {

  /** Encaminha o handshake audiovisual sem permitir recuperação implícita por worker legado. */
  @Test
  void forwardsVersionedWorkerContractToCanonicalClaim() throws Exception {
    AgentTaskService service = mock(AgentTaskService.class);
    when(service.claimEligibleProcessTask(
            "videomaker",
            "creative-production-approval",
            "audiovisual",
            "video-management-service",
            "APOLLO_AUDIOVISUAL_V1"))
        .thenReturn(Optional.empty());
    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(
                new InternalAgentTaskExecutionController(
                    service, mock(AgentTaskVisualEvidenceService.class)))
            .build();

    mvc.perform(
            get("/api/internal/agent-tasks/videomaker/stage-executions/pending")
                .param("processCode", "creative-production-approval")
                .param("activityId", "audiovisual")
                .param("executionResourceCode", "video-management-service")
                .param("workerContract", "APOLLO_AUDIOVISUAL_V1"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    verify(service)
        .claimEligibleProcessTask(
            "videomaker",
            "creative-production-approval",
            "audiovisual",
            "video-management-service",
            "APOLLO_AUDIOVISUAL_V1");
  }
}
