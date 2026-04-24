package com.marketinghub.mois.service;

import com.marketinghub.mois.domain.MoisDomainModels.DiscoveryRequest;
import com.marketinghub.mois.domain.MoisDomainModels.DiscoveryStatus;
import com.marketinghub.mois.domain.MoisDomainModels.OfferCard;
import com.marketinghub.mois.domain.MoisDomainModels.SourceSnapshot;
import com.marketinghub.mois.dto.MoisArtifactDtos;
import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.dto.MoisInsightDtos;
import com.marketinghub.mois.dto.MoisOfferDtos;
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
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MoisDomainService {

    private static final Logger log = LoggerFactory.getLogger(MoisDomainService.class);
    private static final Pattern PRICE_PATTERN = Pattern.compile("(R\\$\\s?\\d{2,5}(?:[\\.,]\\d{2})?|\\$\\s?\\d{2,5}(?:[\\.,]\\d{2})?)");

    private final Map<String, DiscoveryRequest> requests = new ConcurrentHashMap<>();
    private final Map<String, SourceSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<String, OfferCard> offers = new ConcurrentHashMap<>();
    private final Map<String, MoisInsightDtos.InsightReportResponse> reports = new ConcurrentHashMap<>();
    private final Map<String, String> runOperations = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static final String MODULE_NAME = "MOIS";
    private static final String CREATED_BY = "mois-system";

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
                buildSaturationNotes(repeatedPromises, pricingPatterns, offerCount),
                buildGapOpportunities(requestOffers, repeatedPromises, pricingPatterns),
                buildDifferentiationSignals(requestOffers, repeatedPromises, mechanismPatterns),
                buildRecommendedActions(repeatedPromises, pricingPatterns, mechanismPatterns));
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
            content.put("saturationNotes", report.saturationNotes());
            content.put("gapOpportunities", report.gapOpportunities());
            content.put("differentiationSignals", report.differentiationSignals());
            content.put("recommendedNextActions", report.recommendedNextActions());
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
                .collect(java.util.stream.Collectors.groupingBy(Function.identity(), java.util.stream.Collectors.counting()))
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
            List<MoisInsightDtos.InsightReportPatternResponse> pricingPatterns
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
                        0.72));
            }
        });

        if (pricingPatterns.size() <= 1) {
            gaps.add(new MoisInsightDtos.GapOpportunityResponse(
                    "PRICING_MODEL_VARIETY",
                    "Baixa variedade aparente de modelo de preço entre as ofertas.",
                    "Adicionar modelos de ancoragem, parcelamento ou ticket escalonado pode ampliar conversão por perfil.",
                    requestOffers.stream().map(OfferCard::artifactId).toList(),
                    "MEDIUM",
                    0.66));
        }
        return gaps;
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

    private List<String> defaultList(List<String> values) {
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
}
