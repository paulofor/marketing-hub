package com.marketinghub.businessprocess.document.service.recentDocuments;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.agenttask.AgentTaskAuditLinkResponse;
import com.marketinghub.agenttask.AgentTaskVisualEvidenceResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Contrato de leitura de um documento produzido por uma execução de atividade BPM. */
public record BusinessProcessActivityDocumentResponse(
    Long taskId,
    String title,
    String sourceReference,
    String assignedAgentKey,
    String assignedAgentNickname,
    String resultJson,
    String evidenceJson,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    BigDecimal estimatedCostUsd,
    String costEstimationStatus,
    Instant startedAt,
    Instant finishedAt,
    String modelCode,
    String executionMode,
    String reasoningEffort,
    String productInternalName,
    String promptSent,
    List<AgentTaskAuditLinkResponse> accessedUrls,
    List<AgentTaskVisualEvidenceResponse> visualEvidence,
    JsonNode visualAudit,
    JsonNode purchaseEmotion) {}
