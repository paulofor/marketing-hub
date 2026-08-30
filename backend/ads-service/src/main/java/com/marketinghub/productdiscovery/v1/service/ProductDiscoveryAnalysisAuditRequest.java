package com.marketinghub.productdiscovery.v1.service;

import com.marketinghub.agenttask.AgentTaskAccessedUrlRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Responsabilidade: receber a auditoria da síntese factual executada por Argos. */
public record ProductDiscoveryAnalysisAuditRequest(
    @NotBlank @Size(max = 16_777_215) String rawResponse,
    @NotBlank @Size(max = 128) String model,
    @NotBlank @Size(max = 24) String executionMode,
    @Size(max = 16_777_215) String promptSent,
    @Size(max = 16_777_215) String agentPromptPart,
    @Size(max = 16_777_215) String activityPromptPart,
    @Size(max = 32) String reasoningEffort,
    Long inputTokens,
    Long cachedInputTokens,
    Long outputTokens,
    @Size(max = 50) List<@Valid AgentTaskAccessedUrlRequest> accessedUrls) {}
