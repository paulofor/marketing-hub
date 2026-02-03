package com.marketinghub.targeting.service;

import com.marketinghub.targeting.TargetingAudienceType;
import com.marketinghub.targeting.TargetingCandidate;
import com.marketinghub.targeting.TargetingCandidateStatus;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingOption;
import com.marketinghub.targeting.TargetingRequest;
import com.marketinghub.targeting.TargetingRequestOrigin;
import com.marketinghub.targeting.TargetingRequestStatus;
import com.marketinghub.targeting.dto.CreateTargetingRequestPayload;
import com.marketinghub.targeting.dto.TargetingCandidateIngestionRequest;
import com.marketinghub.targeting.dto.TargetingCandidateReprocessRequest;
import com.marketinghub.targeting.dto.TargetingCandidateResolutionUpdateRequest;
import com.marketinghub.targeting.dto.TargetingCandidateResolutionUpdateRequest.OptionPayload;
import com.marketinghub.targeting.integration.TargetingResolverClient;
import com.marketinghub.targeting.repository.TargetingCandidateRepository;
import com.marketinghub.targeting.repository.TargetingRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TargetingRequestService {
    private static final Logger log = LoggerFactory.getLogger(TargetingRequestService.class);
    private static final String DEFAULT_LOCALE = "pt_BR";
    private static final String DEFAULT_COUNTRY = "BR";
    private static final int ETA_SECONDS = 90;

    private final TargetingRequestRepository requestRepository;
    private final TargetingCandidateRepository candidateRepository;
    private final TargetingResolverClient targetingResolverClient;

    public TargetingRequestService(TargetingRequestRepository requestRepository,
                                   TargetingCandidateRepository candidateRepository,
                                   TargetingResolverClient targetingResolverClient) {
        this.requestRepository = requestRepository;
        this.candidateRepository = candidateRepository;
        this.targetingResolverClient = targetingResolverClient;
    }

    @Transactional
    public TargetingRequest create(CreateTargetingRequestPayload payload) {
        TargetingRequest request = TargetingRequest.builder()
                .descricao(normalize(payload.getDescricao()))
                .locale(normalizeLocale(payload.getIdioma()))
                .country(normalizeCountry(payload.getPais()))
                .audienceType(payload.getPublicoTipo() != null ? payload.getPublicoTipo() : TargetingAudienceType.PROSPECT)
                .status(TargetingRequestStatus.PENDING_AI)
                .origin(TargetingRequestOrigin.CLIENT)
                .build();
        TargetingRequest saved = requestRepository.save(request);
        log.info("Created targeting request {} with status {}", saved.getId(), saved.getStatus());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<TargetingRequest> listRequests(TargetingRequestStatus status, int limit) {
        int pageSize = limit > 0 ? limit : 10;
        PageRequest pageable = PageRequest.of(0, pageSize);
        if (status != null) {
            return requestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return requestRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public TargetingRequest getWithCandidates(UUID requestId) {
        return requestRepository.findDetailedById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Targeting request %s not found".formatted(requestId)));
    }

    public List<TargetingRequest> listPendingForAi(int limit) {
        return requestRepository.findByStatus(TargetingRequestStatus.PENDING_AI)
                .stream()
                .limit(Math.max(limit, 1))
                .toList();
    }

    @Transactional
    public void saveCandidates(UUID requestId, TargetingCandidateIngestionRequest payload) {
        TargetingRequest request = requestRepository.findById(requestId).orElseThrow();
        if (payload == null || payload.getCandidates() == null) {
            log.warn("Received empty candidate payload for request {}", requestId);
            return;
        }
        List<TargetingCandidate> persistedCandidates = new ArrayList<>();
        for (TargetingCandidateIngestionRequest.CandidatePayload candidatePayload : payload.getCandidates()) {
            if (candidatePayload == null || !StringUtils.hasText(candidatePayload.getTextoSugerido())) {
                continue;
            }
            String idioma = StringUtils.hasText(candidatePayload.getIdioma())
                    ? candidatePayload.getIdioma()
                    : request.getLocale();
            String country = StringUtils.hasText(candidatePayload.getPais())
                    ? candidatePayload.getPais()
                    : request.getCountry();
            TargetingCandidate candidate = TargetingCandidate.builder()
                    .request(request)
                    .textoSugerido(candidatePayload.getTextoSugerido().trim())
                    .type(candidatePayload.getTipo() != null ? candidatePayload.getTipo() : TargetingCandidateType.INTEREST)
                    .status(TargetingCandidateStatus.PENDING_FACEBOOK_MATCH)
                    .origem(StringUtils.hasText(candidatePayload.getOrigem()) ? candidatePayload.getOrigem() : "AI")
                    .score(normalizeScore(candidatePayload.getScore()))
                    .rationale(candidatePayload.getRationale())
                    .idioma(normalizeLocale(idioma))
                    .country(normalizeCountry(country))
                    .intentTag(candidatePayload.getIntentTag())
                    .build();
            TargetingCandidate savedCandidate = candidateRepository.save(candidate);
            persistedCandidates.add(savedCandidate);
        }
        request.setStatus(TargetingRequestStatus.COMPLETED);
        requestRepository.save(request);
        log.info("Persisted {} candidates for request {}", payload.getCandidates().size(), requestId);
        triggerResolutionAfterCommit(request, List.copyOf(persistedCandidates));
    }

    @Transactional
    public TargetingCandidate reprocessCandidate(Long candidateId, TargetingCandidateReprocessRequest payload) {
        TargetingCandidate candidate = loadCandidate(candidateId);
        if (payload != null) {
            if (StringUtils.hasText(payload.getTextoSugerido())) {
                candidate.setTextoSugerido(payload.getTextoSugerido().trim());
            }
            if (StringUtils.hasText(payload.getIdioma())) {
                candidate.setIdioma(normalizeLocale(payload.getIdioma()));
            }
            if (StringUtils.hasText(payload.getPais())) {
                candidate.setCountry(normalizeCountry(payload.getPais()));
            }
        }
        candidate.setStatus(TargetingCandidateStatus.PENDING_FACEBOOK_MATCH);
        candidate.setRejectionReason(null);
        if (!CollectionUtils.isEmpty(candidate.getOptions())) {
            candidate.getOptions().clear();
        }
        TargetingCandidate saved = candidateRepository.save(candidate);
        log.info("Candidate {} requeued for Facebook resolution", candidateId);
        triggerResolutionAfterCommit(candidate.getRequest(), List.of(saved));
        return saved;
    }

    @Transactional
    public void applyResolution(Long candidateId, TargetingCandidateResolutionUpdateRequest payload) {
        if (payload == null || payload.getStatus() == null) {
            log.warn("Ignoring resolution update for candidate {} due to empty payload", candidateId);
            return;
        }
        TargetingCandidate candidate = loadCandidate(candidateId);
        candidate.setStatus(payload.getStatus());
        candidate.setRejectionReason(normalize(payload.getRejectionReason()));
        if (!CollectionUtils.isEmpty(candidate.getOptions())) {
            candidate.getOptions().clear();
        }
        if (payload.getStatus() == TargetingCandidateStatus.VALIDATED) {
            List<OptionPayload> optionPayloads = payload.getOptions();
            if (!CollectionUtils.isEmpty(optionPayloads)) {
                for (OptionPayload optionPayload : optionPayloads) {
                    TargetingOption option = toOptionEntity(candidate, optionPayload);
                    if (option != null) {
                        candidate.getOptions().add(option);
                    }
                }
            }
        }
        candidateRepository.save(candidate);
        log.info("Updated candidate {} with status {}", candidateId, candidate.getStatus());
    }

    public int etaSeconds() {
        return ETA_SECONDS;
    }

    private void triggerResolutionAfterCommit(TargetingRequest request, List<TargetingCandidate> candidates) {
        if (request == null || CollectionUtils.isEmpty(candidates)) {
            return;
        }
        Runnable action = () -> targetingResolverClient.requestResolution(request, candidates);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private TargetingCandidate loadCandidate(Long candidateId) {
        return candidateRepository.findDetailedById(candidateId)
                .orElseThrow(() -> new EntityNotFoundException("Targeting candidate %s not found".formatted(candidateId)));
    }

    private TargetingOption toOptionEntity(TargetingCandidate candidate, OptionPayload payload) {
        if (payload == null) {
            return null;
        }
        if (!StringUtils.hasText(payload.getFacebookId()) || !StringUtils.hasText(payload.getName())) {
            return null;
        }
        List<String> path = sanitizePath(payload.getPath());
        return TargetingOption.builder()
                .candidate(candidate)
                .facebookId(payload.getFacebookId().trim())
                .name(payload.getName().trim())
                .type(payload.getType() != null ? payload.getType() : candidate.getType())
                .audienceSize(payload.getAudienceSize())
                .matchScore(payload.getMatchScore())
                .path(path)
                .searchLocale(normalizeOptionalLocale(payload.getSearchLocale()))
                .searchCountry(normalizeOptionalCountry(payload.getSearchCountry()))
                .searchTerm(normalize(payload.getSearchTerm()))
                .build();
    }

    private List<String> sanitizePath(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }
        return entries.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.trim();
    }

    private String normalizeLocale(String locale) {
        if (!StringUtils.hasText(locale)) return DEFAULT_LOCALE;
        return locale.replace('-', '_').trim();
    }

    private String normalizeOptionalLocale(String locale) {
        if (!StringUtils.hasText(locale)) return null;
        return locale.replace('-', '_').trim();
    }

    private String normalizeCountry(String country) {
        if (!StringUtils.hasText(country)) return DEFAULT_COUNTRY;
        return country.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalCountry(String country) {
        if (!StringUtils.hasText(country)) return null;
        return country.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) return null;
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal one = BigDecimal.ONE;
        if (score.compareTo(zero) < 0) return zero;
        if (score.compareTo(one) > 0) return one;
        return score;
    }
}
