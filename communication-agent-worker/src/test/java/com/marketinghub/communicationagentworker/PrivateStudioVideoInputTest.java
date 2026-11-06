package com.marketinghub.communicationagentworker;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: aceitar somente briefing privado com autoridade própria, sem fabricar
 * estratégia de Atena.
 */
class PrivateStudioVideoInputTest {
  /**
   * Contexto de vídeo elegível independe de nova estratégia comercial e recusa outro
   * projeto/tarefa.
   */
  @Test
  void privateVideoContextIsSpecificAndDoesNotApproveCommercialStrategy() throws Exception {
    var json = new ObjectMapper();
    var input =
        json.createObjectNode()
            .put("contractVersion", "STUDIO_PRIVATE_VIDEO_BRIEF_V1")
            .put("scope", "SINGLE_PRIVATE_VIDEO_ONLY")
            .put("taskId", 7884)
            .put("sourceReference", "experiment:7114")
            .put("receiptId", 7128)
            .put("totalLimitUsd", 15)
            .put("videoProductionCycleId", 7047)
            .put("videoProjectId", 7014)
            .put("productVersion", "private-video")
            .put("inputSha256", "a".repeat(64))
            .put("commercialPublicationAuthorized", false)
            .put("newStrategyApproved", false)
            .put("mediaAuthorized", false)
            .put("qualityApproved", false);
    input.putObject("project").put("id", 7014).put("campaignKey", "private-video");
    Map<String, Object> task =
        new HashMap<>(
            Map.of(
                "taskId",
                7884L,
                "sourceReference",
                "experiment:7114",
                "processCode",
                "pde-communication-sales-journey",
                "activityId",
                "communicationContract",
                "singleVideoSpendingRequired",
                true));
    task.put(
        "processContextJson",
        json.createObjectNode().set("privateStudioVideoBrief", input).toString());
    CommunicationAgentCodexRunner.validateInput(task);
    input.put("taskId", 999);
    assertThatThrownBy(() -> CommunicationAgentCodexRunner.validatePrivateStudioInput(input, task))
        .hasMessageContaining("contexto");
    input.put("taskId", 7884).put("newStrategyApproved", true);
    assertThatThrownBy(() -> CommunicationAgentCodexRunner.validatePrivateStudioInput(input, task))
        .hasMessageContaining("estratégia aprovada");
    input.put("newStrategyApproved", false);
    task.put("singleVideoSpendingRequired", false);
    assertThatThrownBy(() -> CommunicationAgentCodexRunner.validatePrivateStudioInput(input, task))
        .hasMessageContaining("contexto");
  }
}
