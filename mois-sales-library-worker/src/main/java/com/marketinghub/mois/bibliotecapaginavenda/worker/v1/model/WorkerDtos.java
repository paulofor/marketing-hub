package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Agrupa contratos usados pelo worker MOIS para conversar com o backend principal.
 */
public final class WorkerDtos {
    /**
     * Impede instanciação do agrupador de DTOs.
     */
    private WorkerDtos() {}

    public record ClaimRequest(String workspaceId, String source) {}
    public record ClaimedJob(Long jobId, Long pageId, String urlCanonical, String title) {}
    public record ClaimResponse(boolean claimed, ClaimedJob job) {}
    public record CompleteRequest(BigDecimal scoreTotal, String sectionsJson, String copyJson, String visualJson, String imageJson, String analysisNotes, String requestPayloadJson, String parserVersion, String promptVersion, String modelName, Instant analyzedAt) {}
    public record FailRequest(String errorCategory, String errorMessage) {}

    public record CollectedReferenceHtmlClaimRequest(String workspaceId, String source) {}
    public record CollectedReferenceHtmlCaptureJob(Long captureId, Long collectedReferenceId, String collectionJobId, String referenceId, String source, String title, String url, String urlSource) {}
    public record CollectedReferenceHtmlClaimResponse(boolean claimed, CollectedReferenceHtmlCaptureJob job) {}
    public record CollectedReferenceHtmlCompleteRequest(String rawHtml, String finalUrl, Integer httpStatus, String contentType, Instant fetchedAt) {}
    public record CollectedReferenceHtmlFailRequest(String errorCategory, String errorMessage) {}
    public record CollectedReferenceHtmlPersistResponse(Long captureId, String status) {}

    public record HtmlCaptureClaimRequest(String workspaceId, Integer limit, Boolean force) {}
    public record HtmlCaptureJob(Long snapshotId, Long pageId, String urlCanonical, String title) {}
    public record HtmlCaptureClaimResponse(boolean claimed, HtmlCaptureJob job) {}
    public record HtmlCaptureCompleteRequest(String rawHtml, String finalUrl, Integer httpStatus, String contentType, String sha256, Long sizeBytes, Instant capturedAt) {}
    public record HtmlCaptureFailRequest(String errorCategory, String errorMessage) {}
    public record HtmlCapturePersistResponse(Long snapshotId, String status) {}
}
