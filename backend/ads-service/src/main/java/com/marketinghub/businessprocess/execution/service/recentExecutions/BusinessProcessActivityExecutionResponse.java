package com.marketinghub.businessprocess.execution.service.recentExecutions;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.agenttask.AgentTaskAuditLinkResponse;
import com.marketinghub.agenttask.AgentTaskBlockerGuidanceResponse;
import com.marketinghub.agenttask.AgentTaskVisualEvidenceResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: apresentar uma tarefa BPM com sua auditoria operacional e de modelo. */
public record BusinessProcessActivityExecutionResponse(
    Long taskId,
    Long processDefinitionId,
    Integer processVersionNumber,
    String title,
    String status,
    String sourceReference,
    String assignedAgentKey,
    String assignedAgentNickname,
    String comments,
    String evidenceJson,
    String executionError,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    BigDecimal estimatedCostUsd,
    String costEstimationStatus,
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt,
    String modelCode,
    String executionMode,
    String reasoningEffort,
    String productInternalName,
    String promptSent,
    AgentTaskBlockerGuidanceResponse blockerGuidance,
    List<AgentTaskAuditLinkResponse> accessedUrls,
    List<AgentTaskVisualEvidenceResponse> visualEvidence,
    JsonNode visualAudit,
    JsonNode purchaseEmotion) {}
