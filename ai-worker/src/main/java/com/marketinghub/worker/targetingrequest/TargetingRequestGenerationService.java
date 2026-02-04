package com.marketinghub.worker.targetingrequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TargetingRequestGenerationService {
    private static final Logger log = LoggerFactory.getLogger(TargetingRequestGenerationService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+?\\d[\\s-]?)?(\\(?\\d{2,3}\\)?[\\s-]?)?\\d{4,5}[\\s-]?\\d{4}");
    private static final Set<String> FORBIDDEN_TERMS = Set.of("sexo", "armas", "violência", "ódio", "hate", "drogas");
    private static final int LIMIT_PER_TYPE = 30;
    private static final int MAX_VARIANTS = 6;
    private static final int MAX_SEED_WORDS = 4;
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern LOCATION_SUFFIX_PATTERN = Pattern.compile("(?i)\\s+(em|no|na)\\s+[\\p{L}\\s]{2,}$");
    private static final Pattern STATE_SUFFIX_PATTERN = Pattern.compile("(?i)\\s+(em|no|na)\\s+[A-Z]{2}$");
    private static final Pattern PARENTHESIS_SUFFIX_PATTERN = Pattern.compile("\\s*\\([^)]*\\)$");

    private final BackendTargetingRequestClient backendClient;
    private final TargetingRequestChatGptClient chatGptClient;

    public TargetingRequestGenerationService(BackendTargetingRequestClient backendClient,
                                             TargetingRequestChatGptClient chatGptClient) {
        this.backendClient = backendClient;
        this.chatGptClient = chatGptClient;
    }

    public void processPending() {
        List<TargetingRequestDto> pending = backendClient.listPending(20);
        if (pending.isEmpty()) {
            return;
        }
        for (TargetingRequestDto request : pending) {
            try {
                handleRequest(request);
            } catch (Exception e) {
                log.error("Failed to generate targeting candidates for request {}", request.id(), e);
            }
        }
    }

    private void handleRequest(TargetingRequestDto request) {
        List<TargetingCandidateSuggestion> suggestions = chatGptClient.generateCandidates(request);
        List<TargetingCandidateSuggestion> filtered = filterSuggestions(suggestions, request);
        TargetingCandidateIngestionPayload payload = new TargetingCandidateIngestionPayload(
                filtered.stream()
                        .map(s -> toPayload(s, request))
                        .toList()
        );
        backendClient.sendCandidates(request.id(), payload);
        log.info("Sent {} candidates for targeting request {}", filtered.size(), request.id());
    }

    private TargetingCandidateIngestionPayload.CandidatePayload toPayload(TargetingCandidateSuggestion suggestion,
                                                                         TargetingRequestDto request) {
        TargetingCandidateIngestionPayload.ConstraintsPayload constraints =
                new TargetingCandidateIngestionPayload.ConstraintsPayload(firstNonBlank(
                        suggestion.countryConstraint(),
                        request.countryOrDefault()
                ));
        List<String> variants = normalizeVariants(suggestion.seedVariants(), suggestion.seed());
        return new TargetingCandidateIngestionPayload.CandidatePayload(
                suggestion.seed(),
                suggestion.seed(),
                variants,
                suggestion.tipo(),
                StringUtils.hasText(suggestion.origem()) ? suggestion.origem() : "AI",
                normalizeScore(suggestion.score()),
                suggestion.rationale(),
                resolveLocale(suggestion.idiomaHint(), request),
                firstNonBlank(suggestion.countryConstraint(), request.countryOrDefault()),
                suggestion.intentTag(),
                constraints
        );
    }

    private List<TargetingCandidateSuggestion> filterSuggestions(List<TargetingCandidateSuggestion> suggestions,
                                                                 TargetingRequestDto request) {
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of();
        }
        Map<TargetingCandidateType, List<TargetingCandidateSuggestion>> grouped = new LinkedHashMap<>();
        Set<String> seen = new LinkedHashSet<>();
        for (TargetingCandidateSuggestion suggestion : suggestions) {
            if (suggestion == null || !StringUtils.hasText(suggestion.seed())) {
                continue;
            }
            String sanitizedSeed = sanitizeSeed(suggestion.seed());
            if (!StringUtils.hasText(sanitizedSeed)) {
                continue;
            }
            TargetingCandidateType type = suggestion.tipo() != null ? suggestion.tipo() : TargetingCandidateType.INTEREST;
            String key = type + "|" + sanitizedSeed.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                continue;
            }
            if (containsForbidden(sanitizedSeed) || hasPii(sanitizedSeed)) {
                continue;
            }
            List<String> variants = normalizeVariants(suggestion.seedVariants(), sanitizedSeed);
            TargetingCandidateSuggestion sanitized = new TargetingCandidateSuggestion(
                    sanitizedSeed,
                    variants,
                    type,
                    suggestion.origem(),
                    normalizeScore(suggestion.score()),
                    suggestion.rationale(),
                    resolveLocale(suggestion.idiomaHint(), request),
                    suggestion.intentTag(),
                    suggestion.countryConstraint()
            );
            grouped.computeIfAbsent(type, keyType -> new ArrayList<>()).add(sanitized);
        }

        if (grouped.isEmpty() && !"en_US".equalsIgnoreCase(request.localeOrDefault())) {
            seen.clear();
            for (TargetingCandidateSuggestion suggestion : suggestions) {
                if (suggestion == null || !StringUtils.hasText(suggestion.seed())) {
                    continue;
                }
                String sanitizedSeed = sanitizeSeed(suggestion.seed());
                if (!StringUtils.hasText(sanitizedSeed)) {
                    continue;
                }
                TargetingCandidateType type = suggestion.tipo() != null ? suggestion.tipo() : TargetingCandidateType.INTEREST;
                String key = type + "|" + sanitizedSeed.toLowerCase(Locale.ROOT);
                if (!seen.add(key)) {
                    continue;
                }
                if (containsForbidden(sanitizedSeed) || hasPii(sanitizedSeed)) {
                    continue;
                }
                List<String> variants = normalizeVariants(suggestion.seedVariants(), sanitizedSeed);
                TargetingCandidateSuggestion fallback = new TargetingCandidateSuggestion(
                        sanitizedSeed,
                        variants,
                        type,
                        suggestion.origem(),
                        normalizeScore(suggestion.score()),
                        suggestion.rationale(),
                        "en_US",
                        suggestion.intentTag(),
                        suggestion.countryConstraint()
                );
                grouped.computeIfAbsent(type, keyType -> new ArrayList<>()).add(fallback);
            }
        }

        return grouped.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .sorted(Comparator.comparing(TargetingCandidateSuggestion::score, Comparator.nullsLast(BigDecimal::compareTo)).reversed())
                        .limit(LIMIT_PER_TYPE))
                .collect(Collectors.toList());
    }

    private boolean hasPii(String value) {
        return EMAIL_PATTERN.matcher(value).find() || PHONE_PATTERN.matcher(value).find();
    }

    private boolean containsForbidden(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return FORBIDDEN_TERMS.stream().anyMatch(lower::contains);
    }

    private String resolveLocale(String candidateLocale, TargetingRequestDto request) {
        String locale = StringUtils.hasText(candidateLocale) ? candidateLocale : request.localeOrDefault();
        return locale.replace('-', '_');
    }

    private BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) return null;
        if (score.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (score.compareTo(BigDecimal.ONE) > 0) return BigDecimal.ONE;
        return score;
    }

    private List<String> normalizeVariants(List<String> providedVariants, String seed) {
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        if (StringUtils.hasText(seed)) {
            variants.add(seed);
            variants.add(removeAccents(seed));
        }
        if (providedVariants != null) {
            for (String variant : providedVariants) {
                String sanitized = sanitizeSeed(variant);
                if (StringUtils.hasText(sanitized)) {
                    variants.add(sanitized);
                    variants.add(removeAccents(sanitized));
                }
            }
        }
        variants.removeIf(value -> !StringUtils.hasText(value));
        return variants.stream().limit(MAX_VARIANTS).toList();
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
        if (!StringUtils.hasText(collapsed)) {
            return null;
        }
        String[] tokens = collapsed.split(" ");
        if (tokens.length <= MAX_SEED_WORDS) {
            return collapsed;
        }
        return String.join(" ", Arrays.asList(tokens).subList(0, MAX_SEED_WORDS));
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
