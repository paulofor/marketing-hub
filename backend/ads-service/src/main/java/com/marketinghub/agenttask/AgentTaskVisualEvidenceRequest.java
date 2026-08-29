package com.marketinghub.agenttask;

import java.time.Instant;

/** Responsabilidade: receber os metadados determinísticos de um snapshot produzido pelo worker. */
public record AgentTaskVisualEvidenceRequest(
    String captureSessionId,
    String evidenceKey,
    String evidenceType,
    String deviceProfile,
    Integer pageNumber,
    Integer foldNumber,
    Integer viewportWidth,
    Integer viewportHeight,
    Integer pageHeightPx,
    Integer scrollY,
    String sourceUrl,
    String finalUrl,
    Instant capturedAt) {}
