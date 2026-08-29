package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: apresentar a situação operacional de uma atividade na instância BPM. */
public record ProcessInstanceTaskResponse(
    Long taskId,
    Long activityInstanceId,
    Integer attemptNumber,
    String activityId,
    String activityName,
    String agentKey,
    String agentNickname,
    String taskStatus,
    String operationalState,
    String stateReason,
    AgentTaskFailureAuditResponse failureAudit,
    String executionMode,
    String modelCode,
    String reasoningEffort,
    String promptSent,
    String agentPromptPart,
    String activityPromptPart,
    AgentTaskBlockerGuidanceResponse blockerGuidance,
    List<AgentTaskAuditLinkResponse> accessedUrls,
    List<AgentTaskVisualEvidenceResponse> visualEvidence,
    JsonNode visualAudit,
    JsonNode purchaseEmotion,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    BigDecimal estimatedCostUsd,
    String costEstimationStatus,
    Instant receivedAt,
    Instant deliveredAt) {}
