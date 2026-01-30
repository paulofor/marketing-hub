package com.marketinghub.targeting.service;

import com.marketinghub.targeting.*;
import com.marketinghub.targeting.dto.CreateTargetingRequestPayload;
import com.marketinghub.targeting.dto.TargetingCandidateIngestionRequest;
import com.marketinghub.targeting.repository.TargetingCandidateRepository;
import com.marketinghub.targeting.repository.TargetingRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TargetingRequestService {
    private static final Logger log = LoggerFactory.getLogger(TargetingRequestService.class);
    private static final String DEFAULT_LOCALE = "pt_BR";
    private static final String DEFAULT_COUNTRY = "BR";
    private static final int ETA_SECONDS = 90;

    private final TargetingRequestRepository requestRepository;
    private final TargetingCandidateRepository candidateRepository;

    public TargetingRequestService(TargetingRequestRepository requestRepository,
                                   TargetingCandidateRepository candidateRepository) {
        this.requestRepository = requestRepository;
        this.candidateRepository = candidateRepository;
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
        for (TargetingCandidateIngestionRequest.CandidatePayload candidatePayload : payload.getCandidates()) {
            if (candidatePayload == null || !StringUtils.hasText(candidatePayload.getTextoSugerido())) {
                continue;
            }
            TargetingCandidate candidate = TargetingCandidate.builder()
                    .request(request)
                    .textoSugerido(candidatePayload.getTextoSugerido().trim())
                    .type(candidatePayload.getTipo() != null ? candidatePayload.getTipo() : TargetingCandidateType.INTEREST)
                    .status(TargetingCandidateStatus.PENDING_FACEBOOK_MATCH)
                    .origem(StringUtils.hasText(candidatePayload.getOrigem()) ? candidatePayload.getOrigem() : "AI")
                    .score(normalizeScore(candidatePayload.getScore()))
                    .rationale(candidatePayload.getRationale())
                    .idioma(normalizeLocale(candidatePayload.getIdioma()))
                    .intentTag(candidatePayload.getIntentTag())
                    .build();
            candidateRepository.save(candidate);
        }
        request.setStatus(TargetingRequestStatus.COMPLETED);
        requestRepository.save(request);
        log.info("Persisted {} candidates for request {}", payload.getCandidates().size(), requestId);
    }

    public int etaSeconds() {
        return ETA_SECONDS;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.trim();
    }

    private String normalizeLocale(String locale) {
        if (!StringUtils.hasText(locale)) return DEFAULT_LOCALE;
        return locale.replace('-', '_').trim();
    }

    private String normalizeCountry(String country) {
        if (!StringUtils.hasText(country)) return DEFAULT_COUNTRY;
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
