package com.marketinghub.agenttask;

import com.marketinghub.researchintelligence.v1.service.select.ResearchIntelligenceSelectionResponse;
import java.time.Instant;

/** Responsabilidade: entregar ao executor uma atividade BPM elegível e auditável. */
public record AgentTaskPendingResponse(
    Long taskId,
    String agentKey,
    String processCode,
    Integer processVersion,
    String activityId,
    String activityName,
    String title,
    String description,
    String sourceReference,
    Instant receivedAt,
    AgentTaskExecutionResourceResponse executionResource,
    AgentTaskTargetResponse taskTarget,
    String processContextJson,
    ResearchIntelligenceSelectionResponse researchIntelligence) {

  /** Preserva integrações internas anteriores ao recurso opcional da atividade. */
  public AgentTaskPendingResponse(
      Long taskId,
      String agentKey,
      String processCode,
      Integer processVersion,
      String activityId,
      String activityName,
      String title,
      String description,
      String sourceReference,
      Instant receivedAt,
      String processContextJson) {
    this(
        taskId,
        agentKey,
        processCode,
        processVersion,
        activityId,
        activityName,
        title,
        description,
        sourceReference,
        receivedAt,
        null,
        null,
        processContextJson,
        null);
  }
}
