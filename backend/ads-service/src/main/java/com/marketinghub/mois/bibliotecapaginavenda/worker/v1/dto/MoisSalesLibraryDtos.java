package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Agrupa contratos HTTP da Biblioteca de Páginas de Vendas do MOIS.
 */
public final class MoisSalesLibraryDtos {


    public record SalesLibraryClaimRequest(
            @NotBlank String workspaceId,
            @NotBlank String source
    ) {
    }

    public record SalesLibraryClaimedJob(
            long jobId,
            long pageId,
            String urlCanonical,
            String title,
            String rawHtml
    ) {
    }

    public record SalesLibraryClaimResponse(
            boolean claimed,
            SalesLibraryClaimedJob job
    ) {
    }

    public record SalesLibraryCompleteRequest(
            BigDecimal scoreTotal,
            String sectionsJson,
            String copyJson,
            String visualJson,
            String imageJson,
            String analysisNotes,
            String requestPayloadJson,
            String parserVersion,
            String promptVersion,
            String modelName,
            Instant analyzedAt
    ) {
    }

    public record SalesLibraryFailRequest(
            String errorCategory,
            String errorMessage
    ) {
    }


    /**
     * Impede instanciação do agrupador de contratos.
     */
    private MoisSalesLibraryDtos() {
    }


    public record CollectedReferenceHtmlClaimRequest(
            @NotBlank String workspaceId,
            @NotBlank String source
    ) {
    }

    public record CollectedReferenceHtmlCaptureJob(
            long captureId,
            long collectedReferenceId,
            String collectionJobId,
            String referenceId,
            String source,
            String title,
            String url,
            String urlSource
    ) {
    }

    public record CollectedReferenceHtmlClaimResponse(
            boolean claimed,
            CollectedReferenceHtmlCaptureJob job
    ) {
    }

    public record CollectedReferenceHtmlCompleteRequest(
            @NotBlank String rawHtml,
            String finalUrl,
            Integer httpStatus,
            String contentType,
            Instant fetchedAt
    ) {
    }

    public record CollectedReferenceHtmlFailRequest(
            String errorCategory,
            String errorMessage
    ) {
    }

    public record CollectedReferenceHtmlPersistResponse(
            long captureId,
            String status
    ) {
    }

    public record SalesLibraryIngestRequest(
            @NotBlank String workspaceId,
            @NotBlank String source,
            @NotEmpty List<@Valid SalesLibraryUrlItem> urls
    ) {
    }

    public record SalesLibraryUrlItem(
            @NotBlank String url,
            String title,
            Instant capturedAt
    ) {
    }

    public record SalesLibraryIngestResponse(
            String workspaceId,
            String source,
            int received,
            int persisted
    ) {
    }

    public record SalesLibraryHotmartCollectedIngestRequest(
            @NotBlank String workspaceId,
            String jobId,
            Integer limit
    ) {
    }

    public record SalesLibraryHotmartCollectedIngestResponse(
            String workspaceId,
            String jobId,
            int collectedReferencesRead,
            int eligibleUrls,
            int insertedUrls,
            int updatedUrls,
            int jobsCreated,
            int skippedWithoutUrl
    ) {
    }

    public record SalesLibraryJobResponse(
            long id,
            long urlIngestId,
            String status,
            int attempts,
            String errorCategory,
            String errorMessage,
            Instant nextRetryAt,
            Instant createdAt,
            Instant updatedAt,
            Instant startedAt,
            Instant finishedAt
    ) {
    }

    public record SalesLibraryJobPageResponse(
            int page,
            int pageSize,
            long total,
            List<SalesLibraryJobResponse> items
    ) {
    }

    public record SalesLibraryEntryResponse(
            long id,
            String workspaceId,
            String source,
            String urlOriginal,
            String urlCanonical,
            String title,
            int ingestCount,
            Instant firstCapturedAt,
            Instant lastCapturedAt,
            Instant updatedAt
    ) {
    }

    public record SalesLibraryEntryPageResponse(
            int page,
            int pageSize,
            long total,
            List<SalesLibraryEntryResponse> items
    ) {
    }

    /**
     * Representa o estado atual consolidado de uma página de venda no modelo novo.
     */
    public record SalesLibraryPageResponse(
            long pageId,
            String workspaceId,
            String source,
            String urlCanonical,
            String title,
            String currentStage,
            String currentStatus,
            String captureStatus,
            String analysisStatus,
            String urlFinal,
            Integer httpStatus,
            String htmlSha256,
            long htmlBytes,
            BigDecimal scoreTotal,
            String offerSummary,
            String mechanismSummary,
            String promiseSummary,
            String proofSummary,
            String lastErrorCategory,
            String lastErrorMessage,
            Long lastJobExecutionId,
            Instant lastCapturedAt,
            Instant analyzedAt,
            Instant updatedAt,
            BigDecimal marketWarmupScoreTotal,
            MarketWarmupTemperature marketWarmupTemperature,
            MarketWarmupEcosystemType marketWarmupEcosystemType,
            MarketWarmupRecommendation marketWarmupRecommendation,
            MarketWarmupJobStatus marketWarmupStatus,
            Instant marketWarmupUpdatedAt
    ) {
    }

    /**
     * Representa os contadores globais consolidados da Biblioteca de Páginas de Vendas.
     */
    public record SalesLibraryPageSummaryResponse(
            String workspaceId,
            long total,
            long pending,
            long capturing,
            long captured,
            long analyzed,
            long analysisPending,
            long analysisRunning,
            long analysisFailed,
            long failed,
            long blockedCooldown,
            long hotmart,
            long clickbank,
            long marketWarmupEligible,
            long marketWarmupPending,
            long marketWarmupRunning,
            long marketWarmupCompleted,
            long marketWarmupFailed,
            long marketWarmupHot,
            long marketWarmupPromising,
            long marketWarmupWarm,
            long marketWarmupCold,
            long marketWarmupSaturated,
            long marketWarmupStuck,
            Instant updatedAt
    ) {
    }


    /**
     * Representa a cobertura de URLs únicas vindas da origem bruta mois_collected_reference.
     */
    public record CollectedReferenceUrlSummaryResponse(
            String workspaceId,
            long uniqueEffectiveUrls,
            long explicitSalesPageUrls,
            long fallbackProductUrls,
            long operationalLibraryUrls,
            long missingFromOperationalLibrary,
            List<CollectedReferenceUrlSourceBreakdown> bySource,
            List<CollectedReferenceUrlTypeBreakdown> byUrlType
    ) {
    }

    /**
     * Representa o desdobramento de URLs únicas coletadas por marketplace/origem.
     */
    public record CollectedReferenceUrlSourceBreakdown(
            String source,
            long uniqueEffectiveUrls,
            long operationalLibraryUrls,
            long missingFromOperationalLibrary
    ) {
    }

    /**
     * Representa o desdobramento de URLs únicas coletadas por tipo de URL usada.
     */
    public record CollectedReferenceUrlTypeBreakdown(
            String urlType,
            long uniqueUrls
    ) {
    }

    /**
     * Representa uma execução auditável da página no histórico consolidado.
     */
    public record SalesLibraryPageExecutionResponse(
            long executionId,
            long pageId,
            String jobType,
            String stage,
            String status,
            int attempt,
            String inputUrl,
            String finalUrl,
            String redirectRootUrl,
            Integer httpStatus,
            String contentType,
            long rawHtmlBytes,
            long screenshotBytes,
            BigDecimal scoreTotal,
            String errorCategory,
            String errorMessage,
            Instant startedAt,
            Instant finishedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record SalesLibraryPageListResponse(
            int page,
            int pageSize,
            long total,
            List<SalesLibraryPageResponse> items
    ) {
    }

    public record SalesLibraryPageAnalysisResponse(
            long analysisId,
            long pageId,
            Long jobId,
            String status,
            BigDecimal scoreTotal,
            String parserVersion,
            String promptVersion,
            String modelName,
            String sectionsJson,
            String copyJson,
            String visualJson,
            String imageJson,
            String analysisNotes,
            String requestPayloadJson,
            Instant analyzedAt,
            Instant updatedAt
    ) {
    }

    public record SalesLibraryReanalyzeResponse(
            long pageId,
            long jobId,
            String status,
            Instant createdAt
    ) {
    }


    public record SalesLibraryStatusUpdateRequest(
            @NotBlank String status,
            String reason
    ) {
    }

    public record SalesLibraryStatusUpdateResponse(
            long pageId,
            Long jobId,
            String status,
            String reason,
            Instant createdAt
    ) {
    }

    public record SalesLibrarySnapshotCaptureRequest(
            @NotBlank String workspaceId,
            Integer limit,
            Boolean force
    ) {
    }

    public record SalesLibrarySnapshotCaptureItem(
            long pageId,
            Long snapshotId,
            String urlCanonical,
            String redirectDestinationUrl,
            String redirectRootUrl,
            String status,
            String snapshotHash,
            Integer httpStatus,
            long rawHtmlBytes,
            long screenshotBytes,
            String errorMessage
    ) {
    }

    public record SalesLibrarySnapshotCaptureResponse(
            String workspaceId,
            int requestedLimit,
            boolean force,
            int processed,
            int captured,
            int failed,
            List<SalesLibrarySnapshotCaptureItem> items,
            Instant capturedAt
    ) {
    }

    public record SalesLibraryPageSnapshotResponse(
            long snapshotId,
            long pageId,
            String snapshotHash,
            String status,
            Integer httpStatus,
            String contentType,
            String redirectDestinationUrl,
            String redirectRootUrl,
            long rawHtmlBytes,
            long screenshotBytes,
            Instant capturedAt,
            Instant updatedAt
    ) {
    }

    public record HtmlCaptureClaimRequest(
            @NotBlank String workspaceId,
            Integer limit,
            Boolean force
    ) {
    }

    public record HtmlCaptureJobResponse(
            long snapshotId,
            long pageId,
            String urlCanonical,
            String title
    ) {
    }

    public record HtmlCaptureClaimResponse(
            boolean claimed,
            HtmlCaptureJobResponse job
    ) {
    }

    public record HtmlCaptureCompleteRequest(
            @NotBlank String rawHtml,
            String finalUrl,
            String redirectDestinationUrl,
            String redirectRootUrl,
            Integer httpStatus,
            String contentType,
            String sha256,
            Long sizeBytes,
            Instant capturedAt
    ) {
    }

    public record HtmlCaptureFailRequest(
            String errorCategory,
            String errorMessage,
            String redirectDestinationUrl,
            String redirectRootUrl,
            Integer httpStatus
    ) {
    }

    public record HtmlCapturePersistResponse(
            long snapshotId,
            String status
    ) {
    }


    /**
     * Define os estados operacionais possíveis de uma pesquisa de aquecimento de mercado.
     */
    public enum MarketWarmupJobStatus {
        PENDING,
        FETCHING,
        DONE,
        FAILED
    }

    /**
     * Define a temperatura comercial calculada para o mercado analisado.
     */
    public enum MarketWarmupTemperature {
        HOT,
        PROMISING,
        WARM,
        COLD,
        SATURATED
    }

    /**
     * Define o tipo principal de ecossistema encontrado na pesquisa pública.
     */
    public enum MarketWarmupEcosystemType {
        SPECIALISTS_HEATED,
        CREATORS_HEATED,
        RECURRING_PAIN_HEATED,
        COMPETITORS_HEATED,
        COLD_OR_UNEDUCATED,
        SATURATED
    }

    /**
     * Define a recomendação comercial objetiva derivada do score e dos sinais.
     */
    public enum MarketWarmupRecommendation {
        PRIORITIZE,
        OBSERVE,
        RESEARCH_MORE,
        DISCARD,
        SATURATED_REQUIRES_ANGLE
    }

    /**
     * Define as plataformas públicas aceitas como origem rastreável de evidência.
     */
    public enum MarketWarmupPlatform {
        WEB,
        GOOGLE,
        YOUTUBE,
        INSTAGRAM,
        TIKTOK,
        BLOG,
        FORUM,
        COMMUNITY,
        MARKETPLACE,
        REVIEW_SITE,
        OTHER
    }

    /**
     * Define o tipo funcional da fonte pública encontrada.
     */
    public enum MarketWarmupSourceType {
        PRODUCT_PRESENCE,
        CREATOR_CONTENT,
        SPECIALIST_CONTENT,
        COMMUNITY_DISCUSSION,
        REVIEW,
        COMPLAINT,
        COMPETITOR_OFFER,
        AFFILIATE_PROMOTION,
        SOCIAL_POST,
        SEARCH_RESULT,
        OTHER
    }

    /**
     * Define os sinais comerciais extraídos das fontes públicas.
     */
    public enum MarketWarmupSignalType {
        PAIN_EXPLICIT,
        BUYING_INTENT,
        OBJECTION,
        SOCIAL_PROOF,
        CREATOR_AUTHORITY,
        COMPETITOR_OFFER,
        COMMUNITY_ACTIVITY,
        CONTENT_RECENCY,
        SATURATION_RISK,
        CHANNEL_FIT
    }


    /**
     * Representa uma página priorizada por score comercial combinado da Etapa 3.
     */
    public record MarketWarmupOpportunityRankingItem(
            long pageId,
            String title,
            String urlCanonical,
            String source,
            BigDecimal pageScoreTotal,
            BigDecimal warmupScoreTotal,
            BigDecimal combinedCommercialScore,
            MarketWarmupTemperature marketTemperature,
            MarketWarmupEcosystemType ecosystemType,
            MarketWarmupRecommendation recommendation,
            String saturationRisk,
            Instant evidenceUpdatedAt,
            String suggestedNextAction,
            String evidenceSummary
    ) {
    }

    /**
     * Representa o ranking comercial de oportunidades para escolher próximo experimento ou pesquisa.
     */
    public record MarketWarmupOpportunityRankingResponse(
            String workspaceId,
            int limit,
            List<MarketWarmupOpportunityRankingItem> items
    ) {
    }

    /**
     * Representa a solicitação aceita para criar ou reutilizar uma pesquisa de aquecimento da página.
     */
    public record MarketWarmupRequestResponse(
            long pageId,
            long jobId,
            MarketWarmupJobStatus status,
            Instant createdAt
    ) {
    }

    /**
     * Representa o resumo final rastreável da pesquisa de aquecimento de mercado.
     */
    public record MarketWarmupSummaryResponse(
            long jobId,
            long pageId,
            BigDecimal scoreTotal,
            MarketWarmupTemperature marketTemperature,
            MarketWarmupEcosystemType ecosystemType,
            MarketWarmupRecommendation recommendation,
            List<String> mainPains,
            List<String> mainObjections,
            List<String> mainPromises,
            List<String> mainChannels,
            List<String> mainCompetitors,
            String saturationRisk,
            String opportunityRecommendation,
            String nextExperimentSuggestion,
            MarketWarmupJobStatus status,
            String errorCategory,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    /**
     * Representa uma fonte pública usada para justificar o aquecimento do mercado.
     */
    public record MarketWarmupSourceResponse(
            long sourceId,
            long jobId,
            long pageId,
            MarketWarmupPlatform platform,
            MarketWarmupSourceType sourceType,
            String sourceUrl,
            String sourceTitle,
            String authorName,
            Instant publishedAt,
            Instant lastActivityAt,
            Long followersOrSubscribers,
            Long viewsCount,
            Long likesCount,
            Long commentsCount,
            BigDecimal recencyScore,
            BigDecimal engagementScore,
            String evidenceSummary,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    /**
     * Representa a página de fontes públicas retornada para revisão humana.
     */
    public record MarketWarmupSourceListResponse(
            long pageId,
            long jobId,
            List<MarketWarmupSourceResponse> items
    ) {
    }

    /**
     * Representa um sinal comercial extraído de uma fonte pública.
     */
    public record MarketWarmupSignalResponse(
            long signalId,
            long jobId,
            long sourceId,
            long pageId,
            MarketWarmupSignalType signalType,
            BigDecimal signalStrength,
            String signalText,
            String businessInterpretation,
            Instant createdAt
    ) {
    }

    /**
     * Representa a lista de sinais que explicam a pontuação da pesquisa.
     */
    public record MarketWarmupSignalListResponse(
            long pageId,
            long jobId,
            List<MarketWarmupSignalResponse> items
    ) {
    }

    /**
     * Representa o pedido interno do worker para reservar uma pesquisa pendente.
     */
    public record MarketWarmupClaimRequest(
            @NotBlank String workspaceId,
            @NotBlank String workerId
    ) {
    }

    /**
     * Representa uma pesquisa reservada com os dados comerciais necessários para coleta pública.
     */
    public record MarketWarmupClaimedJob(
            long jobId,
            long pageId,
            String workspaceId,
            String urlCanonical,
            String title,
            String producerName,
            String offerSummary,
            String mechanismSummary,
            String promiseSummary,
            String proofSummary
    ) {
    }

    /**
     * Representa a resposta da reserva interna de pesquisa de aquecimento.
     */
    public record MarketWarmupClaimResponse(
            boolean claimed,
            MarketWarmupClaimedJob job
    ) {
    }

    /**
     * Representa a solicitação operacional para reprocessar jobs de aquecimento presos em execução antiga.
     */
    public record MarketWarmupReprocessStaleRequest(
            @NotBlank String workspaceId,
            @Positive Integer staleMinutes
    ) {
    }

    /**
     * Representa o resultado da ação de saneamento que refileira jobs presos sem acesso direto ao banco.
     */
    public record MarketWarmupReprocessStaleResponse(
            String workspaceId,
            int staleMinutes,
            long requeuedJobs,
            Instant updatedAt
    ) {
    }

    /**
     * Representa uma fonte pública enviada pelo worker ao concluir a pesquisa.
     */
    public record MarketWarmupSourceCompleteItem(
            @NotNull MarketWarmupPlatform platform,
            @NotNull MarketWarmupSourceType sourceType,
            @NotBlank String sourceUrl,
            String sourceTitle,
            String authorName,
            Instant publishedAt,
            Instant lastActivityAt,
            Long followersOrSubscribers,
            Long viewsCount,
            Long likesCount,
            Long commentsCount,
            BigDecimal recencyScore,
            BigDecimal engagementScore,
            String evidenceSummary
    ) {
    }

    /**
     * Representa um sinal extraído pelo worker e vinculado a uma fonte pelo índice enviado.
     */
    public record MarketWarmupSignalCompleteItem(
            int sourceIndex,
            @NotNull MarketWarmupSignalType signalType,
            @NotNull BigDecimal signalStrength,
            @NotBlank String signalText,
            String businessInterpretation
    ) {
    }

    /**
     * Representa o resumo calculado pelo worker para persistência final da pesquisa.
     */
    public record MarketWarmupSummaryCompleteItem(
            @NotNull BigDecimal scoreTotal,
            @NotNull MarketWarmupTemperature marketTemperature,
            @NotNull MarketWarmupEcosystemType ecosystemType,
            @NotNull MarketWarmupRecommendation recommendation,
            List<String> mainPains,
            List<String> mainObjections,
            List<String> mainPromises,
            List<String> mainChannels,
            List<String> mainCompetitors,
            String saturationRisk,
            String opportunityRecommendation,
            String nextExperimentSuggestion
    ) {
    }

    /**
     * Representa o payload final do worker sem JSON serializado em campos textuais funcionais.
     */
    public record MarketWarmupCompleteRequest(
            @NotEmpty List<@Valid MarketWarmupSourceCompleteItem> sources,
            @NotEmpty List<@Valid MarketWarmupSignalCompleteItem> signals,
            @NotNull @Valid MarketWarmupSummaryCompleteItem summary,
            Instant finishedAt
    ) {
    }

    /**
     * Representa a falha operacional de uma pesquisa de aquecimento pelo worker.
     */
    public record MarketWarmupFailRequest(
            @NotBlank String errorCategory,
            @NotBlank String errorMessage
    ) {
    }

}
