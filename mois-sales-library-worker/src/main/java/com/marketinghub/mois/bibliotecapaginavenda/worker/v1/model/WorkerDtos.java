package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public final class WorkerDtos {
    private WorkerDtos() {}
    public record ClaimRequest(String workspaceId, String source) {}
    public record ClaimedJob(Long jobId, Long pageId, String urlCanonical, String title) {}
    public record ClaimResponse(boolean claimed, ClaimedJob job) {}
    public record CompleteRequest(BigDecimal scoreTotal, String sectionsJson, String copyJson, String visualJson, String imageJson, String analysisNotes, String parserVersion, String promptVersion, String modelName, Instant analyzedAt) {}
    public record FailRequest(String errorCategory, String errorMessage) {}
}
