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
    private final ObjectMapper objectMapper;
    private final MoisResearchGateway researchGateway;

    public MoisApiStubService(
            MoisDiscoveryRequestRepository discoveryRequestRepository,
            MoisSourceSnapshotRepository sourceSnapshotRepository,
            MoisOfferCardRepository offerCardRepository,
            ObjectMapper objectMapper,
            MoisResearchGateway researchGateway
    ) {
        this.discoveryRequestRepository = discoveryRequestRepository;
        this.sourceSnapshotRepository = sourceSnapshotRepository;
        this.offerCardRepository = offerCardRepository;
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
                List.of("Executar Sprint 3 com descoberta real de fontes")
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
                offerCardRepository.save(buildOfferFromSnapshot(request, savedSnapshot));
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

    private MoisOfferCard buildOfferFromSnapshot(MoisDiscoveryRequest request, MoisSourceSnapshot savedSnapshot) {
        return MoisOfferCard.builder()
                .request(request)
                .sourceSnapshot(savedSnapshot)
                .artifactId("mois-offer-" + UUID.randomUUID())
                .status(MoisArtifactStatus.DRAFT)
                .offerName(defaultText(savedSnapshot.getSourceTitle(), "Oferta observada"))
                .sellerOrBrand("Market source")
                .channel(defaultText(savedSnapshot.getSourceKind(), "landing"))
                .targetAudienceHypothesis("Público buscando " + request.getMarketTheme())
                .corePromise(defaultText(request.getPainOrOutcomeFocus(), "Melhoria prática percebida"))
                .primaryOfferType("unknown")
                .mainPrice("não identificado")
                .confidence(0.4)
                .deliverablesJson(toJson(List.of("conteúdo a extrair na sprint 4")))
                .pricePointsJson(toJson(List.of("não identificado")))
                .proofSummary("Prova pendente de extração")
                .mechanismClaimSummary("Mecanismo alegado pendente de extração")
                .positioningSummary("Posicionamento preliminar de fonte real")
                .funnelPatternSummary("captura por descoberta real")
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
        return refs;
    }

    private List<MoisDiscoveryDtos.ArtifactRefResponse> buildOfferSourceArtifacts(MoisOfferCard offer) {
        if (offer.getSourceSnapshot() == null) {
            return List.of(new MoisDiscoveryDtos.ArtifactRefResponse(
                    offer.getRequest().getRequestId(),
                    "mois.marketOfferDiscoveryRequest.v1",
                    "v1"
            ));
        }
        return List.of(
                new MoisDiscoveryDtos.ArtifactRefResponse(
                        offer.getSourceSnapshot().getArtifactId(),
                        "mois.marketOfferSourceSnapshot.v1",
                        "v1"
                ),
                new MoisDiscoveryDtos.ArtifactRefResponse(
                        offer.getRequest().getRequestId(),
                        "mois.marketOfferDiscoveryRequest.v1",
                        "v1"
                )
        );
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
}
