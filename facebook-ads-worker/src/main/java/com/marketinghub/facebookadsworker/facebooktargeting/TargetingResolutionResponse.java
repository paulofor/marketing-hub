package com.marketinghub.facebookadsworker.facebooktargeting;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Resposta com o resumo do processamento realizado pelo worker.
 */
public record TargetingResolutionResponse(
    @JsonProperty("request_id") UUID requestId,
    @JsonProperty("candidates") List<CandidateResolutionSummary> candidates
) {
    public record CandidateResolutionSummary(
        @JsonProperty("id") Long id,
        @JsonProperty("status") TargetingCandidateStatus status,
        @JsonProperty("resolved_options") int resolvedOptions,
        @JsonProperty("message") String message
    ) {}
}
