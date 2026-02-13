package com.marketinghub.facebookadsworker.facebooktargeting;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingBackendClient.TargetingCandidateResolutionUpdate;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingBackendClient.TargetingOptionPayload;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolutionResponse.CandidateResolutionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Serviço responsável por aterrar os candidatos em opções válidas da Meta.
 */
@Service
public class TargetingResolverService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TargetingResolverService.class);
    private static final double AI_SCORE_WEIGHT = 0.55;
    private static final double MATCH_SCORE_WEIGHT = 0.35;
    private static final double SIZE_SCORE_WEIGHT = 0.10;
    private static final double MIN_AUDIENCE = 1_000d;
    private static final double MAX_AUDIENCE = 50_000_000d;

    private final FacebookAdsService facebookAdsService;
    private final TargetingBackendClient backendClient;
    private final TargetingResolverProperties properties;

    public TargetingResolverService(FacebookAdsService facebookAdsService,
                                    TargetingBackendClient backendClient,
                                    TargetingResolverProperties properties) {
        this.facebookAdsService = facebookAdsService;
        this.backendClient = backendClient;
        this.properties = properties;
    }

    public TargetingResolutionResponse resolve(UUID requestId, TargetingResolutionRequest request) {
        List<TargetingCandidatePayload> candidates = request != null ? request.getCandidates() : List.of();
        if (CollectionUtils.isEmpty(candidates)) {
            LOGGER.info("Received targeting resolution request {} without candidates", requestId);
            return new TargetingResolutionResponse(requestId, List.of());
        }

        List<CandidateResolutionSummary> summaries = new ArrayList<>();
        for (TargetingCandidatePayload candidate : candidates) {
            summaries.add(processCandidate(requestId, request, candidate));
        }
        return new TargetingResolutionResponse(requestId, summaries);
    }

    private CandidateResolutionSummary processCandidate(UUID requestId,
                                                         TargetingResolutionRequest request,
                                                         TargetingCandidatePayload candidate) {
        if (candidate == null || candidate.id() == null) {
            return new CandidateResolutionSummary(null, TargetingCandidateStatus.NO_MATCH, 0,
                "Candidato inválido recebido para o request %s".formatted(requestId));
        }
        String primarySeed = sanitizeSeed(firstNonBlank(candidate.seed(), candidate.legacySeed()));
        List<String> variants = resolveVariants(candidate, primarySeed);
        if (variants.isEmpty()) {
            TargetingCandidateResolutionUpdate update = new TargetingCandidateResolutionUpdate(
                TargetingCandidateStatus.NO_MATCH,
                "INVALID_SEED",
                List.of()
            );
            backendClient.reportResolution(candidate.id(), update);
            return new CandidateResolutionSummary(candidate.id(), TargetingCandidateStatus.NO_MATCH, 0,
                "Seed ausente ou inválida");
        }

        TargetingCandidateType type = candidate.tipo() != null ? candidate.tipo() : TargetingCandidateType.INTEREST;
        FacebookAdsService.TargetingSearchType searchType = mapSearchType(type);
        int limit = resolveLimit(request);
        String adAccountId = resolveAdAccountId(request);
        List<String> localeFallbacks = buildFallbacks(
            candidate.idiomaHint(),
            candidate.idioma(),
            request != null ? request.getLocale() : null,
            properties.getDefaultLocale(),
            "en_US",
            null
        );
        List<String> countryFallbacks = buildFallbacks(
            candidate.constraints() != null ? candidate.constraints().country() : null,
            candidate.pais(),
            request != null ? request.getCountry() : null,
            properties.getDefaultCountry(),
            null,
            null
        );
        double aiScore = normalizeScore(candidate.score());

        Map<String, ResolvedOption> resolvedOptions = new LinkedHashMap<>();
        for (String variant : variants.stream().limit(Math.max(1, properties.getMaxSeedVariants())).toList()) {
            SearchParameters parameters = new SearchParameters(variant, searchType, adAccountId, limit);
            SearchOutcome outcome;
            try {
                outcome = searchWithFallbacks(parameters, localeFallbacks, countryFallbacks);
            } catch (RuntimeException ex) {
                LOGGER.error("Failed to resolve targeting candidate {}: {}", candidate.id(), ex.getMessage(), ex);
                return reportApiError(candidate.id());
            }
            if (outcome.hasResults()) {
                mergeOptionsFromOutcome(resolvedOptions, outcome, type, variant, aiScore, TargetingOptionSource.SEARCH);
            }
            if (resolvedOptions.size() >= properties.getResultLimit()) {
                break;
            }
        }

        if (resolvedOptions.isEmpty()) {
            TargetingCandidateResolutionUpdate update = new TargetingCandidateResolutionUpdate(
                TargetingCandidateStatus.NO_MATCH,
                "EMPTY_RESULTS",
                List.of()
            );
            backendClient.reportResolution(candidate.id(), update);
            return new CandidateResolutionSummary(candidate.id(), TargetingCandidateStatus.NO_MATCH, 0,
                "Nenhum resultado retornado pela Meta");
        }

        if (properties.isSuggestionsEnabled()) {
            enrichWithSuggestions(resolvedOptions, type, aiScore, adAccountId, localeFallbacks, countryFallbacks);
        }

        List<TargetingOptionPayload> optionPayloads = resolvedOptions.values().stream()
                .sorted(Comparator.comparingDouble(ResolvedOption::finalScore).reversed())
                .limit(Math.max(1, properties.getResultLimit()))
                .map(this::toPayload)
                .toList();

        TargetingCandidateResolutionUpdate update = new TargetingCandidateResolutionUpdate(
            TargetingCandidateStatus.VALIDATED,
            null,
            optionPayloads
        );
        backendClient.reportResolution(candidate.id(), update);
        return new CandidateResolutionSummary(candidate.id(), TargetingCandidateStatus.VALIDATED, optionPayloads.size(),
            "Opções resolvidas pela Graph API (%d)".formatted(optionPayloads.size()));
    }

    private CandidateResolutionSummary reportApiError(Long candidateId) {
        TargetingCandidateResolutionUpdate update = new TargetingCandidateResolutionUpdate(
            TargetingCandidateStatus.NO_MATCH,
            "API_ERROR",
            List.of()
        );
        backendClient.reportResolution(candidateId, update);
        return new CandidateResolutionSummary(candidateId, TargetingCandidateStatus.NO_MATCH, 0,
            "Erro ao consultar a Graph API");
    }

    private void enrichWithSuggestions(Map<String, ResolvedOption> resolvedOptions,
                                       TargetingCandidateType type,
                                       double aiScore,
                                       String adAccountId,
                                       List<String> localeFallbacks,
                                       List<String> countryFallbacks) {
        if (resolvedOptions.isEmpty()) {
            return;
        }
        List<FacebookAdsService.TargetingSuggestionSeed> seeds = resolvedOptions.values().stream()
                .sorted(Comparator.comparingDouble(ResolvedOption::finalScore).reversed())
                .limit(Math.max(1, properties.getSuggestionSeedLimit()))
                .map(option -> new FacebookAdsService.TargetingSuggestionSeed(resolveSuggestionSeed(option, type), mapSearchType(type).graphType()))
                .toList();
        if (CollectionUtils.isEmpty(seeds)) {
            return;
        }
        String locale = resolveSuggestionLocale(resolvedOptions.values(), localeFallbacks);
        String country = resolveSuggestionCountry(resolvedOptions.values(), countryFallbacks);
        FacebookAdsService.TargetingSuggestionsRequest request = new FacebookAdsService.TargetingSuggestionsRequest(
                adAccountId,
                seeds,
                locale,
                country,
                properties.getSuggestionLimit()
        );
        List<FacebookAdsService.FacebookTargetingSuggestionResult> suggestions = facebookAdsService.suggestTargetingOptions(request);
        if (CollectionUtils.isEmpty(suggestions)) {
            return;
        }
        for (FacebookAdsService.FacebookTargetingSuggestionResult suggestion : suggestions) {
            if (resolvedOptions.containsKey(suggestion.id())) {
                continue;
            }
            double matchScore = resolvedOptions.values().stream()
                    .mapToDouble(option -> computeMatchScore(option.seedVariant(), suggestion.name()))
                    .max()
                    .orElse(0.6);
            double sizeScore = computeSizeScore(suggestion.audienceSize());
            double finalScore = blendScores(aiScore, matchScore, sizeScore);
            ResolvedOption option = new ResolvedOption(
                    new FacebookAdsService.FacebookTargetingSearchResult(
                            suggestion.id(),
                            suggestion.name(),
                            "INTEREST",
                            null,
                            suggestion.audienceSize(),
                            suggestion.audienceSize(),
                            suggestion.path()),
                    type,
                    matchScore,
                    finalScore,
                    locale,
                    country,
                    null,
                    resolvedOptions.values().iterator().next().seedVariant(),
                    TargetingOptionSource.SUGGESTION
            );
            resolvedOptions.put(suggestion.id(), option);
            if (resolvedOptions.size() >= properties.getResultLimit()) {
                break;
            }
        }
    }

    private String resolveSuggestionSeed(ResolvedOption option, TargetingCandidateType type) {
        if (type == TargetingCandidateType.INTEREST) {
            if (StringUtils.hasText(option.result().name())) {
                return option.result().name();
            }
            if (StringUtils.hasText(option.seedVariant())) {
                return option.seedVariant();
            }
        }
        return option.result().id();
    }

    private String resolveSuggestionLocale(Iterable<ResolvedOption> options, List<String> localeFallbacks) {
        for (ResolvedOption option : options) {
            if (StringUtils.hasText(option.locale())) {
                return option.locale();
            }
        }
        return localeFallbacks.isEmpty() ? null : localeFallbacks.get(0);
    }

    private String resolveSuggestionCountry(Iterable<ResolvedOption> options, List<String> countryFallbacks) {
        for (ResolvedOption option : options) {
            if (StringUtils.hasText(option.country())) {
                return option.country();
            }
        }
        return countryFallbacks.isEmpty() ? null : countryFallbacks.get(0);
    }

    private void mergeOptionsFromOutcome(Map<String, ResolvedOption> resolvedOptions,
                                         SearchOutcome outcome,
                                         TargetingCandidateType type,
                                         String seedVariant,
                                         double aiScore,
                                         TargetingOptionSource source) {
        for (FacebookAdsService.FacebookTargetingSearchResult result : outcome.results()) {
            if (resolvedOptions.size() >= properties.getResultLimit()) {
                break;
            }
            if (resolvedOptions.containsKey(result.id())) {
                continue;
            }
            double matchScore = computeMatchScore(seedVariant, result.name());
            double sizeScore = computeSizeScore(result.audienceSize());
            double finalScore = blendScores(aiScore, matchScore, sizeScore);
            ResolvedOption option = new ResolvedOption(
                    result,
                    type,
                    matchScore,
                    finalScore,
                    outcome.locale(),
                    outcome.country(),
                    outcome.term(),
                    seedVariant,
                    source
            );
            resolvedOptions.put(result.id(), option);
        }
    }

    private List<String> resolveVariants(TargetingCandidatePayload candidate, String primarySeed) {
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        if (StringUtils.hasText(primarySeed)) {
            variants.add(primarySeed);
        }
        if (candidate.seedVariants() != null) {
            for (String rawVariant : candidate.seedVariants()) {
                String sanitized = sanitizeSeed(rawVariant);
                if (StringUtils.hasText(sanitized)) {
                    variants.add(sanitized);
                }
            }
        }
        variants.removeIf(value -> !StringUtils.hasText(value));
        if (variants.isEmpty() && StringUtils.hasText(primarySeed)) {
            variants.add(primarySeed);
        }
        return variants.stream().limit(Math.max(1, properties.getMaxSeedVariants())).toList();
    }

    private String sanitizeSeed(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        trimmed = trimmed.replaceAll("\\s+", " ");
        trimmed = trimmed.replaceAll("(?i)\\s+(em|no|na)\\s+[\\p{L}\\s]{2,}$", "");
        trimmed = trimmed.replaceAll("(?i)\\s+(em|no|na)\\s+[A-Z]{2}$", "");
        trimmed = trimmed.replaceAll("\\s*\\([^)]*\\)$", "");
        trimmed = trimmed.replaceAll("[\\p{Punct}]+$", "");
        trimmed = trimmed.trim();
        if (!StringUtils.hasText(trimmed)) {
            return null;
        }
        String[] tokens = trimmed.split(" ");
        if (tokens.length <= 4) {
            return trimmed;
        }
        return String.join(" ", java.util.Arrays.asList(tokens).subList(0, 4));
    }

    private int resolveLimit(TargetingResolutionRequest request) {
        Integer provided = request != null ? request.getLimit() : null;
        int limit = provided != null && provided > 0 ? provided : properties.getSearchLimit();
        return Math.max(1, limit);
    }

    private String resolveAdAccountId(TargetingResolutionRequest request) {
        if (request != null && StringUtils.hasText(request.getAdAccountId())) {
            return request.getAdAccountId().trim();
        }
        return properties.getDefaultAdAccountId();
    }

    private List<String> buildFallbacks(String first, String second, String third, String fourth, String fifth, String sixth) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        addIfHasText(ordered, normalize(first));
        addIfHasText(ordered, normalize(second));
        addIfHasText(ordered, normalize(third));
        addIfHasText(ordered, normalize(fourth));
        addIfHasText(ordered, normalize(fifth));
        addIfHasText(ordered, normalize(sixth));
        if (ordered.isEmpty()) {
            ordered.add(null);
        } else if (!ordered.contains(null)) {
            ordered.add(null);
        }
        return new ArrayList<>(ordered);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void addIfHasText(Set<String> target, String value) {
        if (StringUtils.hasText(value)) {
            target.add(value);
        }
    }

    private SearchOutcome searchWithFallbacks(SearchParameters parameters,
                                              List<String> locales,
                                              List<String> countries) {
        for (String locale : locales) {
            for (String country : countries) {
                FacebookAdsService.TargetingSearchRequest request = new FacebookAdsService.TargetingSearchRequest(
                    parameters.type(),
                    parameters.term(),
                    parameters.adAccountId(),
                    locale,
                    country,
                    parameters.limit()
                );
                List<FacebookAdsService.FacebookTargetingSearchResult> results = facebookAdsService.searchTargetingOptions(request);
                if (!CollectionUtils.isEmpty(results)) {
                    LOGGER.info(
                        "Resolved targeting term '{}' with locale={} country={} ({} resultados)",
                        parameters.term(),
                        locale,
                        country,
                        results.size()
                    );
                    return new SearchOutcome(results, locale, country, parameters.term());
                }
            }
        }
        return SearchOutcome.empty();
    }

    private double computeMatchScore(String query, String resultName) {
        if (!StringUtils.hasText(query) || !StringUtils.hasText(resultName)) {
            return 0d;
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        String normalizedResult = resultName.trim().toLowerCase(Locale.ROOT);

        if (normalizedResult.equals(normalizedQuery)) {
            return 1.0;
        }
        if (normalizedResult.startsWith(normalizedQuery)) {
            return 0.9;
        }
        if (normalizedResult.contains(normalizedQuery)) {
            return 0.75;
        }

        double tokenScore = tokenOverlapScore(normalizedQuery, normalizedResult);
        double distanceScore = levenshteinScore(normalizedQuery, normalizedResult);
        return Math.max(tokenScore, distanceScore * 0.6);
    }

    private double tokenOverlapScore(String a, String b) {
        Set<String> tokensA = new LinkedHashSet<>(List.of(a.split("\\s+")));
        Set<String> tokensB = new LinkedHashSet<>(List.of(b.split("\\s+")));
        tokensA.removeIf(String::isBlank);
        tokensB.removeIf(String::isBlank);
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new LinkedHashSet<>(tokensA);
        intersection.retainAll(tokensB);
        return (double) intersection.size() / (double) Math.min(tokensA.size(), tokensB.size());
    }

    private double levenshteinScore(String a, String b) {
        int distance = levenshteinDistance(a, b);
        int max = Math.max(a.length(), b.length());
        if (max == 0) {
            return 0d;
        }
        double normalized = 1d - ((double) distance / (double) max);
        return Math.max(0d, normalized);
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    private double computeSizeScore(Long audienceSize) {
        if (audienceSize == null || audienceSize <= 0) {
            return 0.5;
        }
        double clamped = Math.min(Math.max(audienceSize.doubleValue(), MIN_AUDIENCE), MAX_AUDIENCE);
        double numerator = Math.log10(clamped) - Math.log10(MIN_AUDIENCE);
        double denominator = Math.log10(MAX_AUDIENCE) - Math.log10(MIN_AUDIENCE);
        double normalized = numerator / denominator;
        return Math.max(0d, Math.min(1d, normalized));
    }

    private double blendScores(double aiScore, double matchScore, double sizeScore) {
        double value = (AI_SCORE_WEIGHT * aiScore) + (MATCH_SCORE_WEIGHT * matchScore) + (SIZE_SCORE_WEIGHT * sizeScore);
        return Math.max(0d, Math.min(1d, value));
    }

    private double normalizeScore(BigDecimal score) {
        if (score == null) {
            return 0.5d;
        }
        double value = score.doubleValue();
        if (value < 0d) {
            return 0d;
        }
        if (value > 1d) {
            return 1d;
        }
        return value;
    }

    private TargetingOptionPayload toPayload(ResolvedOption option) {
        return new TargetingOptionPayload(
            option.result().id(),
            option.result().name(),
            option.type(),
            option.result().audienceSize(),
            toBigDecimal(option.matchScore()),
            toBigDecimal(option.finalScore()),
            option.result().path(),
            option.locale(),
            option.country(),
            option.searchTerm(),
            option.source(),
            option.seedVariant()
        );
    }

    private BigDecimal toBigDecimal(double score) {
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    private FacebookAdsService.TargetingSearchType mapSearchType(TargetingCandidateType candidateType) {
        return switch (candidateType) {
            case BEHAVIOR -> FacebookAdsService.TargetingSearchType.AD_BEHAVIOR;
            case WORK_POSITION -> FacebookAdsService.TargetingSearchType.AD_WORK_POSITION;
            case INTEREST -> FacebookAdsService.TargetingSearchType.AD_INTEREST;
        };
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

    private record ResolvedOption(
        FacebookAdsService.FacebookTargetingSearchResult result,
        TargetingCandidateType type,
        double matchScore,
        double finalScore,
        String locale,
        String country,
        String searchTerm,
        String seedVariant,
        TargetingOptionSource source
    ) {}

    private record SearchParameters(String term,
                                    FacebookAdsService.TargetingSearchType type,
                                    String adAccountId,
                                    int limit) {}

    private record SearchOutcome(List<FacebookAdsService.FacebookTargetingSearchResult> results,
                                 String locale,
                                 String country,
                                 String term) {
        boolean hasResults() {
            return !CollectionUtils.isEmpty(results);
        }

        static SearchOutcome empty() {
            return new SearchOutcome(List.of(), null, null, null);
        }
    }
}
