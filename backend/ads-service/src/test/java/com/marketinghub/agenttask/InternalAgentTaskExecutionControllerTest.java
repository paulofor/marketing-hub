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

  /** Preserva recibos, callback bruto e motivos de bloqueio no transporte HTTP da escuta. */
  @Test
  void exposesAudioReceiptsAndFinancialRefusal() throws Exception {
    var audio =
        mock(com.marketinghub.agenttask.audioreview.v1.service.AgentAudioReviewService.class);
    var controller =
        new InternalAgentTaskExecutionController(
            mock(AgentTaskService.class), mock(AgentTaskVisualEvidenceService.class));
    org.springframework.test.util.ReflectionTestUtils.setField(controller, "audioReviews", audio);
    var mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new com.marketinghub.web.ApiExceptionHandler())
            .build();
    var receipt =
        new com.marketinghub.agenttask.audioreview.v1.service.receipt.AudioReviewReceipt(
            9001L,
            9002L,
            "video-hash",
            "audio-hash",
            "COMPLETED",
            false,
            new java.math.BigDecimal("4.116"),
            new java.math.BigDecimal("0.03"),
            new com.fasterxml.jackson.databind.ObjectMapper()
                .createObjectNode()
                .put("verdict", "PASS"),
            null);
    when(audio.list("customer-agent", 9003)).thenReturn(java.util.List.of(receipt));
    when(audio.complete(
            org.mockito.ArgumentMatchers.eq("customer-agent"),
            org.mockito.ArgumentMatchers.eq(9003L),
            org.mockito.ArgumentMatchers.eq(9002L),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(receipt);
    when(audio.reserve(
            org.mockito.ArgumentMatchers.eq("customer-agent"),
            org.mockito.ArgumentMatchers.eq(9003L),
            org.mockito.ArgumentMatchers.any()))
        .thenThrow(
            new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "Saldo audiovisual insuficiente; nenhum consumo."));
    String path = "/api/internal/agent-tasks/customer-agent/stage-executions/9003/audio-reviews";
    mvc.perform(get(path))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$[0].assetId")
                .value(9002));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    path + "/9002/result")
                .contentType("application/json")
                .content("{\"rawResponse\":\"{\\\"fixture\\\":true}\"}"))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                .value("COMPLETED"));
    verify(audio)
        .complete(
            "customer-agent",
            9003,
            9002,
            new com.marketinghub.agenttask.audioreview.v1.service.result.AudioReviewResultRequest(
                "{\"fixture\":true}"));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .contentType("application/json")
                .content(
                    "{\"assetId\":9002,\"videoSha256\":\"v\",\"audioSha256\":\"a\",\"request\":{}}"))
        .andExpect(status().isConflict())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                .value("Saldo audiovisual insuficiente; nenhum consumo."));
  }

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
