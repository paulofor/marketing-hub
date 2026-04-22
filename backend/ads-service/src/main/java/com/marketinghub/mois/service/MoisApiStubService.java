package com.marketinghub.mois.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.*;
import com.marketinghub.mois.dto.MoisArtifactDtos;
import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.dto.MoisInsightDtos;
import com.marketinghub.mois.dto.MoisOfferDtos;
import com.marketinghub.mois.repository.MoisDiscoveryRequestRepository;
import com.marketinghub.mois.repository.MoisOfferCardRepository;
import com.marketinghub.mois.repository.MoisOfferFunnelPatternRepository;
import com.marketinghub.mois.repository.MoisOfferMechanismClaimRepository;
import com.marketinghub.mois.repository.MoisOfferProofSignalRepository;
import com.marketinghub.mois.repository.MoisOfferPromiseSignalRepository;
import com.marketinghub.mois.repository.MoisSourceSnapshotRepository;
import com.marketinghub.mois.service.MoisResearchGateway.MoisDiscoveredSource;
import com.marketinghub.mois.service.MoisResearchGateway.MoisResearchResult;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MoisApiStubService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final MoisDiscoveryRequestRepository discoveryRequestRepository;
    private final MoisSourceSnapshotRepository sourceSnapshotRepository;
    private final MoisOfferCardRepository offerCardRepository;
    private final MoisOfferPromiseSignalRepository promiseSignalRepository;
    private final MoisOfferProofSignalRepository proofSignalRepository;
    private final MoisOfferMechanismClaimRepository mechanismClaimRepository;
    private final MoisOfferFunnelPatternRepository funnelPatternRepository;
    private final ObjectMapper objectMapper;
    private final MoisResearchGateway researchGateway;

    public MoisApiStubService(
            MoisDiscoveryRequestRepository discoveryRequestRepository,
            MoisSourceSnapshotRepository sourceSnapshotRepository,
            MoisOfferCardRepository offerCardRepository,
            MoisOfferPromiseSignalRepository promiseSignalRepository,
            MoisOfferProofSignalRepository proofSignalRepository,
            MoisOfferMechanismClaimRepository mechanismClaimRepository,
            MoisOfferFunnelPatternRepository funnelPatternRepository,
            ObjectMapper objectMapper,
            MoisResearchGateway researchGateway
    ) {
        this.discoveryRequestRepository = discoveryRequestRepository;
        this.sourceSnapshotRepository = sourceSnapshotRepository;
        this.offerCardRepository = offerCardRepository;
        this.promiseSignalRepository = promiseSignalRepository;
        this.proofSignalRepository = proofSignalRepository;
        this.mechanismClaimRepository = mechanismClaimRepository;
        this.funnelPatternRepository = funnelPatternRepository;
        this.objectMapper = objectMapper;
        this.researchGateway = researchGateway;
    }

    @Transactional
    public MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse createDiscoveryRequest(
            MoisDiscoveryDtos.CreateDiscoveryRequest request
    ) {
        String requestId = "mois-req-" + UUID.randomUUID();
        MoisDiscoveryRequest entity = MoisDiscoveryRequest.builder()
                .requestId(requestId)
                .status(MoisDiscoveryRequestStatus.DRAFT)
                .nicheName(request.nicheName())
                .marketTheme(request.marketTheme())
                .painOrOutcomeFocus(request.painOrOutcomeFocus())
                .seedQueriesJson(toJson(defaultList(request.seedQueries())))
                .seedUrlsJson(toJson(defaultList(request.seedUrls())))
                .channelsJson(toJson(defaultList(request.channels())))
                .country(request.country())
                .language(request.language())
                .discoveryPolicyJson(toJson(defaultMap(request.discoveryPolicy())))
                .build();

        MoisDiscoveryRequest savedRequest = discoveryRequestRepository.save(entity);
        return new MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse(savedRequest.getRequestId(), "ACCEPTED");
    }

    @Transactional(readOnly = true)
    public MoisDiscoveryDtos.DiscoveryRequestListResponse listDiscoveryRequests(
            String status,
            String nicheName,
            String marketTheme
    ) {
        List<MoisDiscoveryRequest> requests = discoveryRequestRepository.findAll((Specification<MoisDiscoveryRequest>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), parseStatus(status)));
            }
            if (nicheName != null && !nicheName.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("nicheName")), nicheName.toLowerCase()));
            }
            if (marketTheme != null && !marketTheme.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("marketTheme")), marketTheme.toLowerCase()));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        });

        List<MoisDiscoveryDtos.DiscoveryRequestSummaryResponse> items = requests.stream()
                .map(this::toSummaryResponse)
                .toList();
        return new MoisDiscoveryDtos.DiscoveryRequestListResponse(items);
    }

    @Transactional(readOnly = true)
    public Optional<MoisDiscoveryDtos.DiscoveryRequestDetailResponse> getDiscoveryRequest(String requestId) {
        return discoveryRequestRepository.findByRequestId(requestId)
                .map(request -> new MoisDiscoveryDtos.DiscoveryRequestDetailResponse(
                        request.getRequestId(),
                        request.getNicheName(),
                        request.getMarketTheme(),
                        request.getPainOrOutcomeFocus(),
                        request.getStatus().name(),
                        request.getCreatedAt(),
                        buildRequestArtifacts(request.getRequestId())
                ));
    }

    @Transactional
    public MoisDiscoveryDtos.AsyncAcceptedResponse runDiscoveryRequest(String requestId) {
        MoisDiscoveryRequest request = discoveryRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "discovery request not found"));
        List<String> seedUrls = readStringList(request.getSeedUrlsJson());
        List<String> seedQueries = readStringList(request.getSeedQueriesJson());
        MoisResearchResult discovery = researchGateway.discoverSources(request, seedUrls, seedQueries);

        int collectedCount = persistDiscoveredSources(request, discovery);
        request.setStatus(collectedCount > 0 ? MoisDiscoveryRequestStatus.COLLECTED : MoisDiscoveryRequestStatus.FAILED);

        return new MoisDiscoveryDtos.AsyncAcceptedResponse("ACCEPTED", "mois-run-" + requestId);
    }

    @Transactional(readOnly = true)
    public MoisOfferDtos.OfferCardListResponse listOffers(String requestId, String nicheName, String sellerOrBrand) {
        List<MoisOfferCard> offers = offerCardRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (requestId != null && !requestId.isBlank()) {
                predicates.add(cb.equal(root.get("request").get("requestId"), requestId));
            }
            if (nicheName != null && !nicheName.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("request").get("nicheName")), nicheName.toLowerCase()));
            }
            if (sellerOrBrand != null && !sellerOrBrand.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("sellerOrBrand")), "%" + sellerOrBrand.toLowerCase() + "%"));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        });

        return new MoisOfferDtos.OfferCardListResponse(offers.stream().map(this::toOfferSummary).toList());
    }

    @Transactional(readOnly = true)
    public Optional<MoisOfferDtos.OfferCardResponse> getOffer(String offerId) {
        return offerCardRepository.findByArtifactId(offerId)
                .map(offer -> new MoisOfferDtos.OfferCardResponse(
                        offer.getArtifactId(),
                        offer.getRequest().getRequestId(),
                        offer.getRequest().getNicheName(),
                        offer.getOfferName(),
                        offer.getSellerOrBrand(),
                        offer.getCorePromise(),
                        offer.getPrimaryOfferType(),
                        offer.getMainPrice(),
                        offer.getConfidence(),
                        readStringList(offer.getDeliverablesJson()),
                        readStringList(offer.getPricePointsJson()),
                        offer.getProofSummary(),
                        offer.getMechanismClaimSummary(),
                        offer.getFunnelPatternSummary(),
                        buildOfferSourceArtifacts(offer)
                ));
    }

    @Transactional(readOnly = true)
    public MoisInsightDtos.InsightReportListResponse listInsightReports(String requestId, String nicheName) {
        List<MoisDiscoveryDtos.DiscoveryRequestSummaryResponse> requests = listDiscoveryRequests("COLLECTED", nicheName, null).items();
        List<MoisInsightDtos.InsightReportSummaryResponse> items = requests.stream()
                .filter(request -> requestId == null || requestId.isBlank() || request.requestId().equals(requestId))
                .map(request -> new MoisInsightDtos.InsightReportSummaryResponse(
                        "mois-report-" + request.requestId(),
                        request.requestId(),
                        request.nicheName(),
                        request.marketTheme(),
                        "DRAFT",
                        request.createdAt()
                ))
                .toList();
        return new MoisInsightDtos.InsightReportListResponse(items);
    }

    @Transactional(readOnly = true)
    public Optional<MoisInsightDtos.InsightReportResponse> getInsightReport(String reportId) {
        String prefix = "mois-report-";
        if (!reportId.startsWith(prefix)) {
            return Optional.empty();
        }
        String requestId = reportId.substring(prefix.length());
        Optional<MoisDiscoveryRequest> request = discoveryRequestRepository.findByRequestId(requestId);
        if (request.isEmpty()) {
            return Optional.empty();
        }

        List<MoisOfferCard> offers = offerCardRepository.findByRequest_RequestIdOrderByCreatedAtDesc(requestId);
        return Optional.of(new MoisInsightDtos.InsightReportResponse(
                reportId,
                requestId,
                request.get().getNicheName(),
                request.get().getMarketTheme(),
                "DRAFT",
                request.get().getCreatedAt(),
                offers.stream().map(MoisOfferCard::getCorePromise).distinct().toList(),
                offers.stream().map(MoisOfferCard::getProofSummary).filter(s -> s != null && !s.isBlank()).distinct().toList(),
                offers.stream().map(MoisOfferCard::getMainPrice).filter(s -> s != null && !s.isBlank()).distinct().toList(),
                offers.stream().map(MoisOfferCard::getFunnelPatternSummary).filter(s -> s != null && !s.isBlank()).distinct().toList(),
                List.of("Mapear lacunas de prova para próxima coleta"),
                List.of("Executar Sprint 5 para consolidar sinais em relatório acionável")
        ));
    }

    @Transactional(readOnly = true)
    public Optional<MoisArtifactDtos.ArtifactEnvelopeResponse> getArtifact(String artifactId) {
        Optional<MoisDiscoveryRequest> requestArtifact = discoveryRequestRepository.findByRequestId(artifactId);
        if (requestArtifact.isPresent()) {
            MoisDiscoveryRequest request = requestArtifact.get();
            return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                    request.getRequestId(),
                    "mois.marketOfferDiscoveryRequest.v1",
                    "v1",
                    request.getStatus().name(),
                    "MOIS",
                    request.getCreatedAt(),
                    request.getUpdatedAt(),
                    Map.of("requestId", request.getRequestId(), "parentArtifactIds", List.of()),
                    Map.of("nicheName", request.getNicheName(), "marketTheme", request.getMarketTheme()),
                    Map.ofEntries(
                            Map.entry("nicheName", request.getNicheName()),
                            Map.entry("marketTheme", request.getMarketTheme()),
                            Map.entry("painOrOutcomeFocus", request.getPainOrOutcomeFocus()),
                            Map.entry("seedQueries", readStringList(request.getSeedQueriesJson())),
                            Map.entry("seedUrls", readStringList(request.getSeedUrlsJson())),
                            Map.entry("channels", readStringList(request.getChannelsJson())),
                            Map.entry("country", request.getCountry()),
                            Map.entry("language", request.getLanguage()),
                            Map.entry("discoveryPolicy", readMap(request.getDiscoveryPolicyJson()))
                    )
            ));
        }

        Optional<MoisSourceSnapshot> snapshotArtifact = sourceSnapshotRepository.findByArtifactId(artifactId);
        if (snapshotArtifact.isPresent()) {
            MoisSourceSnapshot snapshot = snapshotArtifact.get();
            return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                    snapshot.getArtifactId(),
                    "mois.marketOfferSourceSnapshot.v1",
                    "v1",
                    snapshot.getStatus().name(),
                    "MOIS",
                    snapshot.getCreatedAt(),
                    snapshot.getUpdatedAt(),
                    Map.of("requestId", snapshot.getRequest().getRequestId(), "parentArtifactIds", List.of(snapshot.getRequest().getRequestId())),
                    Map.of("sourceKind", defaultText(snapshot.getSourceKind(), "unknown")),
                    Map.of(
                            "sourceUrl", snapshot.getSourceUrl(),
                            "sourceTitle", snapshot.getSourceTitle(),
                            "sourceKind", snapshot.getSourceKind(),
                            "capturedAt", snapshot.getCapturedAt(),
                            "httpStatus", snapshot.getHttpStatus(),
                            "contentHash", snapshot.getContentHash(),
                            "rawExcerpt", snapshot.getRawExcerpt(),
                            "normalizedTextRef", snapshot.getNormalizedTextRef(),
                            "captureNotes", snapshot.getCaptureNotes()
                    )
            ));
        }

        Optional<MoisOfferPromiseSignal> promiseArtifact = promiseSignalRepository.findByArtifactId(artifactId);
        if (promiseArtifact.isPresent()) {
            MoisOfferPromiseSignal promise = promiseArtifact.get();
            return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                    promise.getArtifactId(),
                    "mois.marketOfferPromiseSignal.v1",
                    "v1",
                    promise.getStatus().name(),
                    "MOIS",
                    promise.getCreatedAt(),
                    promise.getUpdatedAt(),
                    Map.of("requestId", promise.getRequest().getRequestId(),
                            "parentArtifactIds", buildOfferSignalParentIds(promise.getRequest().getRequestId(), promise.getOfferCard().getArtifactId(), promise.getSourceSnapshot())),
                    Map.of("signalType", "promise"),
                    Map.of(
                            "promiseText", promise.getPromiseText(),
                            "promiseType", promise.getPromiseType(),
                            "intensity", promise.getIntensity(),
                            "timeframeClaim", promise.getTimeframeClaim(),
                            "targetOutcome", promise.getTargetOutcome(),
                            "confidence", promise.getConfidence()
                    )
            ));
        }

        Optional<MoisOfferProofSignal> proofArtifact = proofSignalRepository.findByArtifactId(artifactId);
        if (proofArtifact.isPresent()) {
            MoisOfferProofSignal proof = proofArtifact.get();
            return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                    proof.getArtifactId(),
                    "mois.marketOfferProofSignal.v1",
                    "v1",
                    proof.getStatus().name(),
                    "MOIS",
                    proof.getCreatedAt(),
                    proof.getUpdatedAt(),
                    Map.of("requestId", proof.getRequest().getRequestId(),
                            "parentArtifactIds", buildOfferSignalParentIds(proof.getRequest().getRequestId(), proof.getOfferCard().getArtifactId(), proof.getSourceSnapshot())),
                    Map.of("signalType", "proof"),
                    Map.of(
                            "proofType", proof.getProofType(),
                            "proofText", proof.getProofText(),
                            "proofStrengthHypothesis", proof.getProofStrengthHypothesis(),
                            "proofLocation", proof.getProofLocation(),
                            "confidence", proof.getConfidence()
                    )
            ));
        }

        Optional<MoisOfferMechanismClaim> mechanismArtifact = mechanismClaimRepository.findByArtifactId(artifactId);
        if (mechanismArtifact.isPresent()) {
            MoisOfferMechanismClaim mechanism = mechanismArtifact.get();
            return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                    mechanism.getArtifactId(),
                    "mois.marketOfferMechanismClaim.v1",
                    "v1",
                    mechanism.getStatus().name(),
                    "MOIS",
                    mechanism.getCreatedAt(),
                    mechanism.getUpdatedAt(),
                    Map.of("requestId", mechanism.getRequest().getRequestId(),
                            "parentArtifactIds", buildOfferSignalParentIds(mechanism.getRequest().getRequestId(), mechanism.getOfferCard().getArtifactId(), mechanism.getSourceSnapshot())),
                    Map.of("signalType", "mechanism-claim"),
                    Map.of(
                            "claimText", mechanism.getClaimText(),
                            "claimCategory", mechanism.getClaimCategory(),
                            "claimSpecificity", mechanism.getClaimSpecificity(),
                            "claimRiskLevel", mechanism.getClaimRiskLevel(),
                            "confidence", mechanism.getConfidence()
                    )
            ));
        }

        Optional<MoisOfferFunnelPattern> funnelArtifact = funnelPatternRepository.findByArtifactId(artifactId);
        if (funnelArtifact.isPresent()) {
            MoisOfferFunnelPattern funnel = funnelArtifact.get();
            return Optional.of(new MoisArtifactDtos.ArtifactEnvelopeResponse(
                    funnel.getArtifactId(),
                    "mois.marketOfferFunnelPattern.v1",
                    "v1",
                    funnel.getStatus().name(),
                    "MOIS",
                    funnel.getCreatedAt(),
                    funnel.getUpdatedAt(),
                    Map.of("requestId", funnel.getRequest().getRequestId(),
                            "parentArtifactIds", buildOfferSignalParentIds(funnel.getRequest().getRequestId(), funnel.getOfferCard().getArtifactId(), funnel.getSourceSnapshot())),
                    Map.of("signalType", "funnel-pattern"),
                    Map.of(
                            "entryAssetType", funnel.getEntryAssetType(),
                            "leadCaptureFields", readStringList(funnel.getLeadCaptureFieldsJson()),
                            "ctaStyle", funnel.getCtaStyle(),
                            "nextStepHypothesis", funnel.getNextStepHypothesis(),
                            "deliveryFormat", funnel.getDeliveryFormat(),
                            "upsellVisible", funnel.getUpsellVisible(),
                            "retentionHint", funnel.getRetentionHint(),
                            "confidence", funnel.getConfidence()
                    )
            ));
        }

        return offerCardRepository.findByArtifactId(artifactId).map(offer -> new MoisArtifactDtos.ArtifactEnvelopeResponse(
                offer.getArtifactId(),
                "mois.marketOfferCard.v1",
                "v1",
                offer.getStatus().name(),
                "MOIS",
                offer.getCreatedAt(),
                offer.getUpdatedAt(),
                Map.of(
                        "requestId", offer.getRequest().getRequestId(),
                        "parentArtifactIds", offer.getSourceSnapshot() == null
                                ? List.of(offer.getRequest().getRequestId())
                                : List.of(offer.getRequest().getRequestId(), offer.getSourceSnapshot().getArtifactId())
                ),
                Map.of("channel", defaultText(offer.getChannel(), "unknown")),
                Map.ofEntries(
                        Map.entry("offerName", offer.getOfferName()),
                        Map.entry("sellerOrBrand", offer.getSellerOrBrand()),
                        Map.entry("channel", offer.getChannel()),
                        Map.entry("targetAudienceHypothesis", offer.getTargetAudienceHypothesis()),
                        Map.entry("corePromise", offer.getCorePromise()),
                        Map.entry("primaryOfferType", offer.getPrimaryOfferType()),
                        Map.entry("deliverables", readStringList(offer.getDeliverablesJson())),
                        Map.entry("pricePoints", readStringList(offer.getPricePointsJson())),
                        Map.entry("mainPrice", offer.getMainPrice()),
                        Map.entry("mechanismClaimSummary", offer.getMechanismClaimSummary()),
                        Map.entry("proofSummary", offer.getProofSummary()),
                        Map.entry("positioningSummary", offer.getPositioningSummary())
                )
        ));
    }

    private int persistDiscoveredSources(MoisDiscoveryRequest request, MoisResearchResult discovery) {
        int collected = 0;
        for (MoisDiscoveredSource source : discovery.sources()) {
            String normalizedText = defaultText(source.normalizedText(), "");
            boolean collectedSource = source.success() && !normalizedText.isBlank();
            MoisSourceSnapshot snapshot = MoisSourceSnapshot.builder()
                    .request(request)
                    .artifactId("mois-art-src-" + UUID.randomUUID())
                    .status(collectedSource ? MoisArtifactStatus.COLLECTED : MoisArtifactStatus.DRAFT)
                    .sourceUrl(source.sourceUrl())
                    .sourceTitle(defaultText(source.sourceTitle(), "Fonte sem título"))
                    .sourceKind(defaultText(source.sourceKind(), "landing-page"))
                    .capturedAt(Instant.now())
                    .httpStatus(source.httpStatus())
                    .contentHash(collectedSource ? Integer.toHexString(Objects.hash(normalizedText)) : null)
                    .rawExcerpt(limitText(normalizedText, 2000))
                    .normalizedTextRef(collectedSource ? "mois://snapshots/" + request.getRequestId() + "/" + UUID.randomUUID() : null)
                    .captureNotes(source.captureNotes())
                    .build();
            MoisSourceSnapshot savedSnapshot = sourceSnapshotRepository.save(snapshot);
            if (collectedSource) {
                collected++;
                MoisOfferExtractionResult extraction = extractSignalsFromSnapshot(request, savedSnapshot);
                MoisOfferCard offer = offerCardRepository.save(buildOfferFromSnapshot(request, savedSnapshot, extraction));
                persistDerivedArtifacts(request, savedSnapshot, offer, extraction);
            }
        }

        for (String operationalError : discovery.operationalErrors()) {
            sourceSnapshotRepository.save(MoisSourceSnapshot.builder()
                    .request(request)
                    .artifactId("mois-art-src-" + UUID.randomUUID())
                    .status(MoisArtifactStatus.DRAFT)
                    .sourceUrl("mois://operational-error")
                    .sourceTitle("Operational error")
                    .sourceKind("system")
                    .capturedAt(Instant.now())
                    .captureNotes(operationalError)
                    .build());
        }
        return collected;
    }

    private MoisOfferCard buildOfferFromSnapshot(
            MoisDiscoveryRequest request,
            MoisSourceSnapshot savedSnapshot,
            MoisOfferExtractionResult extraction
    ) {
        return MoisOfferCard.builder()
                .request(request)
                .sourceSnapshot(savedSnapshot)
                .artifactId("mois-offer-" + UUID.randomUUID())
                .status(MoisArtifactStatus.DRAFT)
                .offerName(defaultText(savedSnapshot.getSourceTitle(), "Oferta observada"))
                .sellerOrBrand("Market source")
                .channel(defaultText(savedSnapshot.getSourceKind(), "landing"))
                .targetAudienceHypothesis("Público buscando " + request.getMarketTheme())
                .corePromise(defaultText(extraction.promiseText(), defaultText(request.getPainOrOutcomeFocus(), "Melhoria prática percebida")))
                .primaryOfferType("unknown")
                .mainPrice(defaultText(extraction.mainPrice(), "não identificado"))
                .confidence(extraction.confidence())
                .deliverablesJson(toJson(List.of(defaultText(extraction.deliveryFormat(), "conteúdo digital"))))
                .pricePointsJson(toJson(extraction.pricePoints()))
                .proofSummary(defaultText(extraction.proofText(), "Prova não identificada no recorte coletado"))
                .mechanismClaimSummary(defaultText(extraction.claimText(), "Mecanismo alegado não identificado no recorte coletado"))
                .positioningSummary("Oferta observada em " + defaultText(savedSnapshot.getSourceKind(), "canal não identificado"))
                .funnelPatternSummary(defaultText(extraction.funnelSummary(), "padrão de funil básico"))
                .build();
    }

    private List<MoisDiscoveryDtos.ArtifactRefResponse> buildRequestArtifacts(String requestId) {
        List<MoisDiscoveryDtos.ArtifactRefResponse> refs = new ArrayList<>();
        refs.add(new MoisDiscoveryDtos.ArtifactRefResponse(requestId, "mois.marketOfferDiscoveryRequest.v1", "v1"));
        refs.addAll(sourceSnapshotRepository.findByRequest_RequestIdOrderByCreatedAtAsc(requestId)
                .stream()
                .map(snapshot -> new MoisDiscoveryDtos.ArtifactRefResponse(
                        snapshot.getArtifactId(),
                        "mois.marketOfferSourceSnapshot.v1",
                        "v1"
                ))
                .toList());
        refs.addAll(offerCardRepository.findByRequest_RequestIdOrderByCreatedAtDesc(requestId)
                .stream()
                .map(offer -> new MoisDiscoveryDtos.ArtifactRefResponse(
                        offer.getArtifactId(),
                        "mois.marketOfferCard.v1",
                        "v1"
                ))
                .toList());
        refs.addAll(promiseSignalRepository.findByRequest_RequestIdOrderByCreatedAtAsc(requestId)
                .stream()
                .map(promise -> new MoisDiscoveryDtos.ArtifactRefResponse(
                        promise.getArtifactId(),
                        "mois.marketOfferPromiseSignal.v1",
                        "v1"
                ))
                .toList());
        refs.addAll(proofSignalRepository.findByRequest_RequestIdOrderByCreatedAtAsc(requestId)
                .stream()
                .map(proof -> new MoisDiscoveryDtos.ArtifactRefResponse(
                        proof.getArtifactId(),
                        "mois.marketOfferProofSignal.v1",
                        "v1"
                ))
                .toList());
        refs.addAll(mechanismClaimRepository.findByRequest_RequestIdOrderByCreatedAtAsc(requestId)
                .stream()
                .map(mechanism -> new MoisDiscoveryDtos.ArtifactRefResponse(
                        mechanism.getArtifactId(),
                        "mois.marketOfferMechanismClaim.v1",
                        "v1"
                ))
                .toList());
        refs.addAll(funnelPatternRepository.findByRequest_RequestIdOrderByCreatedAtAsc(requestId)
                .stream()
                .map(funnel -> new MoisDiscoveryDtos.ArtifactRefResponse(
                        funnel.getArtifactId(),
                        "mois.marketOfferFunnelPattern.v1",
                        "v1"
                ))
                .toList());
        return refs;
    }

    private List<MoisDiscoveryDtos.ArtifactRefResponse> buildOfferSourceArtifacts(MoisOfferCard offer) {
        List<MoisDiscoveryDtos.ArtifactRefResponse> refs = new ArrayList<>();
        if (offer.getSourceSnapshot() == null) {
            refs.add(new MoisDiscoveryDtos.ArtifactRefResponse(
                    offer.getRequest().getRequestId(),
                    "mois.marketOfferDiscoveryRequest.v1",
                    "v1"
            ));
        } else {
            refs.add(new MoisDiscoveryDtos.ArtifactRefResponse(
                    offer.getSourceSnapshot().getArtifactId(),
                    "mois.marketOfferSourceSnapshot.v1",
                    "v1"
            ));
            refs.add(new MoisDiscoveryDtos.ArtifactRefResponse(
                    offer.getRequest().getRequestId(),
                    "mois.marketOfferDiscoveryRequest.v1",
                    "v1"
            ));
        }
        addFirstArtifactRef(refs, promiseSignalRepository.findByOfferCard_ArtifactIdOrderByCreatedAtAsc(offer.getArtifactId()), "mois.marketOfferPromiseSignal.v1");
        addFirstArtifactRef(refs, proofSignalRepository.findByOfferCard_ArtifactIdOrderByCreatedAtAsc(offer.getArtifactId()), "mois.marketOfferProofSignal.v1");
        addFirstArtifactRef(refs, mechanismClaimRepository.findByOfferCard_ArtifactIdOrderByCreatedAtAsc(offer.getArtifactId()), "mois.marketOfferMechanismClaim.v1");
        addFirstArtifactRef(refs, funnelPatternRepository.findByOfferCard_ArtifactIdOrderByCreatedAtAsc(offer.getArtifactId()), "mois.marketOfferFunnelPattern.v1");
        return refs;
    }

    private void addFirstArtifactRef(List<MoisDiscoveryDtos.ArtifactRefResponse> refs, List<? extends Object> items, String artifactType) {
        if (items == null || items.isEmpty()) {
            return;
        }
        String artifactId = null;
        Object first = items.get(0);
        if (first instanceof MoisOfferPromiseSignal promise) {
            artifactId = promise.getArtifactId();
        } else if (first instanceof MoisOfferProofSignal proof) {
            artifactId = proof.getArtifactId();
        } else if (first instanceof MoisOfferMechanismClaim mechanism) {
            artifactId = mechanism.getArtifactId();
        } else if (first instanceof MoisOfferFunnelPattern funnel) {
            artifactId = funnel.getArtifactId();
        }
        if (artifactId != null) {
            refs.add(new MoisDiscoveryDtos.ArtifactRefResponse(artifactId, artifactType, "v1"));
        }
    }

    private void persistDerivedArtifacts(
            MoisDiscoveryRequest request,
            MoisSourceSnapshot snapshot,
            MoisOfferCard offer,
            MoisOfferExtractionResult extraction
    ) {
        promiseSignalRepository.save(MoisOfferPromiseSignal.builder()
                .request(request)
                .sourceSnapshot(snapshot)
                .offerCard(offer)
                .artifactId("mois-promise-" + UUID.randomUUID())
                .status(MoisArtifactStatus.DRAFT)
                .promiseText(extraction.promiseText())
                .promiseType(extraction.promiseType())
                .intensity(extraction.intensity())
                .timeframeClaim(extraction.timeframeClaim())
                .targetOutcome(extraction.targetOutcome())
                .confidence(extraction.confidence())
                .build());

        proofSignalRepository.save(MoisOfferProofSignal.builder()
                .request(request)
                .sourceSnapshot(snapshot)
                .offerCard(offer)
                .artifactId("mois-proof-" + UUID.randomUUID())
                .status(MoisArtifactStatus.DRAFT)
                .proofType(extraction.proofType())
                .proofText(extraction.proofText())
                .proofStrengthHypothesis(extraction.proofStrength())
                .proofLocation("headline-or-body")
                .confidence(extraction.confidence())
                .build());

        mechanismClaimRepository.save(MoisOfferMechanismClaim.builder()
                .request(request)
                .sourceSnapshot(snapshot)
                .offerCard(offer)
                .artifactId("mois-mechanism-" + UUID.randomUUID())
                .status(MoisArtifactStatus.DRAFT)
                .claimText(extraction.claimText())
                .claimCategory(extraction.claimCategory())
                .claimSpecificity(extraction.claimSpecificity())
                .claimRiskLevel(extraction.claimRiskLevel())
                .confidence(extraction.confidence())
                .build());

        funnelPatternRepository.save(MoisOfferFunnelPattern.builder()
                .request(request)
                .sourceSnapshot(snapshot)
                .offerCard(offer)
                .artifactId("mois-funnel-" + UUID.randomUUID())
                .status(MoisArtifactStatus.DRAFT)
                .entryAssetType(extraction.entryAssetType())
                .leadCaptureFieldsJson(toJson(extraction.leadCaptureFields()))
                .ctaStyle(extraction.ctaStyle())
                .nextStepHypothesis(extraction.nextStepHypothesis())
                .deliveryFormat(extraction.deliveryFormat())
                .upsellVisible(extraction.upsellVisible())
                .retentionHint(extraction.retentionHint())
                .confidence(extraction.confidence())
                .build());
    }

    private List<String> buildOfferSignalParentIds(String requestId, String offerArtifactId, MoisSourceSnapshot sourceSnapshot) {
        List<String> parentIds = new ArrayList<>();
        parentIds.add(requestId);
        if (sourceSnapshot != null) {
            parentIds.add(sourceSnapshot.getArtifactId());
        }
        parentIds.add(offerArtifactId);
        return parentIds;
    }

    private MoisOfferExtractionResult extractSignalsFromSnapshot(MoisDiscoveryRequest request, MoisSourceSnapshot snapshot) {
        String sourceText = defaultText(snapshot.getRawExcerpt(), "");
        List<String> sentences = splitSentences(sourceText);

        String promiseSentence = firstSentenceWithKeywords(
                sentences,
                List.of("garanta", "aumente", "alcance", "consiga", "resultado", "sem", "em ")
        );
        if (promiseSentence.isBlank()) {
            promiseSentence = firstSentenceWithKeywords(sentences, List.of("transform", "aprenda", "domine"));
        }
        if (promiseSentence.isBlank()) {
            promiseSentence = defaultText(request.getPainOrOutcomeFocus(), "Melhoria prática percebida");
        }

        String proofSentence = firstSentenceWithKeywords(
                sentences,
                List.of("depoimento", "caso real", "antes e depois", "clientes", "alunos", "resultados comprovados", "%", "x")
        );
        if (proofSentence.isBlank()) {
            proofSentence = "Sem prova explícita no recorte";
        }

        String mechanismSentence = firstSentenceWithKeywords(
                sentences,
                List.of("método", "metodo", "framework", "protocolo", "sistema", "passo a passo", "estratégia")
        );
        if (mechanismSentence.isBlank()) {
            mechanismSentence = "Sem mecanismo explícito no recorte";
        }

        String mainPrice = extractCurrencyValue(sourceText);
        List<String> pricePoints = mainPrice == null ? List.of("não identificado") : List.of(mainPrice);
        String timeframe = extractTimeframe(sourceText);
        String targetOutcome = defaultText(request.getMarketTheme(), request.getNicheName());

        String entryAsset = containsAny(sourceText, List.of("webinar", "aula", "masterclass")) ? "aula"
                : containsAny(sourceText, List.of("ebook", "guia", "pdf")) ? "isca-digital" : "landing";
        String ctaStyle = containsAny(sourceText, List.of("whatsapp", "fale agora")) ? "conversacional" : "direta";
        String nextStep = containsAny(sourceText, List.of("checkout", "comprar", "pagamento")) ? "checkout"
                : containsAny(sourceText, List.of("whatsapp")) ? "whatsapp" : "lead-follow-up";
        String delivery = containsAny(sourceText, List.of("mentoria", "acompanhamento")) ? "mentoria" : "curso-digital";
        boolean upsellVisible = containsAny(sourceText, List.of("bônus", "bonus", "oferta exclusiva"));

        return new MoisOfferExtractionResult(
                promiseSentence,
                classifyPromiseType(promiseSentence),
                classifyIntensity(promiseSentence),
                timeframe,
                targetOutcome,
                classifyProofType(proofSentence),
                proofSentence,
                classifyProofStrength(proofSentence),
                mechanismSentence,
                classifyClaimCategory(mechanismSentence),
                classifyClaimSpecificity(mechanismSentence),
                classifyClaimRisk(mechanismSentence),
                entryAsset,
                detectLeadCaptureFields(sourceText),
                ctaStyle,
                nextStep,
                delivery,
                upsellVisible,
                "oferta com foco em " + targetOutcome,
                mainPrice,
                pricePoints,
                "entrada=" + entryAsset + ", próximo passo=" + nextStep,
                calculateExtractionConfidence(promiseSentence, proofSentence, mechanismSentence)
        );
    }

    private List<String> splitSentences(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text.split("(?<=[.!?])\\s+"))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String firstSentenceWithKeywords(List<String> sentences, List<String> keywords) {
        for (String sentence : sentences) {
            String normalized = sentence.toLowerCase();
            if (keywords.stream().anyMatch(normalized::contains)) {
                return limitText(sentence, 500);
            }
        }
        return "";
    }

    private boolean containsAny(String text, List<String> terms) {
        String normalized = defaultText(text, "").toLowerCase();
        return terms.stream().anyMatch(normalized::contains);
    }

    private String extractCurrencyValue(String text) {
        Matcher matcher = Pattern.compile("(R\\$\\s?\\d{1,3}(?:\\.\\d{3})*(?:,\\d{2})?)").matcher(defaultText(text, ""));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractTimeframe(String text) {
        Matcher matcher = Pattern.compile("(\\d+\\s*(dias|semanas|meses))", Pattern.CASE_INSENSITIVE)
                .matcher(defaultText(text, ""));
        return matcher.find() ? matcher.group(1) : "não declarado";
    }

    private String classifyPromiseType(String promiseText) {
        String value = defaultText(promiseText, "").toLowerCase();
        if (value.contains("sem ")) {
            return "friction-removal";
        }
        if (value.contains("em ")) {
            return "time-bound";
        }
        return "outcome";
    }

    private String classifyIntensity(String promiseText) {
        String value = defaultText(promiseText, "").toLowerCase();
        if (containsAny(value, List.of("garanta", "definitivo", "100%"))) {
            return "alta";
        }
        if (containsAny(value, List.of("melhore", "aumente", "ganhe"))) {
            return "média";
        }
        return "baixa";
    }

    private String classifyProofType(String proofText) {
        String value = defaultText(proofText, "").toLowerCase();
        if (containsAny(value, List.of("depoimento", "caso real", "cliente", "aluno"))) {
            return "social-proof";
        }
        if (containsAny(value, List.of("%", "x", "dados"))) {
            return "numeric-proof";
        }
        return "implicit-proof";
    }

    private String classifyProofStrength(String proofText) {
        String value = defaultText(proofText, "").toLowerCase();
        if (containsAny(value, List.of("comprovado", "caso real", "antes e depois"))) {
            return "forte";
        }
        if (containsAny(value, List.of("cliente", "aluno", "%", "x"))) {
            return "média";
        }
        return "fraca";
    }

    private String classifyClaimCategory(String claimText) {
        String value = defaultText(claimText, "").toLowerCase();
        if (containsAny(value, List.of("método", "metodo", "framework", "protocolo"))) {
            return "framework";
        }
        if (containsAny(value, List.of("sistema", "automatizado", "automação"))) {
            return "system";
        }
        return "generic";
    }

    private String classifyClaimSpecificity(String claimText) {
        String value = defaultText(claimText, "").toLowerCase();
        if (containsAny(value, List.of("3 passos", "4 etapas", "protocolo"))) {
            return "alta";
        }
        if (containsAny(value, List.of("método", "estratégia"))) {
            return "média";
        }
        return "baixa";
    }

    private String classifyClaimRisk(String claimText) {
        String value = defaultText(claimText, "").toLowerCase();
        if (containsAny(value, List.of("garantido", "certeza", "sem risco"))) {
            return "alto";
        }
        if (containsAny(value, List.of("método", "protocolo", "framework"))) {
            return "médio";
        }
        return "baixo";
    }

    private List<String> detectLeadCaptureFields(String text) {
        List<String> fields = new ArrayList<>();
        String normalized = defaultText(text, "").toLowerCase();
        if (normalized.contains("nome")) {
            fields.add("name");
        }
        if (normalized.contains("email")) {
            fields.add("email");
        }
        if (normalized.contains("whatsapp") || normalized.contains("telefone")) {
            fields.add("phone");
        }
        return fields.isEmpty() ? List.of("name", "email") : fields;
    }

    private double calculateExtractionConfidence(String promise, String proof, String mechanism) {
        double score = 0.45;
        if (!promise.isBlank() && !"Melhoria prática percebida".equals(promise)) {
            score += 0.2;
        }
        if (!proof.contains("Sem prova explícita")) {
            score += 0.15;
        }
        if (!mechanism.contains("Sem mecanismo explícito")) {
            score += 0.15;
        }
        return Math.min(0.95, score);
    }

    private MoisDiscoveryDtos.DiscoveryRequestSummaryResponse toSummaryResponse(MoisDiscoveryRequest request) {
        return new MoisDiscoveryDtos.DiscoveryRequestSummaryResponse(
                request.getRequestId(),
                request.getNicheName(),
                request.getMarketTheme(),
                request.getPainOrOutcomeFocus(),
                request.getStatus().name(),
                request.getCreatedAt()
        );
    }

    private MoisOfferDtos.OfferCardSummaryResponse toOfferSummary(MoisOfferCard offer) {
        return new MoisOfferDtos.OfferCardSummaryResponse(
                offer.getArtifactId(),
                offer.getRequest().getRequestId(),
                offer.getRequest().getNicheName(),
                offer.getOfferName(),
                offer.getSellerOrBrand(),
                offer.getCorePromise(),
                offer.getPrimaryOfferType(),
                offer.getMainPrice(),
                offer.getConfidence()
        );
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private Map<String, Object> defaultMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }

    private MoisDiscoveryRequestStatus parseStatus(String status) {
        try {
            return MoisDiscoveryRequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid status");
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "invalid persisted mois json list");
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "invalid persisted mois json map");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "invalid mois payload");
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record MoisOfferExtractionResult(
            String promiseText,
            String promiseType,
            String intensity,
            String timeframeClaim,
            String targetOutcome,
            String proofType,
            String proofText,
            String proofStrength,
            String claimText,
            String claimCategory,
            String claimSpecificity,
            String claimRiskLevel,
            String entryAssetType,
            List<String> leadCaptureFields,
            String ctaStyle,
            String nextStepHypothesis,
            String deliveryFormat,
            Boolean upsellVisible,
            String retentionHint,
            String mainPrice,
            List<String> pricePoints,
            String funnelSummary,
            Double confidence
    ) {
    }
}
