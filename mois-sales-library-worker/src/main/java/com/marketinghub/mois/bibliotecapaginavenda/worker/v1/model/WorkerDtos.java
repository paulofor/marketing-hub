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
    public record ClaimedJob(Long jobId, Long pageId, String urlCanonical, String title, String rawHtml) {}
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
    public record HtmlCaptureCompleteRequest(String rawHtml, String finalUrl, String redirectDestinationUrl, String redirectRootUrl, Integer httpStatus, String contentType, String sha256, Long sizeBytes, Instant capturedAt) {}
    public record HtmlCaptureFailRequest(String errorCategory, String errorMessage, String redirectDestinationUrl, String redirectRootUrl, Integer httpStatus) {}
    public record HtmlCapturePersistResponse(Long snapshotId, String status) {}
    public enum MarketWarmupPlatform { WEB, GOOGLE, YOUTUBE, INSTAGRAM, TIKTOK, BLOG, FORUM, COMMUNITY, MARKETPLACE, REVIEW_SITE, OTHER }
    public enum MarketWarmupSourceType { PRODUCT_PRESENCE, CREATOR_CONTENT, SPECIALIST_CONTENT, COMMUNITY_DISCUSSION, REVIEW, COMPLAINT, COMPETITOR_OFFER, AFFILIATE_PROMOTION, SOCIAL_POST, SEARCH_RESULT, OTHER }
    public enum MarketWarmupSignalType { PAIN_EXPLICIT, BUYING_INTENT, OBJECTION, SOCIAL_PROOF, CREATOR_AUTHORITY, COMPETITOR_OFFER, COMMUNITY_ACTIVITY, CONTENT_RECENCY, SATURATION_RISK, CHANNEL_FIT }
    public enum MarketWarmupTemperature { HOT, PROMISING, WARM, COLD, SATURATED }
    public enum MarketWarmupEcosystemType { SPECIALISTS_HEATED, CREATORS_HEATED, RECURRING_PAIN_HEATED, COMPETITORS_HEATED, COLD_OR_UNEDUCATED, SATURATED }
    public enum MarketWarmupRecommendation { PRIORITIZE, OBSERVE, RESEARCH_MORE, DISCARD, SATURATED_REQUIRES_ANGLE }

    public record MarketWarmupClaimRequest(String workspaceId, String workerId) {}
    public record MarketWarmupClaimedJob(Long jobId, Long pageId, String workspaceId, String urlCanonical, String title, String offerSummary, String mechanismSummary, String promiseSummary, String proofSummary) {}
    public record MarketWarmupClaimResponse(boolean claimed, MarketWarmupClaimedJob job) {}
    public record MarketWarmupSourceCompleteItem(MarketWarmupPlatform platform, MarketWarmupSourceType sourceType, String sourceUrl, String sourceTitle, String authorName, Instant publishedAt, Instant lastActivityAt, Long followersOrSubscribers, Long viewsCount, Long likesCount, Long commentsCount, BigDecimal recencyScore, BigDecimal engagementScore, String evidenceSummary) {}
    public record MarketWarmupSignalCompleteItem(int sourceIndex, MarketWarmupSignalType signalType, BigDecimal signalStrength, String signalText, String businessInterpretation) {}
    public record MarketWarmupSummaryCompleteItem(BigDecimal scoreTotal, MarketWarmupTemperature marketTemperature, MarketWarmupEcosystemType ecosystemType, MarketWarmupRecommendation recommendation, java.util.List<String> mainPains, java.util.List<String> mainObjections, java.util.List<String> mainPromises, java.util.List<String> mainChannels, java.util.List<String> mainCompetitors, String saturationRisk, String opportunityRecommendation, String nextExperimentSuggestion) {}
    public record MarketWarmupCompleteRequest(java.util.List<MarketWarmupSourceCompleteItem> sources, java.util.List<MarketWarmupSignalCompleteItem> signals, MarketWarmupSummaryCompleteItem summary, Instant finishedAt) {}
    public record MarketWarmupFailRequest(String errorCategory, String errorMessage) {}
}
