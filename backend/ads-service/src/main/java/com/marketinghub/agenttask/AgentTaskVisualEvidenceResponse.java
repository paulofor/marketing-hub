package com.marketinghub.agenttask;

import java.time.Instant;

/** Responsabilidade: expor metadados e acesso governado a uma prova visual de tarefa. */
public record AgentTaskVisualEvidenceResponse(
    Long id,
    String captureSessionId,
    String evidenceKey,
    String evidenceType,
    String label,
    String deviceProfile,
    Integer pageNumber,
    Integer foldNumber,
    Integer viewportWidth,
    Integer viewportHeight,
    Integer pageHeightPx,
    Integer scrollY,
    String sourceUrl,
    String finalUrl,
    String contentUrl,
    Long sizeBytes,
    String sha256,
    Instant capturedAt) {}
