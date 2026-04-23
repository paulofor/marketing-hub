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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class MoisDomainService {

    private final Map<String, DiscoveryRequest> requests = new ConcurrentHashMap<>();
    private final Map<String, SourceSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<String, OfferCard> offers = new ConcurrentHashMap<>();

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

        List<String> seedUrls = request.seedUrls().isEmpty() ? List.of("https://example.com/oferta") : request.seedUrls();
        for (String seedUrl : seedUrls) {
            String sourceId = "mois-source-" + UUID.randomUUID();
            SourceSnapshot snapshot = new SourceSnapshot(
                    sourceId,
                    request.requestId(),
                    seedUrl,
                    extractTitle(seedUrl),
                    "landing-page",
                    Instant.now(),
                    "Captured from seed URL for downstream extraction.");
            snapshots.put(sourceId, snapshot);

            String offerId = "mois-offer-" + UUID.randomUUID();
            OfferCard offer = new OfferCard(
                    offerId,
                    request.requestId(),
                    request.nicheName(),
                    "Oferta " + extractTitle(seedUrl),
                    extractHost(seedUrl),
                    Objects.requireNonNullElse(request.painOrOutcomeFocus(), "Promessa ainda em refinamento"),
                    "DIGITAL_PRODUCT",
                    "R$97",
                    0.65,
                    List.of("Acesso imediato", "Material principal"),
                    List.of("R$97", "R$197"),
                    "Prova social declarada no topo da página",
                    "Mecanismo baseado em aplicação prática guiada",
                    "Captura de lead com CTA direto para checkout",
                    Instant.now());
            offers.put(offerId, offer);
        }

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
                DiscoveryStatus.COLLECTED,
                request.createdAt(),
                Instant.now()));

        return Optional.of(new MoisDiscoveryDtos.AsyncAcceptedResponse("ACCEPTED", "mois-run-" + requestId));
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
        String prefix = "mois-report-";
        if (!reportId.startsWith(prefix)) {
            return Optional.empty();
        }
        String requestId = reportId.substring(prefix.length());
        DiscoveryRequest request = requests.get(requestId);
        if (request == null || request.status() != DiscoveryStatus.COLLECTED) {
            return Optional.empty();
        }

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

        return Optional.of(new MoisInsightDtos.InsightReportResponse(
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
                buildRecommendedActions(repeatedPromises, pricingPatterns, mechanismPatterns)));
    }

    public Optional<MoisArtifactDtos.ArtifactEnvelopeResponse> getArtifact(String artifactId) {
        DiscoveryRequest request = requests.get(artifactId);
        if (request != null) {
            return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                    request.requestId(),
                    "mois.marketOfferDiscoveryRequest.v1",
                    "v1",
                    request.status().name(),
                    "MOIS",
                    request.createdAt(),
                    request.updatedAt(),
                    Map.of("requestId", request.requestId(), "parentArtifactIds", List.of()),
                    Map.of("nicheName", request.nicheName(), "marketTheme", request.marketTheme()),
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
                    "MOIS",
                    snapshot.capturedAt(),
                    snapshot.capturedAt(),
                    Map.of("requestId", snapshot.requestId(), "parentArtifactIds", List.of(snapshot.requestId())),
                    Map.of("sourceKind", snapshot.sourceKind()),
                    Map.of(
                            "sourceUrl", snapshot.sourceUrl(),
                            "sourceTitle", snapshot.sourceTitle(),
                            "sourceKind", snapshot.sourceKind(),
                            "capturedAt", snapshot.capturedAt(),
                            "rawExcerpt", snapshot.rawExcerpt())));
        }

        OfferCard offer = offers.get(artifactId);
        if (offer != null) {
            return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                    offer.artifactId(),
                    "mois.marketOfferCard.v1",
                    "v1",
                    "COLLECTED",
                    "MOIS",
                    offer.createdAt(),
                    offer.createdAt(),
                    Map.of("requestId", offer.requestId(), "parentArtifactIds", collectParentArtifactsForOffer(offer.requestId())),
                    Map.of("offerName", offer.offerName(), "sellerOrBrand", offer.sellerOrBrand()),
                    Map.of(
                            "corePromise", offer.corePromise(),
                            "primaryOfferType", offer.primaryOfferType(),
                            "mainPrice", offer.mainPrice(),
                            "deliverables", offer.deliverables(),
                            "pricePoints", offer.pricePoints(),
                            "proofSummary", offer.proofSummary(),
                            "mechanismClaimSummary", offer.mechanismClaimSummary(),
                            "funnelPatternSummary", offer.funnelPatternSummary())));
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

    private List<String> collectParentArtifactsForOffer(String requestId) {
        return snapshots.values().stream()
                .filter(item -> item.requestId().equals(requestId))
                .map(SourceSnapshot::artifactId)
                .toList();
    }

    private String extractTitle(String url) {
        return extractHost(url).replace("www.", "");
    }

    private String extractHost(String url) {
        try {
            URI parsed = URI.create(url);
            return Objects.requireNonNullElse(parsed.getHost(), "unknown-source");
        } catch (Exception ignored) {
            return "unknown-source";
        }
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private Map<String, Object> defaultMap(Map<String, Object> values) {
        return values == null ? new HashMap<>() : values;
    }
}
