package com.marketinghub.targeting.service;

import com.marketinghub.targeting.TargetingAudienceType;
import com.marketinghub.targeting.TargetingCandidate;
import com.marketinghub.targeting.TargetingCandidateStatus;
import com.marketinghub.targeting.TargetingCandidateType;
import com.marketinghub.targeting.TargetingOption;
import com.marketinghub.targeting.TargetingOptionSource;
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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class TargetingRequestService {
    private static final Logger log = LoggerFactory.getLogger(TargetingRequestService.class);
    private static final String DEFAULT_LOCALE = "pt_BR";
    private static final String DEFAULT_COUNTRY = "BR";
    private static final int ETA_SECONDS = 90;
    private static final int MAX_SEED_WORDS = 4;
    private static final int MAX_VARIANTS = 6;
    private static final Pattern LOCATION_SUFFIX_PATTERN = Pattern.compile("(?i)\\s+(em|no|na)\\s+[\\p{L}\\s]{2,}$");
    private static final Pattern STATE_SUFFIX_PATTERN = Pattern.compile("(?i)\\s+(em|no|na)\\s+[A-Z]{2}$");
    private static final Pattern PARENTHESIS_SUFFIX_PATTERN = Pattern.compile("\\s*\\([^)]*\\)$");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

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
            TargetingCandidate candidate = buildCandidate(candidatePayload, request);
            if (candidate == null) {
                continue;
            }
            TargetingCandidate savedCandidate = candidateRepository.save(candidate);
            persistedCandidates.add(savedCandidate);
        }
        if (persistedCandidates.isEmpty()) {
            log.warn("No valid candidates found for request {}", requestId);
            return;
        }
        request.setStatus(TargetingRequestStatus.COMPLETED);
        requestRepository.save(request);
        log.info("Persisted {} candidates for request {}", persistedCandidates.size(), requestId);
        triggerResolutionAfterCommit(request, List.copyOf(persistedCandidates));
    }

    private TargetingCandidate buildCandidate(TargetingCandidateIngestionRequest.CandidatePayload payload,
                                              TargetingRequest request) {
        if (payload == null) {
            return null;
        }
        String seed = sanitizeSeed(resolveSeed(payload));
        if (!StringUtils.hasText(seed)) {
            return null;
        }
        String locale = firstNonBlank(payload.getIdiomaHint(), payload.getIdioma(), request.getLocale());
        String country = firstNonBlank(
                payload.getPais(),
                payload.getConstraints() != null ? payload.getConstraints().getCountry() : null,
                request.getCountry()
        );
        List<String> variants = normalizeVariants(payload.getSeedVariants(), seed);
        return TargetingCandidate.builder()
                .request(request)
                .seed(seed)
                .seedVariants(variants)
                .type(payload.getTipo() != null ? payload.getTipo() : TargetingCandidateType.INTEREST)
                .status(TargetingCandidateStatus.PENDING_FACEBOOK_MATCH)
                .origem(StringUtils.hasText(payload.getOrigem()) ? payload.getOrigem() : "AI")
                .score(normalizeScore(payload.getScore()))
                .rationale(payload.getRationale())
                .localeHint(normalizeLocale(locale))
                .country(normalizeCountry(country))
                .intentTag(payload.getIntentTag())
                .build();
    }

    @Transactional
    public TargetingCandidate reprocessCandidate(Long candidateId, TargetingCandidateReprocessRequest payload) {
        TargetingCandidate candidate = loadCandidate(candidateId);
        if (payload != null) {
            String seedInput = firstNonBlank(payload.getSeed(), payload.getLegacySeed());
            if (StringUtils.hasText(seedInput)) {
                String sanitized = sanitizeSeed(seedInput);
                if (StringUtils.hasText(sanitized)) {
                    candidate.setSeed(sanitized);
                    candidate.setSeedVariants(normalizeVariants(payload.getSeedVariants(), sanitized));
                }
            } else if (!CollectionUtils.isEmpty(payload.getSeedVariants())) {
                candidate.setSeedVariants(normalizeVariants(payload.getSeedVariants(), candidate.getSeed()));
            }
            String locale = firstNonBlank(payload.getIdiomaHint(), payload.getIdioma());
            if (StringUtils.hasText(locale)) {
                candidate.setLocaleHint(normalizeLocale(locale));
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
                .finalScore(payload.getFinalScore())
                .path(path)
                .searchLocale(normalizeOptionalLocale(payload.getSearchLocale()))
                .searchCountry(normalizeOptionalCountry(payload.getSearchCountry()))
                .searchTerm(normalize(payload.getSearchTerm()))
                .source(payload.getSource())
                .seedVariant(normalize(payload.getSeedVariant()))
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
                .toList();
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

    private String resolveSeed(TargetingCandidateIngestionRequest.CandidatePayload payload) {
        if (payload == null) {
            return null;
        }
        return firstNonBlank(payload.getSeed(), payload.getLegacySeed());
    }

    private List<String> normalizeVariants(List<String> providedVariants, String seed) {
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        if (StringUtils.hasText(seed)) {
            variants.add(seed);
            variants.add(removeAccents(seed));
            maybeAddGrammaticalVariant(variants, seed);
        }
        if (!CollectionUtils.isEmpty(providedVariants)) {
            for (String variant : providedVariants) {
                String sanitized = sanitizeSeed(variant);
                if (StringUtils.hasText(sanitized)) {
                    variants.add(sanitized);
                    variants.add(removeAccents(sanitized));
                }
            }
        }
        variants.removeIf(v -> !StringUtils.hasText(v));
        return variants.stream().limit(MAX_VARIANTS).toList();
    }

    private void maybeAddGrammaticalVariant(LinkedHashSet<String> variants, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 3) {
            return;
        }
        if (trimmed.endsWith("s")) {
            String singular = trimmed.substring(0, trimmed.length() - 1).trim();
            if (StringUtils.hasText(singular)) {
                variants.add(singular);
            }
        } else {
            variants.add(trimmed + "s");
        }
    }

    private String sanitizeSeed(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String collapsed = MULTIPLE_SPACES.matcher(raw).replaceAll(" ").trim();
        collapsed = PARENTHESIS_SUFFIX_PATTERN.matcher(collapsed).replaceAll("");
        collapsed = LOCATION_SUFFIX_PATTERN.matcher(collapsed).replaceAll("");
        collapsed = STATE_SUFFIX_PATTERN.matcher(collapsed).replaceAll("");
        collapsed = collapsed.replaceAll("[\\p{Punct}]+$", "");
        collapsed = collapsed.trim();
        if (collapsed.isEmpty()) {
            return null;
        }
        String limited = limitWords(collapsed, MAX_SEED_WORDS);
        return limited.isEmpty() ? null : limited;
    }

    private String limitWords(String value, int maxWords) {
        String[] tokens = MULTIPLE_SPACES.matcher(value).replaceAll(" ").trim().split(" ");
        if (tokens.length <= maxWords) {
            return String.join(" ", tokens).trim();
        }
        return String.join(" ", Arrays.copyOf(tokens, maxWords)).trim();
    }

    private String removeAccents(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
