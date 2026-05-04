package com.marketinghub.mois.service;

import com.marketinghub.mois.domain.MoisDomainModels.DiscoveryRequest;
import com.marketinghub.mois.domain.MoisDomainModels.DiscoveryStatus;
import com.marketinghub.mois.domain.MoisDomainModels.OfferCard;
import com.marketinghub.mois.domain.MoisDomainModels.SourceSnapshot;
import com.marketinghub.mois.dto.MoisArtifactDtos;
import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.dto.MoisInsightDtos;
import com.marketinghub.mois.dto.MoisOfferDtos;
import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MoisDomainService {

    private static final Logger log = LoggerFactory.getLogger(MoisDomainService.class);
    private static final Pattern PRICE_PATTERN = Pattern.compile("(R\\$\\s?\\d{2,5}(?:[\\.,]\\d{2})?|\\$\\s?\\d{2,5}(?:[\\.,]\\d{2})?)");
    private static final int DEFAULT_LIMIT_PER_SOURCE = 50;
    private static final int DEFAULT_MIN_SUCCESS_SCORE = 50;
    private static final double ENGAGEMENT_WEIGHT = 0.45;
    private static final double RECURRENCE_WEIGHT = 0.35;
    private static final double EVIDENCE_WEIGHT = 0.20;
    private static final int MAX_COLLECTION_RETRIES = 2;

    private final Map<String, DiscoveryRequest> requests = new ConcurrentHashMap<>();
    private final Map<String, SourceSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<String, OfferCard> offers = new ConcurrentHashMap<>();
    private final Map<String, MoisInsightDtos.InsightReportResponse> reports = new ConcurrentHashMap<>();
    private final Map<String, String> runOperations = new ConcurrentHashMap<>();
    private final Map<String, MoisWorkspaceDtos.CollectionJobResponse> collectionJobs = new ConcurrentHashMap<>();
    private final Map<String, List<MoisWorkspaceDtos.CollectedReferenceResponse>> collectedReferencesByJob = new ConcurrentHashMap<>();
    private final Map<String, MoisWorkspaceDtos.CollectedReferenceLineageResponse> collectedReferenceLineage = new ConcurrentHashMap<>();
    private final Map<String, CollectionJobRuntimeStats> collectionJobRuntimeStats = new ConcurrentHashMap<>();
    private final Map<String, Map<String, SourceOpsStats>> collectionOpsByWorkspace = new ConcurrentHashMap<>();
    private final Map<String, MoisWorkspaceDtos.ReferenceResponse> referencesById = new ConcurrentHashMap<>();
    private final Map<String, MoisWorkspaceDtos.ExtractionDraftResponse> extractionByReferenceId = new ConcurrentHashMap<>();
    private final Map<String, MoisWorkspaceDtos.LibraryBlockResponse> libraryBlocksById = new ConcurrentHashMap<>();
    private final Map<String, MoisWorkspaceDtos.ComparisonResponse> comparisonsById = new ConcurrentHashMap<>();
    private final Map<String, MoisWorkspaceDtos.BuildOfferResponse> offersById = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final String MODULE_NAME = "MOIS";
    private static final String CREATED_BY = "mois-system";
    private static final String DEFAULT_STAGE = "COLETA";
    private volatile boolean collectionRolloutEnabled = true;
    private final Set<String> rolloutAllowedWorkspaces = ConcurrentHashMap.newKeySet();

    public MoisDomainService() {
        loadCollectionRolloutFromEnv();
    }

    public MoisWorkspaceDtos.WorkspaceDashboardResponse getDashboard(String workspaceId) {
        List<MoisWorkspaceDtos.ReferenceResponse> workspaceReferences = listWorkspaceReferences(workspaceId);
        int extractions = (int) workspaceReferences.stream()
                .filter(reference -> extractionByReferenceId.containsKey(reference.referenceId()))
                .count();

        List<MoisWorkspaceDtos.RecentAnalysisResponse> recentAnalyses = workspaceReferences.stream()
                .sorted(Comparator.comparing(MoisWorkspaceDtos.ReferenceResponse::createdAt).reversed())
                .limit(10)
                .map(reference -> new MoisWorkspaceDtos.RecentAnalysisResponse(
                        reference.referenceId(),
                        reference.niche(),
                        extractionByReferenceId.containsKey(reference.referenceId()) ? "EXTRACTION_DRAFT" : "COLETA_CONCLUIDA",
                        reference.createdAt()))
                .toList();

        return new MoisWorkspaceDtos.WorkspaceDashboardResponse(
                workspaceId,
                new MoisWorkspaceDtos.WorkspaceKpisResponse(workspaceReferences.size(), extractions, 0, 0),
                DEFAULT_STAGE,
                recentAnalyses
        );
    }

    public MoisWorkspaceDtos.ReferenceResponse createReference(MoisWorkspaceDtos.CreateReferenceRequest request) {
        Instant now = Instant.now();
        MoisWorkspaceDtos.ReferenceResponse created = new MoisWorkspaceDtos.ReferenceResponse(
                UUID.randomUUID().toString(),
                request.workspaceId(),
                request.niche(),
                request.sourceUrl(),
                request.assetType(),
                request.primaryPromise(),
                request.awarenessStage(),
                request.priceRange(),
                request.formatType(),
                request.notes(),
                now
        );
        referencesById.put(created.referenceId(), created);
        return created;
    }

    public MoisWorkspaceDtos.ReferenceListResponse listReferences(String workspaceId) {
        List<MoisWorkspaceDtos.ReferenceResponse> references = listWorkspaceReferences(workspaceId).stream()
                .sorted(Comparator.comparing(MoisWorkspaceDtos.ReferenceResponse::createdAt).reversed())
                .toList();
        return new MoisWorkspaceDtos.ReferenceListResponse(references);
    }

    public MoisWorkspaceDtos.ExtractionDraftResponse upsertExtractionDraft(
            String referenceId,
            MoisWorkspaceDtos.UpsertExtractionDraftRequest request
    ) {
        MoisWorkspaceDtos.ReferenceResponse reference = referencesById.get(referenceId);
        if (reference == null) {
            throw new IllegalArgumentException("reference not found");
        }

        MoisWorkspaceDtos.ExtractionDraftResponse draft = new MoisWorkspaceDtos.ExtractionDraftResponse(
                UUID.randomUUID().toString(),
                referenceId,
                hasAnyDraftContent(request) ? "DRAFT" : "EMPTY_DRAFT",
                Instant.now()
        );
        extractionByReferenceId.put(referenceId, draft);
        return draft;
    }

    public MoisWorkspaceDtos.LibraryBlockListResponse listLibraryBlocks(String workspaceId, String niche, String formatType) {
        seedLibraryIfNeeded(workspaceId);
        List<MoisWorkspaceDtos.LibraryBlockResponse> blocks = new ArrayList<>();
        for (MoisWorkspaceDtos.LibraryBlockResponse block : libraryBlocksById.values()) {
            boolean sameWorkspace = workspaceId == null || workspaceId.isBlank() || workspaceId.equals(block.workspaceId());
            boolean matchesNiche = niche == null || niche.isBlank() || block.tags().contains(niche);
            boolean matchesFormat = formatType == null || formatType.isBlank() || block.tags().contains(formatType);
            if (sameWorkspace && matchesNiche && matchesFormat) {
                blocks.add(block);
            }
        }

        blocks.sort(Comparator.comparing(MoisWorkspaceDtos.LibraryBlockResponse::updatedAt).reversed());
        return new MoisWorkspaceDtos.LibraryBlockListResponse(blocks);
    }

    public MoisWorkspaceDtos.LibraryBlockActionResponse favoriteLibraryBlock(String blockId) {
        MoisWorkspaceDtos.LibraryBlockResponse block = getLibraryBlockOrThrow(blockId);
        MoisWorkspaceDtos.LibraryBlockResponse updated = new MoisWorkspaceDtos.LibraryBlockResponse(
                block.blockId(),
                block.workspaceId(),
                block.type(),
                block.summary(),
                block.tags(),
                block.score(),
                block.origin(),
                true,
                Instant.now()
        );
        libraryBlocksById.put(blockId, updated);
        return new MoisWorkspaceDtos.LibraryBlockActionResponse(blockId, "FAVORITE", "OK", updated.updatedAt());
    }

    public MoisWorkspaceDtos.LibraryBlockActionResponse duplicateLibraryBlock(String blockId) {
        MoisWorkspaceDtos.LibraryBlockResponse source = getLibraryBlockOrThrow(blockId);
        String duplicateId = UUID.randomUUID().toString();
        MoisWorkspaceDtos.LibraryBlockResponse duplicate = new MoisWorkspaceDtos.LibraryBlockResponse(
                duplicateId,
                source.workspaceId(),
                source.type(),
                source.summary() + " (cópia)",
                source.tags(),
                source.score(),
                "DUPLICATED_FROM_" + source.blockId(),
                false,
                Instant.now()
        );
        libraryBlocksById.put(duplicateId, duplicate);
        return new MoisWorkspaceDtos.LibraryBlockActionResponse(duplicateId, "DUPLICATE", "OK", duplicate.updatedAt());
    }

    public MoisWorkspaceDtos.ComparisonResponse createComparison(MoisWorkspaceDtos.CreateComparisonRequest request) {
        String comparisonId = UUID.randomUUID().toString();
        List<MoisWorkspaceDtos.ComparisonDimensionResponse> dimensions = List.of(
                new MoisWorkspaceDtos.ComparisonDimensionResponse(
                        "PROMESSA", "Resultado em 8 semanas", "Resultado sem prazo", "Adicionar prazo específico"),
                new MoisWorkspaceDtos.ComparisonDimensionResponse(
                        "MECANISMO", "Protocolo em 3 etapas", "Método genérico", "Explicitar etapas"),
                new MoisWorkspaceDtos.ComparisonDimensionResponse(
                        "PROVA", "Depoimentos com números", "Sem dados concretos", "Anexar provas mensuráveis"),
                new MoisWorkspaceDtos.ComparisonDimensionResponse(
                        "LAYOUT", "Fluxo com CTA principal", "Múltiplos CTAs", "Reduzir atrito visual")
        );
        List<MoisWorkspaceDtos.ComparisonScorecardResponse> scorecards = List.of(
                new MoisWorkspaceDtos.ComparisonScorecardResponse("clareza", 72, "Promessa compreensível, porém ampla."),
                new MoisWorkspaceDtos.ComparisonScorecardResponse("prova", 46, "Faltam evidências verificáveis."),
                new MoisWorkspaceDtos.ComparisonScorecardResponse("coerencia", 69, "Narrativa parcialmente alinhada ao mecanismo."),
                new MoisWorkspaceDtos.ComparisonScorecardResponse("atrito", 58, "Existem etapas redundantes no fluxo.")
        );
        List<MoisWorkspaceDtos.ComparisonImprovementResponse> improvements = List.of(
                new MoisWorkspaceDtos.ComparisonImprovementResponse(UUID.randomUUID().toString(), "HIGH", "Adicionar seção de prova com antes/depois."),
                new MoisWorkspaceDtos.ComparisonImprovementResponse(UUID.randomUUID().toString(), "MEDIUM", "Reescrever headline com prazo e público."),
                new MoisWorkspaceDtos.ComparisonImprovementResponse(UUID.randomUUID().toString(), "MEDIUM", "Consolidar CTAs em um único caminho.")
        );
        MoisWorkspaceDtos.ComparisonResponse comparison = new MoisWorkspaceDtos.ComparisonResponse(
                comparisonId,
                request.workspaceId(),
                dimensions,
                scorecards,
                improvements
        );
        comparisonsById.put(comparisonId, comparison);
        return comparison;
    }

    public MoisWorkspaceDtos.BuildOfferResponse buildOffer(MoisWorkspaceDtos.BuildOfferRequest request) {
        boolean hasPain = request.currentVersion().toLowerCase().contains("dor");
        boolean hasResult = request.currentVersion().toLowerCase().contains("resultado");
        boolean hasMechanism = request.currentVersion().toLowerCase().contains("mecanismo");
        boolean hasProof = request.currentVersion().toLowerCase().contains("prova");
        boolean hasOffer = request.currentVersion().toLowerCase().contains("oferta");

        Map<String, Boolean> checklist = Map.of(
                "dor", hasPain,
                "resultado", hasResult,
                "mecanismo", hasMechanism,
                "prova", hasProof,
                "oferta", hasOffer
        );
        String proposed = request.currentVersion()
                + "\n\n## Versão proposta\n- Blocos selecionados: "
                + (request.selectedBlockIds() == null ? 0 : request.selectedBlockIds().size())
                + "\n- Reforçar Dor → Resultado → Mecanismo → Prova → Oferta.";
        MoisWorkspaceDtos.BuildOfferResponse response = new MoisWorkspaceDtos.BuildOfferResponse(
                UUID.randomUUID().toString(),
                request.workspaceId(),
                checklist.containsValue(false) ? "INCOMPLETE_CHECKLIST" : "READY_TO_EXPORT",
                proposed,
                checklist,
                Instant.now()
        );
        offersById.put(response.offerId(), response);
        return response;
    }

    public MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse createDiscoveryRequest(
            MoisDiscoveryDtos.CreateDiscoveryRequest payload
    ) {
        Instant now = Instant.now();
        String requestId = "mois-req-" + UUID.randomUUID();

        DiscoveryRequest request = new DiscoveryRequest(
                requestId,
                payload.nicheName(),
                payload.marketTheme(),
                payload.painOrOutcomeFocus(),
                defaultList(payload.seedQueries()),
                defaultList(payload.seedUrls()),
                defaultList(payload.channels()),
                payload.country(),
                payload.language(),
                defaultMap(payload.discoveryPolicy()),
                DiscoveryStatus.DRAFT,
                now,
                now);
        requests.put(requestId, request);
        return new MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse(requestId, "ACCEPTED");
    }

    public MoisDiscoveryDtos.DiscoveryRequestListResponse listDiscoveryRequests(String status, String nicheName, String marketTheme) {
        List<MoisDiscoveryDtos.DiscoveryRequestSummaryResponse> items = requests.values().stream()
                .filter(item -> status == null || status.isBlank() || item.status().name().equalsIgnoreCase(status))
                .filter(item -> nicheName == null || nicheName.isBlank() || item.nicheName().equalsIgnoreCase(nicheName))
                .filter(item -> marketTheme == null || marketTheme.isBlank() || item.marketTheme().equalsIgnoreCase(marketTheme))
                .sorted(Comparator.comparing(DiscoveryRequest::createdAt).reversed())
                .map(item -> new MoisDiscoveryDtos.DiscoveryRequestSummaryResponse(
                        item.requestId(),
                        item.nicheName(),
                        item.marketTheme(),
                        item.painOrOutcomeFocus(),
                        item.status().name(),
                        item.createdAt()))
                .toList();
        return new MoisDiscoveryDtos.DiscoveryRequestListResponse(items);
    }

    public Optional<MoisDiscoveryDtos.DiscoveryRequestDetailResponse> getDiscoveryRequest(String requestId) {
        return Optional.ofNullable(requests.get(requestId))
                .map(request -> new MoisDiscoveryDtos.DiscoveryRequestDetailResponse(
                        request.requestId(),
                        request.nicheName(),
                        request.marketTheme(),
                        request.painOrOutcomeFocus(),
                        request.status().name(),
                        request.createdAt(),
                        collectArtifactsForRequest(requestId)));
    }

    public Optional<MoisDiscoveryDtos.AsyncAcceptedResponse> runDiscoveryRequest(String requestId) {
        DiscoveryRequest request = requests.get(requestId);
        if (request == null) {
            return Optional.empty();
        }
        if (runOperations.containsKey(requestId)) {
            return Optional.of(new MoisDiscoveryDtos.AsyncAcceptedResponse("ACCEPTED", runOperations.get(requestId)));
        }

        String correlationId = "mois-run-" + requestId;
        runOperations.put(requestId, correlationId);
        log.info("mois_run_started requestId={} correlationId={} nicheName={} marketTheme={}",
                requestId, correlationId, request.nicheName(), request.marketTheme());

        List<String> collectionSeeds = buildCollectionSeeds(request);
        Map<String, OfferCard> deduplicated = new LinkedHashMap<>();

        for (String seed : collectionSeeds) {
            Optional<CrawledSource> crawled = collectSource(seed, requestId, correlationId);
            if (crawled.isEmpty()) {
                continue;
            }

            CrawledSource source = crawled.get();
            String sourceId = "mois-source-" + UUID.randomUUID();
            SourceSnapshot snapshot = new SourceSnapshot(
                    sourceId,
                    requestId,
                    seed,
                    source.canonicalUrl(),
                    source.title(),
                    source.sourceKind(),
                    Instant.now(),
                    source.normalizedText());
            snapshots.put(sourceId, snapshot);

            OfferSignals signals = extractSignals(source.normalizedText(), request);
            String signature = contentSignature(source.normalizedText());
            String dedupeKey = source.canonicalUrl() + "|" + signature;

            if (deduplicated.containsKey(dedupeKey)) {
                log.info("mois_offer_deduplicated requestId={} correlationId={} canonicalUrl={} signature={} sourceArtifactId={}",
                        requestId, correlationId, source.canonicalUrl(), signature, sourceId);
                continue;
            }

            String offerId = "mois-offer-" + UUID.randomUUID();
            OfferCard offer = new OfferCard(
                    offerId,
                    requestId,
                    request.nicheName(),
                    source.title().isBlank() ? "Oferta " + source.host() : source.title(),
                    source.host(),
                    source.canonicalUrl(),
                    signature,
                    signals.promise(),
                    inferPrimaryOfferType(source),
                    signals.mainPrice(),
                    signals.confidence(),
                    List.of(sourceId),
                    signals.deliverables(),
                    signals.pricePoints(),
                    signals.proof(),
                    signals.mechanism(),
                    signals.funnelPattern(),
                    Instant.now());

            deduplicated.put(dedupeKey, offer);
            offers.put(offerId, offer);
        }

        DiscoveryStatus finalStatus = deduplicated.isEmpty() ? DiscoveryStatus.FAILED : DiscoveryStatus.COLLECTED;
        requests.put(requestId, new DiscoveryRequest(
                request.requestId(),
                request.nicheName(),
                request.marketTheme(),
                request.painOrOutcomeFocus(),
                request.seedQueries(),
                request.seedUrls(),
                request.channels(),
                request.country(),
                request.language(),
                request.discoveryPolicy(),
                finalStatus,
                request.createdAt(),
                Instant.now()));

        if (finalStatus == DiscoveryStatus.COLLECTED) {
            reports.put("mois-report-" + requestId, buildInsightReport("mois-report-" + requestId, requests.get(requestId)));
        }

        log.info("mois_run_finished requestId={} correlationId={} status={} snapshots={} offers={} deduplicatedOffers={}",
                requestId, correlationId, finalStatus.name(), snapshots.values().stream().filter(s -> s.requestId().equals(requestId)).count(),
                listOffersByRequest(requestId).size(), deduplicated.size());

        return Optional.of(new MoisDiscoveryDtos.AsyncAcceptedResponse("ACCEPTED", correlationId));
    }

    public MoisOfferDtos.OfferCardListResponse listOffers(String requestId, String nicheName, String sellerOrBrand) {
        List<MoisOfferDtos.OfferCardSummaryResponse> items = offers.values().stream()
                .filter(offer -> requestId == null || requestId.isBlank() || offer.requestId().equalsIgnoreCase(requestId))
                .filter(offer -> nicheName == null || nicheName.isBlank() || offer.nicheName().equalsIgnoreCase(nicheName))
                .filter(offer -> sellerOrBrand == null || sellerOrBrand.isBlank()
                        || (offer.sellerOrBrand() != null && offer.sellerOrBrand().toLowerCase().contains(sellerOrBrand.toLowerCase())))
                .sorted(Comparator.comparing(OfferCard::createdAt).reversed())
                .map(offer -> new MoisOfferDtos.OfferCardSummaryResponse(
                        offer.artifactId(),
                        offer.requestId(),
                        offer.nicheName(),
                        offer.offerName(),
                        offer.sellerOrBrand(),
                        offer.corePromise(),
                        offer.primaryOfferType(),
                        offer.mainPrice(),
                        offer.confidence()))
                .toList();
        return new MoisOfferDtos.OfferCardListResponse(items);
    }

    public Optional<MoisOfferDtos.OfferCardResponse> getOffer(String offerId) {
        return Optional.ofNullable(offers.get(offerId))
                .map(offer -> new MoisOfferDtos.OfferCardResponse(
                        offer.artifactId(),
                        offer.requestId(),
                        offer.nicheName(),
                        offer.offerName(),
                        offer.sellerOrBrand(),
                        offer.corePromise(),
                        offer.primaryOfferType(),
                        offer.mainPrice(),
                        offer.confidence(),
                        offer.evidenceRefs(),
                        offer.deliverables(),
                        offer.pricePoints(),
                        offer.proofSummary(),
                        offer.mechanismClaimSummary(),
                        offer.funnelPatternSummary(),
                        collectSourceArtifactsForOffer(offer.requestId())));
    }

    public MoisInsightDtos.InsightReportListResponse listInsightReports(String requestId, String nicheName, String category) {
        List<MoisInsightDtos.InsightReportSummaryResponse> items = requests.values().stream()
                .filter(req -> req.status() == DiscoveryStatus.COLLECTED)
                .filter(req -> requestId == null || requestId.isBlank() || req.requestId().equals(requestId))
                .filter(req -> nicheName == null || nicheName.isBlank() || req.nicheName().equalsIgnoreCase(nicheName))
                .filter(req -> category == null || category.isBlank() || hasCategoryMatch(req.requestId(), category))
                .sorted(Comparator.comparing(DiscoveryRequest::createdAt).reversed())
                .map(req -> new MoisInsightDtos.InsightReportSummaryResponse(
                        "mois-report-" + req.requestId(),
                        req.requestId(),
                        req.nicheName(),
                        req.marketTheme(),
                        "DRAFT",
                        req.createdAt(),
                        listOffersByRequest(req.requestId()).size()))
                .toList();

        return new MoisInsightDtos.InsightReportListResponse(items);
    }

    public Optional<MoisInsightDtos.InsightReportResponse> getInsightReport(String reportId) {
        MoisInsightDtos.InsightReportResponse persisted = reports.get(reportId);
        if (persisted != null) {
            return Optional.of(persisted);
        }
        String prefix = "mois-report-";
        if (!reportId.startsWith(prefix)) {
            return Optional.empty();
        }
        String requestId = reportId.substring(prefix.length());
        DiscoveryRequest request = requests.get(requestId);
        if (request == null || request.status() != DiscoveryStatus.COLLECTED) {
            return Optional.empty();
        }
        MoisInsightDtos.InsightReportResponse computed = buildInsightReport(reportId, request);
        reports.put(reportId, computed);
        return Optional.of(computed);
    }

    public Optional<MoisInsightDtos.InsightExecutiveSummaryResponse> getInsightExecutiveSummary(String reportId) {
        return getInsightReport(reportId)
                .map(report -> new MoisInsightDtos.InsightExecutiveSummaryResponse(
                        report.reportId(),
                        report.requestId(),
                        report.nicheName(),
                        report.marketTheme(),
                        report.frameworkRecommendation(),
                        report.gapOpportunities().stream().limit(3).toList(),
                        report.saturationSignals().stream().limit(5).toList(),
                        report.recommendedNextActions()));
    }

    private MoisInsightDtos.InsightReportResponse buildInsightReport(String reportId, DiscoveryRequest request) {
        String requestId = request.requestId();
        List<OfferCard> requestOffers = listOffersByRequest(requestId);
        int offerCount = requestOffers.size();

        List<MoisInsightDtos.InsightReportPatternResponse> repeatedPromises =
                buildPatternDistribution(requestOffers, OfferCard::corePromise, offerCount);
        List<MoisInsightDtos.InsightReportPatternResponse> repeatedProofPatterns =
                buildPatternDistribution(requestOffers, OfferCard::proofSummary, offerCount);
        List<MoisInsightDtos.InsightReportPatternResponse> pricingPatterns =
                buildPatternDistribution(requestOffers, OfferCard::mainPrice, offerCount);
        List<MoisInsightDtos.InsightReportPatternResponse> funnelPatterns =
                buildPatternDistribution(requestOffers, OfferCard::funnelPatternSummary, offerCount);
        List<MoisInsightDtos.InsightReportPatternResponse> mechanismPatterns =
                buildPatternDistribution(requestOffers, OfferCard::mechanismClaimSummary, offerCount);
        List<MoisInsightDtos.SaturationSignalResponse> saturationSignals = buildSaturationSignals(requestOffers);
        MoisInsightDtos.FrameworkRecommendationResponse frameworkRecommendation =
                buildFrameworkRecommendation(request, repeatedPromises, repeatedProofPatterns, mechanismPatterns, pricingPatterns);

        return new MoisInsightDtos.InsightReportResponse(
                reportId,
                requestId,
                request.nicheName(),
                request.marketTheme(),
                "DRAFT",
                request.createdAt(),
                new MoisInsightDtos.InsightReportRequestSummary(
                        request.requestId(),
                        request.nicheName(),
                        request.marketTheme(),
                        request.painOrOutcomeFocus(),
                        request.status().name(),
                        request.createdAt(),
                        request.updatedAt()),
                requestOffers.stream().map(OfferCard::artifactId).toList(),
                repeatedPromises,
                repeatedProofPatterns,
                pricingPatterns,
                funnelPatterns,
                mechanismPatterns,
                saturationSignals,
                buildSaturationNotes(repeatedPromises, pricingPatterns, offerCount),
                buildGapOpportunities(requestOffers, repeatedPromises, pricingPatterns, mechanismPatterns, repeatedProofPatterns),
                buildDifferentiationSignals(requestOffers, repeatedPromises, mechanismPatterns),
                buildRecommendedActions(repeatedPromises, pricingPatterns, mechanismPatterns),
                frameworkRecommendation);
    }

    public Optional<MoisArtifactDtos.ArtifactEnvelopeResponse> getArtifact(String artifactId) {
        DiscoveryRequest request = requests.get(artifactId);
        if (request != null) {
            return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                    request.requestId(),
                    "mois.marketOfferDiscoveryRequest.v1",
                    "v1",
                    request.status().name(),
                    MODULE_NAME,
                    CREATED_BY,
                    request.createdAt(),
                    request.updatedAt(),
                    Map.of("requestId", request.requestId(), "parentArtifactIds", List.of()),
                    Map.of("requestId", request.requestId()),
                    Map.of(
                            "nicheName", request.nicheName(),
                            "marketTheme", request.marketTheme(),
                            "painOrOutcomeFocus", request.painOrOutcomeFocus(),
                            "seedQueries", request.seedQueries(),
                            "seedUrls", request.seedUrls(),
                            "channels", request.channels(),
                            "country", request.country(),
                            "language", request.language(),
                            "discoveryPolicy", request.discoveryPolicy())));
        }

        SourceSnapshot snapshot = snapshots.get(artifactId);
        if (snapshot != null) {
            return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                    snapshot.artifactId(),
                    "mois.marketOfferSourceSnapshot.v1",
                    "v1",
                    "COLLECTED",
                    MODULE_NAME,
                    CREATED_BY,
                    snapshot.capturedAt(),
                    snapshot.capturedAt(),
                    Map.of("requestId", snapshot.requestId(), "parentArtifactIds", List.of(snapshot.requestId())),
                    Map.of("sourceKind", snapshot.sourceKind()),
                    Map.of(
                            "sourceUrl", snapshot.sourceUrl(),
                            "canonicalUrl", snapshot.canonicalUrl(),
                            "sourceTitle", snapshot.sourceTitle(),
                            "sourceKind", snapshot.sourceKind(),
                            "capturedAt", snapshot.capturedAt(),
                            "rawExcerpt", snapshot.rawExcerpt())));
        }

        OfferCard offer = offers.get(artifactId);
        if (offer != null) {
            Map<String, Object> offerContent = new HashMap<>();
            offerContent.put("canonicalUrl", offer.canonicalUrl());
            offerContent.put("contentSignature", offer.contentSignature());
            offerContent.put("corePromise", offer.corePromise());
            offerContent.put("primaryOfferType", offer.primaryOfferType());
            offerContent.put("mainPrice", offer.mainPrice());
            offerContent.put("confidence", offer.confidence());
            offerContent.put("evidenceRefs", offer.evidenceRefs());
            offerContent.put("deliverables", offer.deliverables());
            offerContent.put("pricePoints", offer.pricePoints());
            offerContent.put("proofSummary", offer.proofSummary());
            offerContent.put("mechanismClaimSummary", offer.mechanismClaimSummary());
            offerContent.put("funnelPatternSummary", offer.funnelPatternSummary());
            return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                    offer.artifactId(),
                    "mois.marketOfferCard.v1",
                    "v1",
                    "COLLECTED",
                    MODULE_NAME,
                    CREATED_BY,
                    offer.createdAt(),
                    offer.createdAt(),
                    Map.of("requestId", offer.requestId(), "parentArtifactIds", offer.evidenceRefs()),
                    Map.of("offerName", offer.offerName(), "sellerOrBrand", offer.sellerOrBrand()),
                    offerContent));
        }
        MoisInsightDtos.InsightReportResponse report = reports.get(artifactId);
        if (report != null) {
            Map<String, Object> content = new HashMap<>();
            content.put("requestSummary", report.requestSummary());
            content.put("offersAnalyzed", report.offersAnalyzed());
            content.put("repeatedPromises", report.repeatedPromises());
            content.put("repeatedProofPatterns", report.repeatedProofPatterns());
            content.put("pricingPatterns", report.pricingPatterns());
            content.put("funnelPatterns", report.funnelPatterns());
            content.put("mechanismClaimPatterns", report.mechanismClaimPatterns());
            content.put("saturationSignals", report.saturationSignals());
            content.put("saturationNotes", report.saturationNotes());
            content.put("gapOpportunities", report.gapOpportunities());
            content.put("differentiationSignals", report.differentiationSignals());
            content.put("recommendedNextActions", report.recommendedNextActions());
            content.put("frameworkRecommendation", report.frameworkRecommendation());
            return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                    report.reportId(),
                    "mois.marketOfferInsightReport.v1",
                    "v1",
                    report.status(),
                    MODULE_NAME,
                    CREATED_BY,
                    report.createdAt(),
                    report.createdAt(),
                    Map.of("requestId", report.requestId(), "parentArtifactIds", report.offersAnalyzed()),
                    Map.of("nicheName", report.nicheName(), "marketTheme", report.marketTheme()),
                    content));
        }
        return Optional.empty();
    }

    public MoisWorkspaceDtos.CollectionJobResponse createCollectionJob(MoisWorkspaceDtos.CreateCollectionJobRequest request) {
        if (!isCollectionRolloutAllowed(request.workspaceId())) {
            throw new IllegalStateException("collection rollout is disabled for workspace");
        }
        String jobId = "mois-collect-" + UUID.randomUUID();
        Instant now = Instant.now();
        int limitPerSource = request.limitPerSource() == null ? DEFAULT_LIMIT_PER_SOURCE : request.limitPerSource();
        int minSuccessScore = request.minSuccessScore() == null ? DEFAULT_MIN_SUCCESS_SCORE : request.minSuccessScore();
        MoisWorkspaceDtos.CollectionJobResponse queuedJob = new MoisWorkspaceDtos.CollectionJobResponse(
                jobId,
                request.workspaceId(),
                request.niche(),
                request.marketTheme(),
                "QUEUED",
                request.timeWindow(),
                limitPerSource,
                minSuccessScore,
                request.sources(),
                now
        );
        collectionJobs.put(jobId, queuedJob);
        persistCollectionState(jobId);
        MoisWorkspaceDtos.CollectionJobResponse runningJob = new MoisWorkspaceDtos.CollectionJobResponse(
                queuedJob.jobId(),
                queuedJob.workspaceId(),
                queuedJob.niche(),
                queuedJob.marketTheme(),
                "RUNNING",
                queuedJob.timeWindow(),
                queuedJob.limitPerSource(),
                queuedJob.minSuccessScore(),
                queuedJob.sources(),
                queuedJob.createdAt()
        );
        collectionJobs.put(jobId, runningJob);
        persistCollectionState(jobId);
        CollectionExecutionResult execution = runCollectionWithRetries(runningJob, request.niche());
        collectedReferencesByJob.put(jobId, reRank(execution.references()));
        collectionJobRuntimeStats.put(jobId, execution.runtimeStats());
        MoisWorkspaceDtos.CollectionJobResponse completedJob = new MoisWorkspaceDtos.CollectionJobResponse(
                runningJob.jobId(),
                runningJob.workspaceId(),
                runningJob.niche(),
                runningJob.marketTheme(),
                execution.hasFailure() ? "FAILED" : "COMPLETED",
                runningJob.timeWindow(),
                runningJob.limitPerSource(),
                runningJob.minSuccessScore(),
                runningJob.sources(),
                runningJob.createdAt()
        );
        collectionJobs.put(jobId, completedJob);
        persistCollectionState(jobId);
        log.info("mois_collection_job_created jobId={} workspaceId={} sources={} window={}",
                jobId, request.workspaceId(), request.sources(), request.timeWindow());
        return completedJob;
    }

    public MoisWorkspaceDtos.CollectionOpsSummaryResponse getCollectionOpsSummary(String workspaceId) {
        hydrateCollectionState(workspaceId, null);
        boolean rolloutEnabled = isCollectionRolloutAllowed(workspaceId);
        List<MoisWorkspaceDtos.CollectionJobResponse> workspaceJobs = collectionJobs.values().stream()
                .filter(job -> workspaceId == null || workspaceId.isBlank() || workspaceId.equals(job.workspaceId()))
                .toList();
        int queued = (int) workspaceJobs.stream().filter(job -> "QUEUED".equalsIgnoreCase(job.status())).count();
        int running = (int) workspaceJobs.stream().filter(job -> "RUNNING".equalsIgnoreCase(job.status())).count();
        int completed = (int) workspaceJobs.stream().filter(job -> "COMPLETED".equalsIgnoreCase(job.status())).count();
        int failed = (int) workspaceJobs.stream().filter(job -> "FAILED".equalsIgnoreCase(job.status())).count();
        int totalRetries = workspaceJobs.stream()
                .map(MoisWorkspaceDtos.CollectionJobResponse::jobId)
                .map(collectionJobRuntimeStats::get)
                .filter(Objects::nonNull)
                .mapToInt(CollectionJobRuntimeStats::retries)
                .sum();
        long avgLatencyMs = (long) workspaceJobs.stream()
                .map(MoisWorkspaceDtos.CollectionJobResponse::jobId)
                .map(collectionJobRuntimeStats::get)
                .filter(Objects::nonNull)
                .mapToLong(CollectionJobRuntimeStats::latencyMs)
                .average()
                .orElse(0);
        int totalReferences = workspaceJobs.stream()
                .map(MoisWorkspaceDtos.CollectionJobResponse::jobId)
                .map(collectedReferencesByJob::get)
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
        List<MoisWorkspaceDtos.CollectionSourceOpsSummaryResponse> sourceBreakdown = collectionOpsByWorkspace
                .getOrDefault(workspaceId, Map.of())
                .values()
                .stream()
                .sorted(Comparator.comparing(SourceOpsStats::source))
                .map(item -> new MoisWorkspaceDtos.CollectionSourceOpsSummaryResponse(
                        item.source(),
                        item.attempts(),
                        item.successes(),
                        item.failures(),
                        item.retries(),
                        item.rateLimitedEvents(),
                        item.averageLatencyMs(),
                        item.lastError(),
                        item.lastAttemptAt()
                ))
                .toList();
        return new MoisWorkspaceDtos.CollectionOpsSummaryResponse(
                workspaceId,
                rolloutEnabled,
                workspaceJobs.size(),
                queued,
                running,
                completed,
                failed,
                totalReferences,
                avgLatencyMs,
                totalRetries,
                sourceBreakdown,
                Instant.now()
        );
    }

    public MoisWorkspaceDtos.CollectionJobListResponse listCollectionJobs(String workspaceId, String status) {
        hydrateCollectionState(workspaceId, status);
        List<MoisWorkspaceDtos.CollectionJobResponse> jobs = collectionJobs.values().stream()
                .filter(item -> workspaceId == null || workspaceId.isBlank() || workspaceId.equals(item.workspaceId()))
                .filter(item -> status == null || status.isBlank() || status.equalsIgnoreCase(item.status()))
                .sorted(Comparator.comparing(MoisWorkspaceDtos.CollectionJobResponse::createdAt).reversed())
                .toList();
        return new MoisWorkspaceDtos.CollectionJobListResponse(jobs);
    }

    public Optional<MoisWorkspaceDtos.CollectedReferenceListResponse> listCollectedReferencesByJob(
            String jobId,
            String source,
            String niche,
            Integer minSuccessScore,
            String confidenceLevel
    ) {
        hydrateCollectionStateByJob(jobId);
        if (!collectionJobs.containsKey(jobId)) {
            return Optional.empty();
        }
        int minimumScore = minSuccessScore == null ? 0 : Math.max(0, minSuccessScore);
        List<MoisWorkspaceDtos.CollectedReferenceResponse> filtered = collectedReferencesByJob.getOrDefault(jobId, List.of()).stream()
                .filter(item -> source == null || source.isBlank() || source.equalsIgnoreCase(item.source()))
                .filter(item -> niche == null || niche.isBlank() || niche.equalsIgnoreCase(item.niche()))
                .filter(item -> item.successScore() >= minimumScore)
                .filter(item -> confidenceLevel == null || confidenceLevel.isBlank()
                        || confidenceLevel.equalsIgnoreCase(item.confidenceLevel()))
                .sorted(Comparator.comparingInt(MoisWorkspaceDtos.CollectedReferenceResponse::successScore).reversed()
                        .thenComparing(MoisWorkspaceDtos.CollectedReferenceResponse::collectedAt, Comparator.reverseOrder()))
                .toList();
        return Optional.of(new MoisWorkspaceDtos.CollectedReferenceListResponse(
                jobId,
                reRank(filtered)
        ));
    }

    public Optional<MoisWorkspaceDtos.CollectedReferenceActionResponse> favoriteCollectedReference(String jobId, String referenceId) {
        return updateCollectedReference(jobId, referenceId, "FAVORITE", item -> new MoisWorkspaceDtos.CollectedReferenceResponse(
                item.referenceId(),
                item.jobId(),
                item.source(),
                item.title(),
                item.url(),
                item.niche(),
                item.status(),
                true,
                item.importedReferenceId(),
                item.successScore(),
                item.successSignal(),
                item.confidenceLevel(),
                item.rankingPosition(),
                item.engagementRelative(),
                item.recurrenceScore(),
                item.evidenceScore(),
                item.collectedAt(),
                item.rawMetadata()
        ));
    }

    public Optional<MoisWorkspaceDtos.CollectedReferenceActionResponse> discardCollectedReference(String jobId, String referenceId) {
        return updateCollectedReference(jobId, referenceId, "DISCARD", item -> new MoisWorkspaceDtos.CollectedReferenceResponse(
                item.referenceId(),
                item.jobId(),
                item.source(),
                item.title(),
                item.url(),
                item.niche(),
                "DISCARDED",
                item.favorite(),
                item.importedReferenceId(),
                item.successScore(),
                item.successSignal(),
                item.confidenceLevel(),
                item.rankingPosition(),
                item.engagementRelative(),
                item.recurrenceScore(),
                item.evidenceScore(),
                item.collectedAt(),
                item.rawMetadata()
        ));
    }

    public Optional<MoisWorkspaceDtos.CollectedReferenceActionResponse> importCollectedReference(String jobId, String referenceId) {
        return importCollectedReferenceInternal(jobId, referenceId, false);
    }

    public Optional<MoisWorkspaceDtos.CollectedReferenceActionResponse> importAndStartExtraction(String jobId, String referenceId) {
        return importCollectedReferenceInternal(jobId, referenceId, true);
    }

    public Optional<MoisWorkspaceDtos.CollectedReferenceLineageResponse> getCollectedReferenceLineage(String jobId, String referenceId) {
        hydrateCollectionStateByJob(jobId);
        MoisWorkspaceDtos.CollectedReferenceLineageResponse lineage = collectedReferenceLineage.get(lineageKey(jobId, referenceId));
        return Optional.ofNullable(lineage);
    }

    private Optional<MoisWorkspaceDtos.CollectedReferenceActionResponse> importCollectedReferenceInternal(
            String jobId,
            String referenceId,
            boolean startExtraction
    ) {
        hydrateCollectionStateByJob(jobId);
        if (!collectionJobs.containsKey(jobId)) {
            return Optional.empty();
        }
        MoisWorkspaceDtos.CollectionJobResponse job = collectionJobs.get(jobId);
        List<MoisWorkspaceDtos.CollectedReferenceResponse> items = collectedReferencesByJob.getOrDefault(jobId, List.of());
        for (MoisWorkspaceDtos.CollectedReferenceResponse item : items) {
            if (!item.referenceId().equals(referenceId)) {
                continue;
            }
            String importedReferenceId = item.importedReferenceId() == null || item.importedReferenceId().isBlank()
                    ? UUID.randomUUID().toString()
                    : item.importedReferenceId();
            MoisWorkspaceDtos.ReferenceResponse imported = new MoisWorkspaceDtos.ReferenceResponse(
                    importedReferenceId,
                    job.workspaceId(),
                    item.niche(),
                    item.url(),
                    "AUTO_COLLECTED",
                    item.title(),
                    "SOLUTION_AWARE",
                    null,
                    null,
                    "Importado automaticamente da coleta " + jobId,
                    Instant.now()
            );
            referencesById.put(importedReferenceId, imported);
            String extractionId = null;
            if (startExtraction) {
                MoisWorkspaceDtos.UpsertExtractionDraftRequest extractionSeed = new MoisWorkspaceDtos.UpsertExtractionDraftRequest(
                        "Dor inferida da referência coletada",
                        item.title(),
                        "Mecanismo a validar com base no criativo coletado",
                        "Prova inicial: score " + item.successScore(),
                        "Oferta importada de " + item.source(),
                        List.of(item.url())
                );
                MoisWorkspaceDtos.ExtractionDraftResponse extractionDraft = upsertExtractionDraft(importedReferenceId, extractionSeed);
                extractionId = extractionDraft.extractionId();
            }

            List<String> generatedBlockIds = generateLibraryBlocksFromCollectedReference(job.workspaceId(), item);
            collectedReferenceLineage.put(
                    lineageKey(jobId, referenceId),
                    new MoisWorkspaceDtos.CollectedReferenceLineageResponse(
                            jobId,
                            referenceId,
                            item.url(),
                            importedReferenceId,
                            extractionId,
                            generatedBlockIds,
                            Instant.now()
                    )
            );
            persistCollectionState(jobId);

            final String finalExtractionId = extractionId;
            final List<String> finalGeneratedBlockIds = generatedBlockIds;
            return updateCollectedReference(jobId, referenceId, startExtraction ? "IMPORT_AND_START_EXTRACTION" : "IMPORT",
                    current -> new MoisWorkspaceDtos.CollectedReferenceResponse(
                    current.referenceId(),
                    current.jobId(),
                    current.source(),
                    current.title(),
                    current.url(),
                    current.niche(),
                    "IMPORTED",
                    current.favorite(),
                    importedReferenceId,
                    current.successScore(),
                    current.successSignal(),
                    current.confidenceLevel(),
                    current.rankingPosition(),
                    current.engagementRelative(),
                    current.recurrenceScore(),
                    current.evidenceScore(),
                    current.collectedAt(),
                    current.rawMetadata()
            ), finalExtractionId, finalGeneratedBlockIds);
        }
        return Optional.empty();
    }

    private List<OfferCard> listOffersByRequest(String requestId) {
        return offers.values().stream()
                .filter(offer -> offer.requestId().equals(requestId))
                .toList();
    }

    private boolean hasCategoryMatch(String requestId, String category) {
        String normalizedCategory = category.trim().toLowerCase();
        return listOffersByRequest(requestId).stream()
                .anyMatch(offer -> offer.primaryOfferType() != null
                        && offer.primaryOfferType().toLowerCase().contains(normalizedCategory));
    }

    private List<MoisInsightDtos.InsightReportPatternResponse> buildPatternDistribution(
            List<OfferCard> requestOffers,
            Function<OfferCard, String> extractor,
            int offerCount
    ) {
        if (offerCount == 0) {
            return List.of();
        }

        return requestOffers.stream()
                .map(extractor)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new MoisInsightDtos.InsightReportPatternResponse(
                        entry.getKey(),
                        entry.getValue(),
                        entry.getValue() / (double) offerCount))
                .toList();
    }

    private List<String> buildSaturationNotes(
            List<MoisInsightDtos.InsightReportPatternResponse> repeatedPromises,
            List<MoisInsightDtos.InsightReportPatternResponse> pricingPatterns,
            int offerCount
    ) {
        if (offerCount == 0) {
            return List.of("Não há ofertas suficientes para inferir saturação.");
        }

        List<String> notes = new ArrayList<>();
        repeatedPromises.stream().findFirst().ifPresent(topPromise -> {
            if (topPromise.share() >= 0.6) {
                notes.add("Saturação alta de promessa central ('" + topPromise.label() + "') em " + Math.round(topPromise.share() * 100) + "% das ofertas.");
            }
        });
        pricingPatterns.stream().findFirst().ifPresent(topPrice -> {
            if (topPrice.share() >= 0.6) {
                notes.add("Faixa de preço dominante em torno de " + topPrice.label() + ", sugerindo baixa diferenciação monetária.");
            }
        });
        if (notes.isEmpty()) {
            notes.add("Mercado com saturação moderada: sem padrão dominante acima de 60% nas ofertas analisadas.");
        }
        return notes;
    }

    private List<MoisInsightDtos.GapOpportunityResponse> buildGapOpportunities(
            List<OfferCard> requestOffers,
            List<MoisInsightDtos.InsightReportPatternResponse> repeatedPromises,
            List<MoisInsightDtos.InsightReportPatternResponse> pricingPatterns,
            List<MoisInsightDtos.InsightReportPatternResponse> mechanismPatterns,
            List<MoisInsightDtos.InsightReportPatternResponse> repeatedProofPatterns
    ) {
        if (requestOffers.isEmpty()) {
            return List.of();
        }

        List<MoisInsightDtos.GapOpportunityResponse> gaps = new ArrayList<>();
        repeatedPromises.stream().findFirst().ifPresent(topPromise -> {
            if (topPromise.share() >= 0.7) {
                gaps.add(new MoisInsightDtos.GapOpportunityResponse(
                        "PROMISE_DIFFERENTIATION",
                        "Promessa dominante muito repetida no nicho.",
                        "Ofertas com promessa alternativa tendem a destacar posicionamento em mercado saturado.",
                        requestOffers.stream().map(OfferCard::artifactId).toList(),
                        "HIGH",
                        0.72,
                        List.of("promise_share>0.70", "offer_count>3")));
            }
        });

        if (pricingPatterns.size() <= 1) {
            gaps.add(new MoisInsightDtos.GapOpportunityResponse(
                    "PRICING_MODEL_VARIETY",
                    "Baixa variedade aparente de modelo de preço entre as ofertas.",
                    "Adicionar modelos de ancoragem, parcelamento ou ticket escalonado pode ampliar conversão por perfil.",
                    requestOffers.stream().map(OfferCard::artifactId).toList(),
                    "MEDIUM",
                    0.66,
                    List.of("unique_price_patterns<=1")));
        }

        mechanismPatterns.stream().findFirst().ifPresent(topMechanism -> {
            if (topMechanism.share() >= 0.65) {
                gaps.add(new MoisInsightDtos.GapOpportunityResponse(
                        "MECHANISM_PROOF_GAP",
                        "Um único mecanismo domina o discurso competitivo.",
                        "Há espaço para combinar mecanismo alternativo com prova específica e reduzir paridade.",
                        requestOffers.stream().map(OfferCard::artifactId).toList(),
                        "HIGH",
                        0.74,
                        List.of("mechanism_share>=0.65", "proof_pattern_diversity<3")));
            }
        });

        if (repeatedProofPatterns.size() <= 1) {
            gaps.add(new MoisInsightDtos.GapOpportunityResponse(
                    "PROOF_ANGLE_UNEXPLORED",
                    "Pouca variação de prova nas ofertas coletadas.",
                    "Introduzir prova operacional e estudos de caso aprofundados aumenta credibilidade percebida.",
                    requestOffers.stream().map(OfferCard::artifactId).toList(),
                    "MEDIUM",
                    0.61,
                    List.of("unique_proof_patterns<=1")));
        }
        return gaps.stream()
                .sorted(Comparator.comparingDouble(MoisInsightDtos.GapOpportunityResponse::confidence).reversed())
                .toList();
    }

    private List<MoisInsightDtos.SaturationSignalResponse> buildSaturationSignals(List<OfferCard> requestOffers) {
        if (requestOffers.isEmpty()) {
            return List.of();
        }

        int totalOffers = requestOffers.size();
        Map<String, Long> grouped = requestOffers.stream()
                .collect(Collectors.groupingBy(
                        offer -> offer.primaryOfferType() + "|" + toPriceBand(offer.mainPrice()),
                        LinkedHashMap::new,
                        Collectors.counting()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String[] key = entry.getKey().split("\\|", 2);
                    long count = entry.getValue();
                    double score = count / (double) totalOffers;
                    return new MoisInsightDtos.SaturationSignalResponse(key[0], key[1], count, score);
                })
                .sorted(Comparator.comparingDouble(MoisInsightDtos.SaturationSignalResponse::saturationScore).reversed())
                .toList();
    }

    private MoisInsightDtos.FrameworkRecommendationResponse buildFrameworkRecommendation(
            DiscoveryRequest request,
            List<MoisInsightDtos.InsightReportPatternResponse> repeatedPromises,
            List<MoisInsightDtos.InsightReportPatternResponse> repeatedProofPatterns,
            List<MoisInsightDtos.InsightReportPatternResponse> mechanismPatterns,
            List<MoisInsightDtos.InsightReportPatternResponse> pricingPatterns
    ) {
        String dominantPain = request.painOrOutcomeFocus() == null || request.painOrOutcomeFocus().isBlank()
                ? "Dor não especificada na requisição"
                : request.painOrOutcomeFocus();
        String mostPromisedOutcome = topLabel(repeatedPromises, "Sem padrão de resultado dominante");
        String mostExploredMechanism = topLabel(mechanismPatterns, "Mecanismo ainda difuso");
        String mostUsedProof = topLabel(repeatedProofPatterns, "Prova pouco explicitada");
        List<String> subexploredAngles = new ArrayList<>();
        if (mechanismPatterns.size() > 1) {
            subexploredAngles.add("Explorar mecanismo alternativo: " + mechanismPatterns.get(1).label());
        }
        if (pricingPatterns.size() > 1) {
            subexploredAngles.add("Testar ângulo de oferta na faixa " + pricingPatterns.get(pricingPatterns.size() - 1).label());
        }
        if (subexploredAngles.isEmpty()) {
            subexploredAngles.add("Buscar combinação inédita de mecanismo + prova para romper saturação.");
        }
        return new MoisInsightDtos.FrameworkRecommendationResponse(
                dominantPain,
                mostPromisedOutcome,
                mostExploredMechanism,
                mostUsedProof,
                subexploredAngles);
    }

    private String topLabel(List<MoisInsightDtos.InsightReportPatternResponse> patterns, String fallback) {
        return patterns.stream().findFirst().map(MoisInsightDtos.InsightReportPatternResponse::label).orElse(fallback);
    }

    private String toPriceBand(String mainPrice) {
        if (mainPrice == null || mainPrice.isBlank()) {
            return "UNSPECIFIED";
        }
        double numeric = parsePrice(mainPrice);
        if (numeric <= 0) {
            return "UNSPECIFIED";
        }
        if (numeric < 100) {
            return "LOW";
        }
        if (numeric < 500) {
            return "MID";
        }
        return "HIGH";
    }

    private double parsePrice(String rawPrice) {
        String sanitized = rawPrice.replace("R$", "").replace("$", "").trim();
        sanitized = sanitized.replace(".", "").replace(",", ".");
        try {
            return Double.parseDouble(sanitized);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private List<String> buildDifferentiationSignals(
            List<OfferCard> requestOffers,
            List<MoisInsightDtos.InsightReportPatternResponse> repeatedPromises,
            List<MoisInsightDtos.InsightReportPatternResponse> mechanismPatterns
    ) {
        if (requestOffers.isEmpty()) {
            return List.of();
        }

        List<String> signals = new ArrayList<>();
        repeatedPromises.stream().findFirst().ifPresent(topPromise -> {
            if (topPromise.share() < 0.6) {
                signals.add("Diferenciação de promessa distribuída: nenhuma promessa isolada domina o mercado.");
            }
        });
        mechanismPatterns.stream().findFirst().ifPresent(topMechanism -> {
            if (topMechanism.share() < 0.6) {
                signals.add("Mecanismos alegados variados indicam espaço para narrativa proprietária sem ruptura do mercado.");
            }
        });

        if (signals.isEmpty()) {
            signals.add("Diferenciação aparente baixa: recomenda-se buscar nova combinação de mecanismo + prova para romper paridade.");
        }
        return signals;
    }

    private List<String> buildRecommendedActions(
            List<MoisInsightDtos.InsightReportPatternResponse> repeatedPromises,
            List<MoisInsightDtos.InsightReportPatternResponse> pricingPatterns,
            List<MoisInsightDtos.InsightReportPatternResponse> mechanismPatterns
    ) {
        List<String> actions = new ArrayList<>();
        repeatedPromises.stream().findFirst().ifPresent(topPromise -> {
            if (topPromise.share() >= 0.6) {
                actions.add("Testar promessa com recorte de dor/resultados mais específico que o padrão dominante atual.");
            }
        });
        pricingPatterns.stream().findFirst().ifPresent(topPrice -> {
            if (topPrice.share() >= 0.6) {
                actions.add("Explorar estratégia de preço escalonado para reduzir colisão direta com a faixa " + topPrice.label() + ".");
            }
        });
        mechanismPatterns.stream().findFirst().ifPresent(topMechanism -> {
            if (topMechanism.share() >= 0.6) {
                actions.add("Elevar prova concreta do mecanismo para evitar percepção de promessa genérica de mercado.");
            }
        });

        if (actions.isEmpty()) {
            actions.add("Executar rodada complementar de coleta com novas fontes para aumentar confiança da consolidação.");
        }
        return actions;
    }

    private List<MoisDiscoveryDtos.ArtifactRefResponse> collectArtifactsForRequest(String requestId) {
        List<MoisDiscoveryDtos.ArtifactRefResponse> refs = new ArrayList<>();
        snapshots.values().stream()
                .filter(item -> item.requestId().equals(requestId))
                .forEach(item -> refs.add(new MoisDiscoveryDtos.ArtifactRefResponse(item.artifactId(), "mois.marketOfferSourceSnapshot.v1", "v1")));
        offers.values().stream()
                .filter(item -> item.requestId().equals(requestId))
                .forEach(item -> refs.add(new MoisDiscoveryDtos.ArtifactRefResponse(item.artifactId(), "mois.marketOfferCard.v1", "v1")));
        return refs;
    }

    private List<MoisDiscoveryDtos.ArtifactRefResponse> collectSourceArtifactsForOffer(String requestId) {
        return snapshots.values().stream()
                .filter(item -> item.requestId().equals(requestId))
                .map(item -> new MoisDiscoveryDtos.ArtifactRefResponse(item.artifactId(), "mois.marketOfferSourceSnapshot.v1", "v1"))
                .toList();
    }

    private CollectionExecutionResult runCollectionWithRetries(MoisWorkspaceDtos.CollectionJobResponse job, String niche) {
        List<MoisWorkspaceDtos.CollectedReferenceResponse> items = new ArrayList<>();
        int index = 0;
        boolean hasFailure = false;
        int retries = 0;
        long totalLatencyMs = 0;
        int safeLimitPerSource = Math.max(1, job.limitPerSource());
        collectedReferencesByJob.put(job.jobId(), new ArrayList<>());
        for (String source : job.sources()) {
            String normalizedSource = source == null ? "UNKNOWN" : source.trim().toUpperCase();
            SourceExecutionOutcome outcome = executeSourceCollection(job.workspaceId(), source, index + 1);
            retries += outcome.retries();
            totalLatencyMs += outcome.latencyMs();
            if (!outcome.success()) {
                hasFailure = true;
                continue;
            }
            List<SourceLead> sourceLeads = resolveSourceLeads(normalizedSource, niche, safeLimitPerSource);
            for (int sourceRank = 1; sourceRank <= safeLimitPerSource; sourceRank++) {
                index++;
                SourceMetricSnapshot metrics = buildSourceMetricSnapshot(job, source, index);
                NormalizedSuccessSignal normalized = normalizeSuccessSignal(metrics, job.minSuccessScore());
                SourceLead sourceLead = sourceRank <= sourceLeads.size()
                        ? sourceLeads.get(sourceRank - 1)
                        : new SourceLead(buildCollectionSourceUrl(normalizedSource, niche, sourceRank), null, null, null, null);
                MoisWorkspaceDtos.CollectedReferenceResponse item = buildCollectedReference(
                        job,
                        normalizedSource,
                        niche,
                        sourceLead,
                        index,
                        sourceRank,
                        outcome,
                        metrics,
                        normalized
                );
                items.add(item);
                collectedReferencesByJob.put(job.jobId(), reRank(items));
                persistCollectionState(job.jobId());
            }
        }
        return new CollectionExecutionResult(
                reRank(items),
                hasFailure,
                new CollectionJobRuntimeStats(job.jobId(), retries, totalLatencyMs, Instant.now())
        );
    }

    private MoisWorkspaceDtos.CollectedReferenceResponse buildCollectedReference(
            MoisWorkspaceDtos.CollectionJobResponse job,
            String source,
            String niche,
            SourceLead sourceLead,
            int globalIndex,
            int sourceRank,
            SourceExecutionOutcome outcome,
            SourceMetricSnapshot metrics,
            NormalizedSuccessSignal normalized
    ) {
        String title = "HOTMART".equals(source)
                ? trimTo(firstNonBlank(sourceLead.title(), "Oferta Hotmart #" + sourceRank + " • " + trimTo(niche, 50)), 120)
                : "Referência " + sourceRank + " (" + source + ")";
        Map<String, String> rawMetadata = new LinkedHashMap<>();
        rawMetadata.put("status", "ACTIVE");
        rawMetadata.put("timeWindow", job.timeWindow());
        rawMetadata.put("attempts", String.valueOf(outcome.attempts()));
        rawMetadata.put("retries", String.valueOf(outcome.retries()));
        rawMetadata.put("latencyMs", String.valueOf(outcome.latencyMs()));
        rawMetadata.put("engagementRaw", String.valueOf(metrics.engagementRaw()));
        rawMetadata.put("recurrenceRaw", String.valueOf(metrics.recurrenceRaw()));
        rawMetadata.put("evidenceRaw", String.valueOf(metrics.evidenceRaw()));
        rawMetadata.put("normalizationPolicy", "v1:0.45*engagement+0.35*recurrence+0.20*evidence");
        if (sourceLead.hotmartDescription() != null && !sourceLead.hotmartDescription().isBlank()) {
            rawMetadata.put("hotmartDescription", trimTo(sourceLead.hotmartDescription(), 220));
        }
        if (sourceLead.hotmartProducer() != null && !sourceLead.hotmartProducer().isBlank()) {
            rawMetadata.put("hotmartProducer", trimTo(sourceLead.hotmartProducer(), 120));
        }
        if (sourceLead.hotmartImageUrl() != null && !sourceLead.hotmartImageUrl().isBlank()) {
            rawMetadata.put("hotmartImageUrl", trimTo(sourceLead.hotmartImageUrl(), 260));
        }
        return new MoisWorkspaceDtos.CollectedReferenceResponse(
                UUID.randomUUID().toString(),
                job.jobId(),
                source,
                title,
                sourceLead.url(),
                niche,
                "ACTIVE",
                false,
                null,
                normalized.successScore(),
                normalized.successSignal(),
                normalized.confidenceLevel(),
                globalIndex,
                normalized.engagementRelative(),
                normalized.recurrenceScore(),
                normalized.evidenceScore(),
                job.createdAt().minusSeconds(globalIndex * 120L),
                rawMetadata
        );
    }

    private List<SourceLead> resolveSourceLeads(String source, String niche, int limitPerSource) {
        if ("HOTMART".equals(source)) {
            List<SourceLead> hotmartLeads = fetchHotmartProductLeads(niche, limitPerSource);
            if (!hotmartLeads.isEmpty()) {
                log.info("mois_hotmart_leads_resolved niche={} leadsCount={} limitPerSource={}",
                        niche, hotmartLeads.size(), limitPerSource);
                return hotmartLeads;
            }
            log.warn("mois_hotmart_leads_fallback_activated niche={} reason=no_hotmart_leads_resolved limitPerSource={}",
                    niche, limitPerSource);
        }
        List<SourceLead> fallback = new ArrayList<>();
        for (int sourceRank = 1; sourceRank <= limitPerSource; sourceRank++) {
            fallback.add(new SourceLead(buildCollectionSourceUrl(source, niche, sourceRank), null, null, null, null));
        }
        return fallback;
    }

    private List<SourceLead> fetchHotmartProductLeads(String niche, int limitPerSource) {
        String marketplaceUrl = buildCollectionSourceUrl("HOTMART", niche, 1);
        String httpMethod = "GET";
        log.info("mois_hotmart_product_url_fetch_started niche={} method={} url={} limitPerSource={}",
                niche, httpMethod, marketplaceUrl, limitPerSource);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(marketplaceUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0 (compatible; MarketingHub-MOIS/1.0)")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                String redirectTarget = response.headers().firstValue("location").orElse("<missing-location-header>");
                log.warn("mois_hotmart_product_url_fetch_redirect niche={} method={} requestedUrl={} finalUrl={} statusCode={} location={}",
                        niche, httpMethod, marketplaceUrl, response.uri(), response.statusCode(), redirectTarget);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null) {
                log.warn("mois_hotmart_product_url_fetch_rejected niche={} method={} requestedUrl={} finalUrl={} statusCode={} hasBody={}",
                        niche, httpMethod, marketplaceUrl, response.uri(), response.statusCode(), response.body() != null);
                return List.of();
            }
            List<SourceLead> leads = parseHotmartMarketplaceLeads(response.body(), limitPerSource);
            log.info("mois_hotmart_product_url_fetch_finished niche={} method={} requestedUrl={} finalUrl={} statusCode={} parsedLeads={} responseLength={}",
                    niche, httpMethod, marketplaceUrl, response.uri(), response.statusCode(), leads.size(), response.body().length());
            return leads;
        } catch (Exception ex) {
            log.warn("mois_hotmart_product_url_fetch_failed niche={} reason={}", niche, ex.getMessage());
            return List.of();
        }
    }


    private List<SourceLead> parseHotmartMarketplaceLeads(String body, int limitPerSource) {
        List<SourceLead> leads = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        Pattern richCardPattern = Pattern.compile(
                "\"name\":\"([^\"]+)\".*?\"fullLink\":\"(https:\\\\/\\\\/www\\.hotmart\\.com\\\\/product\\\\/[^\"]+)\".*?\"description\":\"([^\"]*)\".*?\"producerName\":\"([^\"]*)\".*?\"image\":\"([^\"]*)\""
        );
        Matcher richCardMatcher = richCardPattern.matcher(body);
        while (richCardMatcher.find() && leads.size() < limitPerSource) {
            String title = decodeHotmartValue(richCardMatcher.group(1));
            String decodedUrl = decodeHotmartValue(richCardMatcher.group(2));
            String description = decodeHotmartValue(richCardMatcher.group(3));
            String producer = decodeHotmartValue(richCardMatcher.group(4));
            String imageUrl = decodeHotmartValue(richCardMatcher.group(5));
            if (seenUrls.add(decodedUrl)) {
                leads.add(new SourceLead(decodedUrl, title, description, producer, imageUrl));
            }
        }

        if (leads.size() >= limitPerSource) {
            return leads;
        }

        Pattern fallbackUrlPattern = Pattern.compile(
                "https:\\\\/\\\\/www\\\\.hotmart\\\\.com\\\\/product\\\\/[a-zA-Z0-9\\\\-_/]+|https://www\\.hotmart\\.com/product/[a-zA-Z0-9\\-_/]+",
                Pattern.CASE_INSENSITIVE
        );
        Matcher fallbackUrlMatcher = fallbackUrlPattern.matcher(body);
        while (fallbackUrlMatcher.find() && leads.size() < limitPerSource) {
            String url = decodeHotmartValue(fallbackUrlMatcher.group());
            if (seenUrls.add(url)) {
                leads.add(new SourceLead(url, null, null, null, null));
            }
        }

        return leads;
    }

    private String decodeHotmartValue(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\/", "/").replace("\\u0026", "&").trim();
    }
    private String firstNonBlank(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate;
    }

    private String buildCollectionSourceUrl(String source, String niche, int sourceRank) {
        String encodedNiche = URLEncoder.encode(niche == null ? "" : niche, StandardCharsets.UTF_8);
        if ("HOTMART".equals(source)) {
            return "https://www.hotmart.com/pt-br/marketplace?query=" + encodedNiche + "&page=" + sourceRank;
        }
        return "https://example.com/" + source.toLowerCase() + "/offer-" + sourceRank;
    }

    private SourceExecutionOutcome executeSourceCollection(String workspaceId, String source, int index) {
        boolean hasRateLimit = source.toUpperCase().contains("META");
        boolean unavailable = source.toUpperCase().contains("UNAVAILABLE");
        int attempts = unavailable ? (MAX_COLLECTION_RETRIES + 1) : (hasRateLimit ? 2 : 1);
        int retries = Math.max(0, attempts - 1);
        int failures = unavailable ? attempts : retries;
        int successes = unavailable ? 0 : 1;
        int rateLimitedEvents = hasRateLimit ? 1 : 0;
        long latencyMs = 160L + (Math.abs(source.hashCode()) % 450L) + (retries * 120L) + (index * 15L);
        String lastError = unavailable ? "source unavailable" : (hasRateLimit ? "rate limited (recovered)" : null);
        SourceOpsStats stats = collectionOpsByWorkspace
                .computeIfAbsent(workspaceId, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(source, SourceOpsStats::new);
        stats.register(attempts, successes, failures, retries, rateLimitedEvents, latencyMs, lastError);
        return new SourceExecutionOutcome(!unavailable, attempts, retries, latencyMs);
    }

    private List<MoisWorkspaceDtos.CollectedReferenceResponse> reRank(List<MoisWorkspaceDtos.CollectedReferenceResponse> items) {
        List<MoisWorkspaceDtos.CollectedReferenceResponse> ranked = new ArrayList<>();
        int position = 1;
        for (MoisWorkspaceDtos.CollectedReferenceResponse item : items.stream()
                .sorted(Comparator.comparingInt(MoisWorkspaceDtos.CollectedReferenceResponse::successScore).reversed()
                        .thenComparing(MoisWorkspaceDtos.CollectedReferenceResponse::collectedAt, Comparator.reverseOrder()))
                .toList()) {
            ranked.add(new MoisWorkspaceDtos.CollectedReferenceResponse(
                    item.referenceId(),
                    item.jobId(),
                    item.source(),
                    item.title(),
                    item.url(),
                    item.niche(),
                    item.status(),
                    item.favorite(),
                    item.importedReferenceId(),
                    item.successScore(),
                    item.successSignal(),
                    item.confidenceLevel(),
                    position++,
                    item.engagementRelative(),
                    item.recurrenceScore(),
                    item.evidenceScore(),
                    item.collectedAt(),
                    item.rawMetadata()
            ));
        }
        return ranked;
    }

    private Optional<MoisWorkspaceDtos.CollectedReferenceActionResponse> updateCollectedReference(
            String jobId,
            String referenceId,
            String action,
            Function<MoisWorkspaceDtos.CollectedReferenceResponse, MoisWorkspaceDtos.CollectedReferenceResponse> updater
    ) {
        return updateCollectedReference(jobId, referenceId, action, updater, null, List.of());
    }

    private Optional<MoisWorkspaceDtos.CollectedReferenceActionResponse> updateCollectedReference(
            String jobId,
            String referenceId,
            String action,
            Function<MoisWorkspaceDtos.CollectedReferenceResponse, MoisWorkspaceDtos.CollectedReferenceResponse> updater,
            String extractionId,
            List<String> generatedLibraryBlockIds
    ) {
        hydrateCollectionStateByJob(jobId);
        if (!collectionJobs.containsKey(jobId)) {
            return Optional.empty();
        }
        List<MoisWorkspaceDtos.CollectedReferenceResponse> items = new ArrayList<>(collectedReferencesByJob.getOrDefault(jobId, List.of()));
        for (int i = 0; i < items.size(); i++) {
            MoisWorkspaceDtos.CollectedReferenceResponse current = items.get(i);
            if (!current.referenceId().equals(referenceId)) {
                continue;
            }
            MoisWorkspaceDtos.CollectedReferenceResponse updated = updater.apply(current);
            items.set(i, updated);
            collectedReferencesByJob.put(jobId, reRank(items));
            persistCollectionState(jobId);
            return Optional.of(new MoisWorkspaceDtos.CollectedReferenceActionResponse(
                    jobId,
                    referenceId,
                    action,
                    updated.status(),
                    updated.importedReferenceId(),
                    extractionId,
                    generatedLibraryBlockIds,
                    Instant.now()
            ));
        }
        return Optional.empty();
    }

    private List<String> generateLibraryBlocksFromCollectedReference(
            String workspaceId,
            MoisWorkspaceDtos.CollectedReferenceResponse item
    ) {
        Instant now = Instant.now();
        String promiseBlockId = UUID.randomUUID().toString();
        String proofBlockId = UUID.randomUUID().toString();
        MoisWorkspaceDtos.LibraryBlockResponse promiseBlock = new MoisWorkspaceDtos.LibraryBlockResponse(
                promiseBlockId,
                workspaceId,
                "PROMISE",
                "Importado de " + item.source() + ": " + trimTo(item.title(), 90),
                List.of(item.niche(), "AUTO_COLLECTION"),
                Math.min(1.0, item.successScore() / 100.0),
                "AUTO_COLLECTED_REFERENCE_" + item.referenceId(),
                item.favorite(),
                now
        );
        MoisWorkspaceDtos.LibraryBlockResponse proofBlock = new MoisWorkspaceDtos.LibraryBlockResponse(
                proofBlockId,
                workspaceId,
                "PROOF",
                "Sinal " + item.successSignal() + " com confiança " + item.confidenceLevel(),
                List.of(item.niche(), "AUTO_COLLECTION"),
                Math.min(1.0, item.evidenceScore() / 100.0),
                "AUTO_COLLECTED_REFERENCE_" + item.referenceId(),
                item.favorite(),
                now.minusSeconds(1)
        );
        libraryBlocksById.put(promiseBlockId, promiseBlock);
        libraryBlocksById.put(proofBlockId, proofBlock);
        return List.of(promiseBlockId, proofBlockId);
    }

    private String lineageKey(String jobId, String referenceId) {
        return jobId + "::" + referenceId;
    }

    private SourceMetricSnapshot buildSourceMetricSnapshot(MoisWorkspaceDtos.CollectionJobResponse job, String source, int index) {
        double base = 40 + (Math.abs(source.hashCode()) % 50);
        double engagementRaw = clamp(base + index * 6.0);
        double recurrenceRaw = clamp(base - 8 + index * 5.0);
        Double evidenceRaw = (index % 2 == 0) ? null : clamp(base - 12 + index * 4.0);
        return new SourceMetricSnapshot(engagementRaw, recurrenceRaw, evidenceRaw);
    }

    private NormalizedSuccessSignal normalizeSuccessSignal(SourceMetricSnapshot metrics, int minSuccessScore) {
        double engagement = clamp(metrics.engagementRaw());
        double recurrence = clamp(metrics.recurrenceRaw());
        double evidence = metrics.evidenceRaw() == null ? ((engagement + recurrence) / 2.0) : clamp(metrics.evidenceRaw());
        int score = (int) Math.round((engagement * ENGAGEMENT_WEIGHT) + (recurrence * RECURRENCE_WEIGHT) + (evidence * EVIDENCE_WEIGHT));
        score = Math.max(minSuccessScore, Math.min(100, score));
        String successSignal = score >= 85 ? "HIGH" : (score >= 70 ? "MEDIUM" : "LOW");
        String confidenceLevel = score >= 85 ? "HIGH" : (score >= 65 ? "MEDIUM" : "LOW");
        return new NormalizedSuccessSignal(score, successSignal, confidenceLevel, engagement, recurrence, evidence);
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private List<MoisWorkspaceDtos.ReferenceResponse> listWorkspaceReferences(String workspaceId) {
        List<MoisWorkspaceDtos.ReferenceResponse> references = new ArrayList<>();
        for (MoisWorkspaceDtos.ReferenceResponse reference : referencesById.values()) {
            if (reference.workspaceId().equals(workspaceId)) {
                references.add(reference);
            }
        }
        return references;
    }

    private boolean hasAnyDraftContent(MoisWorkspaceDtos.UpsertExtractionDraftRequest request) {
        return isNotBlank(request.pain())
                || isNotBlank(request.result())
                || isNotBlank(request.mechanism())
                || isNotBlank(request.proof())
                || isNotBlank(request.offer())
                || (request.evidenceItems() != null && !request.evidenceItems().isEmpty());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private MoisWorkspaceDtos.LibraryBlockResponse getLibraryBlockOrThrow(String blockId) {
        MoisWorkspaceDtos.LibraryBlockResponse block = libraryBlocksById.get(blockId);
        if (block == null) {
            throw new IllegalArgumentException("library block not found");
        }
        return block;
    }

    private void seedLibraryIfNeeded(String workspaceId) {
        if (!libraryBlocksById.isEmpty()) {
            return;
        }
        String effectiveWorkspace = workspaceId == null || workspaceId.isBlank() ? "workspace-default" : workspaceId;
        Instant now = Instant.now();
        MoisWorkspaceDtos.LibraryBlockResponse promise = new MoisWorkspaceDtos.LibraryBlockResponse(
                UUID.randomUUID().toString(),
                effectiveWorkspace,
                "PROMISE",
                "Headline com resultado e prazo explícito.",
                List.of("nutricao-esportiva", "CURSO"),
                0.82,
                "MARKET_REFERENCE",
                false,
                now
        );
        MoisWorkspaceDtos.LibraryBlockResponse proof = new MoisWorkspaceDtos.LibraryBlockResponse(
                UUID.randomUUID().toString(),
                effectiveWorkspace,
                "PROOF",
                "Prova social com dados antes/depois auditáveis.",
                List.of("nutricao-esportiva", "MENTORIA"),
                0.77,
                "MARKET_REFERENCE",
                false,
                now.minusSeconds(300)
        );
        libraryBlocksById.put(promise.blockId(), promise);
        libraryBlocksById.put(proof.blockId(), proof);
    }

    private Optional<CrawledSource> collectSource(String seedUrl, String requestId, String correlationId) {
        try {
            URI parsed = URI.create(seedUrl);
            if (parsed.getScheme() == null || (!"http".equalsIgnoreCase(parsed.getScheme()) && !"https".equalsIgnoreCase(parsed.getScheme()))) {
                log.warn("mois_source_skipped_invalid_scheme requestId={} correlationId={} seedUrl={}", requestId, correlationId, seedUrl);
                return Optional.empty();
            }

            HttpRequest request = HttpRequest.newBuilder(parsed)
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "MarketingHub-MOIS/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400 || response.body() == null || response.body().isBlank()) {
                log.warn("mois_source_failed requestId={} correlationId={} seedUrl={} statusCode={}", requestId, correlationId, seedUrl, response.statusCode());
                return Optional.empty();
            }

            String canonicalUrl = canonicalizeUrl(response.uri().toString());
            String normalized = normalizeText(response.body());
            if (normalized.isBlank()) {
                log.warn("mois_source_empty_after_normalization requestId={} correlationId={} seedUrl={}", requestId, correlationId, seedUrl);
                return Optional.empty();
            }

            String title = extractTitleFromHtml(response.body());
            String host = Objects.requireNonNullElse(response.uri().getHost(), "unknown-source");
            String sourceKind = inferSourceKind(canonicalUrl);

            log.info("mois_source_collected requestId={} correlationId={} canonicalUrl={} sourceKind={} textSize={}",
                    requestId, correlationId, canonicalUrl, sourceKind, normalized.length());
            return Optional.of(new CrawledSource(seedUrl, canonicalUrl, title, host, sourceKind, normalized));
        } catch (Exception ex) {
            log.warn("mois_source_exception requestId={} correlationId={} seedUrl={} message={}", requestId, correlationId, seedUrl, ex.getMessage());
            return Optional.empty();
        }
    }

    private String inferSourceKind(String canonicalUrl) {
        String lower = canonicalUrl.toLowerCase();
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
            return "VIDEO";
        }
        if (lower.contains("instagram.com") || lower.contains("facebook.com") || lower.contains("tiktok.com")) {
            return "SOCIAL";
        }
        if (lower.contains("blog") || lower.contains("article")) {
            return "ARTICLE";
        }
        return "LANDING_PAGE";
    }

    private String inferPrimaryOfferType(CrawledSource source) {
        String text = source.normalizedText().toLowerCase();
        if (text.contains("consultoria") || text.contains("mentoria")) {
            return "MENTORSHIP";
        }
        if (text.contains("curso") || text.contains("ebook") || text.contains("módulo")) {
            return "DIGITAL_PRODUCT";
        }
        if (source.sourceKind().equals("ARTICLE")) {
            return "CONTENT";
        }
        return "DIGITAL_PRODUCT";
    }

    private OfferSignals extractSignals(String normalizedText, DiscoveryRequest request) {
        String lower = normalizedText.toLowerCase();

        String promise = extractFirstSentenceContaining(lower, normalizedText,
                List.of("resultado", "transform", "alcance", "ganhe", "aprenda", "elimine", "reduza"))
                .orElse(Objects.requireNonNullElse(request.painOrOutcomeFocus(), "Promessa não identificada"));

        String proof = extractFirstSentenceContaining(lower, normalizedText,
                List.of("depoimento", "caso", "prova", "%", "avalia", "clientes", "alunos"))
                .orElse("Sem prova explícita no recorte capturado");

        String mechanism = extractFirstSentenceContaining(lower, normalizedText,
                List.of("método", "passo", "framework", "sistema", "protocolo", "estratégia"))
                .orElse("Mecanismo não declarado claramente");

        String funnel = extractFirstSentenceContaining(lower, normalizedText,
                List.of("cadastre", "inscreva", "checkout", "compre", "garanta", "clique"))
                .orElse("Padrão de funil não explícito no recorte");

        List<String> prices = extractPrices(normalizedText);
        String mainPrice = prices.isEmpty() ? "Preço não identificado" : prices.getFirst();

        List<String> deliverables = extractKeywords(lower,
                Map.of("Aulas gravadas", List.of("aulas", "módulos"),
                        "Comunidade", List.of("comunidade", "grupo"),
                        "Mentoria", List.of("mentoria", "ao vivo"),
                        "Material de apoio", List.of("planilha", "checklist", "material")));

        double confidence = estimateConfidence(promise, proof, mechanism, mainPrice);
        return new OfferSignals(
                trimTo(promise, 180),
                trimTo(proof, 180),
                trimTo(mechanism, 180),
                trimTo(funnel, 180),
                mainPrice,
                confidence,
                deliverables,
                prices);
    }

    private double estimateConfidence(String promise, String proof, String mechanism, String mainPrice) {
        double score = 0.35;
        if (!promise.contains("não identificada")) score += 0.2;
        if (!proof.contains("Sem prova")) score += 0.15;
        if (!mechanism.contains("não declarado")) score += 0.15;
        if (!mainPrice.contains("não identificado")) score += 0.15;
        return Math.min(0.95, score);
    }

    private List<String> buildCollectionSeeds(DiscoveryRequest request) {
        List<String> seeds = new ArrayList<>(request.seedUrls());
        if (!seeds.isEmpty()) {
            return seeds;
        }
        for (String query : request.seedQueries()) {
            if (query == null || query.isBlank()) {
                continue;
            }
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            seeds.add("https://duckduckgo.com/?q=" + encoded);
        }
        return seeds;
    }

    private String canonicalizeUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase();
            String host = Objects.requireNonNullElse(uri.getHost(), "unknown-source").toLowerCase();
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
            return scheme + "://" + host + path;
        } catch (Exception ex) {
            return url;
        }
    }

    private String normalizeText(String html) {
        String noScripts = html.replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ");
        String text = noScripts.replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return trimTo(text, 2500);
    }

    private String extractTitleFromHtml(String html) {
        Matcher matcher = Pattern.compile("(?is)<title>(.*?)</title>").matcher(html);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", " ").trim();
        }
        return "";
    }

    private List<String> extractPrices(String text) {
        List<String> prices = new ArrayList<>();
        Matcher matcher = PRICE_PATTERN.matcher(text);
        while (matcher.find() && prices.size() < 3) {
            prices.add(matcher.group(1).replaceAll("\\s+", " "));
        }
        return prices;
    }

    private List<String> extractKeywords(String lowerText, Map<String, List<String>> dictionary) {
        List<String> found = new ArrayList<>();
        dictionary.forEach((label, words) -> {
            boolean matches = words.stream().anyMatch(lowerText::contains);
            if (matches) {
                found.add(label);
            }
        });
        return found;
    }

    private Optional<String> extractFirstSentenceContaining(String lowerText, String originalText, List<String> tokens) {
        String[] sentences = originalText.split("(?<=[.!?])\\s+");
        for (String sentence : sentences) {
            String lowerSentence = sentence.toLowerCase();
            if (tokens.stream().anyMatch(lowerSentence::contains)) {
                return Optional.of(sentence.trim());
            }
        }
        if (tokens.stream().anyMatch(lowerText::contains)) {
            return Optional.of(trimTo(originalText, 140));
        }
        return Optional.empty();
    }

    private String contentSignature(String normalizedText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(trimTo(normalizedText, 800).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            return Integer.toHexString(normalizedText.hashCode());
        }
    }

    private String trimTo(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private void hydrateCollectionState(String workspaceId, String status) {
        // persistence gateway removed; state remains in-memory only
    }

    private void hydrateCollectionStateByJob(String jobId) {
        // persistence gateway removed; state remains in-memory only
    }

    private void persistCollectionState(String jobId) {
        // persistence gateway removed; state remains in-memory only
    }

    private void loadCollectionRolloutFromEnv() {
        String enabled = System.getenv().getOrDefault("MOIS_COLLECTION_ROLLOUT_ENABLED", "true");
        this.collectionRolloutEnabled = !"false".equalsIgnoreCase(enabled);
        String allowed = System.getenv().getOrDefault("MOIS_COLLECTION_ROLLOUT_ALLOWED_WORKSPACES", "");
        rolloutAllowedWorkspaces.clear();
        if (!allowed.isBlank()) {
            for (String workspace : allowed.split(",")) {
                String normalized = workspace.trim();
                if (!normalized.isBlank()) {
                    rolloutAllowedWorkspaces.add(normalized);
                }
            }
        }
    }

    void configureCollectionRollout(boolean enabled, List<String> allowedWorkspaces) {
        this.collectionRolloutEnabled = enabled;
        this.rolloutAllowedWorkspaces.clear();
        if (allowedWorkspaces != null) {
            this.rolloutAllowedWorkspaces.addAll(allowedWorkspaces.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList());
        }
    }

    private boolean isCollectionRolloutAllowed(String workspaceId) {
        if (!collectionRolloutEnabled) {
            return false;
        }
        if (rolloutAllowedWorkspaces.isEmpty()) {
            return true;
        }
        return workspaceId != null && rolloutAllowedWorkspaces.contains(workspaceId);
    }

    private <T> List<T> defaultList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private Map<String, Object> defaultMap(Map<String, Object> values) {
        return values == null ? new HashMap<>() : values;
    }

    private record CrawledSource(
            String sourceUrl,
            String canonicalUrl,
            String title,
            String host,
            String sourceKind,
            String normalizedText
    ) {
    }

    private record OfferSignals(
            String promise,
            String proof,
            String mechanism,
            String funnelPattern,
            String mainPrice,
            double confidence,
            List<String> deliverables,
            List<String> pricePoints
    ) {
    }

    private record SourceMetricSnapshot(
            double engagementRaw,
            double recurrenceRaw,
            Double evidenceRaw
    ) {
    }

    private record NormalizedSuccessSignal(
            int successScore,
            String successSignal,
            String confidenceLevel,
            double engagementRelative,
            double recurrenceScore,
            double evidenceScore
    ) {
    }

    private record SourceLead(
            String url,
            String title,
            String hotmartDescription,
            String hotmartProducer,
            String hotmartImageUrl
    ) {
    }

    private record SourceExecutionOutcome(
            boolean success,
            int attempts,
            int retries,
            long latencyMs
    ) {
    }

    private record CollectionExecutionResult(
            List<MoisWorkspaceDtos.CollectedReferenceResponse> references,
            boolean hasFailure,
            CollectionJobRuntimeStats runtimeStats
    ) {
    }

    private record CollectionJobRuntimeStats(
            String jobId,
            int retries,
            long latencyMs,
            Instant finishedAt
    ) {
    }

    private static final class SourceOpsStats {
        private final String source;
        private int attempts;
        private int successes;
        private int failures;
        private int retries;
        private int rateLimitedEvents;
        private long totalLatencyMs;
        private String lastError;
        private Instant lastAttemptAt;

        private SourceOpsStats(String source) {
            this.source = source;
        }

        synchronized void register(
                int attempts,
                int successes,
                int failures,
                int retries,
                int rateLimitedEvents,
                long latencyMs,
                String lastError
        ) {
            this.attempts += attempts;
            this.successes += successes;
            this.failures += failures;
            this.retries += retries;
            this.rateLimitedEvents += rateLimitedEvents;
            this.totalLatencyMs += latencyMs;
            this.lastError = lastError;
            this.lastAttemptAt = Instant.now();
        }

        String source() {
            return source;
        }

        synchronized int attempts() {
            return attempts;
        }

        synchronized int successes() {
            return successes;
        }

        synchronized int failures() {
            return failures;
        }

        synchronized int retries() {
            return retries;
        }

        synchronized int rateLimitedEvents() {
            return rateLimitedEvents;
        }

        synchronized long averageLatencyMs() {
            return attempts == 0 ? 0 : totalLatencyMs / attempts;
        }

        synchronized String lastError() {
            return lastError;
        }

        synchronized Instant lastAttemptAt() {
            return lastAttemptAt;
        }

        static SourceOpsStats copyOf(MoisWorkspaceDtos.CollectionSourceOpsSummaryResponse response) {
            SourceOpsStats stats = new SourceOpsStats(response.source());
            stats.attempts = response.attempts();
            stats.successes = response.successes();
            stats.failures = response.failures();
            stats.retries = response.retries();
            stats.rateLimitedEvents = response.rateLimitedEvents();
            stats.totalLatencyMs = response.averageLatencyMs() * Math.max(1, response.attempts());
            stats.lastError = response.lastError();
            stats.lastAttemptAt = response.lastAttemptAt();
            return stats;
        }

        synchronized MoisWorkspaceDtos.CollectionSourceOpsSummaryResponse toResponse() {
            return new MoisWorkspaceDtos.CollectionSourceOpsSummaryResponse(
                    source,
                    attempts,
                    successes,
                    failures,
                    retries,
                    rateLimitedEvents,
                    averageLatencyMs(),
                    lastError,
                    lastAttemptAt
            );
        }
    }
}
