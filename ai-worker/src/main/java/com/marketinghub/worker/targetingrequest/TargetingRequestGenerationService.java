package com.marketinghub.worker.targetingrequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
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
                        .map(this::toPayload)
                        .toList()
        );
        backendClient.sendCandidates(request.id(), payload);
        log.info("Sent {} candidates for targeting request {}", filtered.size(), request.id());
    }

    private TargetingCandidateIngestionPayload.CandidatePayload toPayload(TargetingCandidateSuggestion suggestion) {
        return new TargetingCandidateIngestionPayload.CandidatePayload(
                suggestion.textoSugerido(),
                suggestion.tipo(),
                StringUtils.hasText(suggestion.origem()) ? suggestion.origem() : "AI",
                normalizeScore(suggestion.score()),
                suggestion.rationale(),
                normalizeLocale(suggestion.idioma()),
                suggestion.intentTag()
        );
    }

    private List<TargetingCandidateSuggestion> filterSuggestions(List<TargetingCandidateSuggestion> suggestions,
                                                                 TargetingRequestDto request) {
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        Map<TargetingCandidateType, List<TargetingCandidateSuggestion>> grouped = new LinkedHashMap<>();
        for (TargetingCandidateSuggestion suggestion : suggestions) {
            if (suggestion == null || !StringUtils.hasText(suggestion.textoSugerido())) {
                continue;
            }
            String texto = suggestion.textoSugerido().trim();
            String normalizedKey = (suggestion.tipo() != null ? suggestion.tipo() : TargetingCandidateType.INTEREST)
                    + "|" + texto.toLowerCase();
            if (!seen.add(normalizedKey)) {
                continue;
            }
            if (containsForbidden(texto) || hasPii(texto)) {
                continue;
            }
            TargetingCandidateSuggestion sanitized = new TargetingCandidateSuggestion(
                    texto,
                    suggestion.tipo() != null ? suggestion.tipo() : TargetingCandidateType.INTEREST,
                    suggestion.origem(),
                    normalizeScore(suggestion.score()),
                    suggestion.rationale(),
                    resolveLocale(suggestion.idioma(), request),
                    suggestion.intentTag()
            );
            grouped.computeIfAbsent(sanitized.tipo(), key -> new ArrayList<>()).add(sanitized);
        }

        if (grouped.isEmpty() && !"en_US".equalsIgnoreCase(request.localeOrDefault())) {
            seen.clear();
            for (TargetingCandidateSuggestion suggestion : suggestions) {
                if (suggestion == null || !StringUtils.hasText(suggestion.textoSugerido())) {
                    continue;
                }
                String texto = suggestion.textoSugerido().trim();
                String normalizedKey = (suggestion.tipo() != null ? suggestion.tipo() : TargetingCandidateType.INTEREST)
                        + "|" + texto.toLowerCase();
                if (!seen.add(normalizedKey)) {
                    continue;
                }
                if (containsForbidden(texto) || hasPii(texto)) {
                    continue;
                }
                TargetingCandidateSuggestion fallbackSuggestion = new TargetingCandidateSuggestion(
                        texto,
                        suggestion.tipo() != null ? suggestion.tipo() : TargetingCandidateType.INTEREST,
                        suggestion.origem(),
                        normalizeScore(suggestion.score()),
                        suggestion.rationale(),
                        "en_US",
                        suggestion.intentTag()
                );
                grouped.computeIfAbsent(fallbackSuggestion.tipo(), key -> new ArrayList<>()).add(fallbackSuggestion);
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
        String lower = value.toLowerCase();
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

    private String normalizeLocale(String locale) {
        if (!StringUtils.hasText(locale)) {
            return null;
        }
        String normalized = locale.trim().replace('-', '_');
        if (normalized.length() == 5 && normalized.charAt(2) == '_') {
            String lang = normalized.substring(0, 2).toLowerCase(Locale.ROOT);
            String country = normalized.substring(3).toUpperCase(Locale.ROOT);
            return lang + "_" + country;
        }
        return normalized;
    }
}
