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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serviço responsável por aterrar os candidatos em opções válidas da Meta.
 */
@Service
public class TargetingResolverService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TargetingResolverService.class);

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
        String term = candidate.textoSugerido();
        if (!StringUtils.hasText(term)) {
            TargetingCandidateResolutionUpdate update = new TargetingCandidateResolutionUpdate(
                TargetingCandidateStatus.NO_MATCH,
                "Texto sugerido não foi informado",
                List.of()
            );
            backendClient.reportResolution(candidate.id(), update);
            return new CandidateResolutionSummary(candidate.id(), TargetingCandidateStatus.NO_MATCH, 0,
                "Texto sugerido vazio");
        }

        TargetingCandidateType type = candidate.tipo() != null ? candidate.tipo() : TargetingCandidateType.INTEREST;
        FacebookAdsService.TargetingSearchType searchType = mapSearchType(type);
        int limit = resolveLimit(request);
        String adAccountId = resolveAdAccountId(request);

        SearchParameters searchParameters = new SearchParameters(term.trim(), searchType, adAccountId, limit);
        List<String> localeFallbacks = buildFallbacks(candidate.idioma(), request != null ? request.getLocale() : null, properties.getDefaultLocale(), "en_US", (String) null);
        List<String> countryFallbacks = buildFallbacks(candidate.pais(), request != null ? request.getCountry() : null, properties.getDefaultCountry(), (String) null);

        SearchOutcome outcome;
        try {
            outcome = searchWithFallbacks(searchParameters, localeFallbacks, countryFallbacks);
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to resolve targeting candidate {}: {}", candidate.id(), ex.getMessage(), ex);
            TargetingCandidateResolutionUpdate update = new TargetingCandidateResolutionUpdate(
                TargetingCandidateStatus.NO_MATCH,
                "Erro ao consultar a Graph API: " + ex.getMessage(),
                List.of()
            );
            backendClient.reportResolution(candidate.id(), update);
            return new CandidateResolutionSummary(candidate.id(), TargetingCandidateStatus.NO_MATCH, 0,
                "Erro ao consultar a Graph API");
        }

        if (outcome == null || outcome.results().isEmpty()) {
            TargetingCandidateResolutionUpdate update = new TargetingCandidateResolutionUpdate(
                TargetingCandidateStatus.NO_MATCH,
                "Nenhuma opção encontrada na Meta para o termo informado",
                List.of()
            );
            backendClient.reportResolution(candidate.id(), update);
            return new CandidateResolutionSummary(candidate.id(), TargetingCandidateStatus.NO_MATCH, 0,
                "Nenhum resultado retornado pela Meta");
        }

        List<TargetingOptionPayload> options = toOptionPayloads(outcome, type, term, properties.getResultLimit());
        TargetingCandidateResolutionUpdate update = new TargetingCandidateResolutionUpdate(
            TargetingCandidateStatus.VALIDATED,
            null,
            options
        );
        backendClient.reportResolution(candidate.id(), update);
        return new CandidateResolutionSummary(candidate.id(), TargetingCandidateStatus.VALIDATED, options.size(),
            "Opções resolvidas pela Graph API (%d)".formatted(options.size()));
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

    private List<String> buildFallbacks(String first, String second, String third, String... others) {
        Set<String> ordered = new LinkedHashSet<>();
        addIfHasText(ordered, normalize(first));
        addIfHasText(ordered, normalize(second));
        addIfHasText(ordered, normalize(third));
        if (others != null) {
            for (String other : others) {
                addIfHasText(ordered, normalize(other));
            }
        }
        // se todos forem vazios, precisamos garantir que existe um elemento null
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
        if (value == null) {
            return;
        }
        if (value.isEmpty()) {
            return;
        }
        target.add(value);
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
                    return new SearchOutcome(results, locale, country);
                }
            }
        }
        return SearchOutcome.empty();
    }

    private List<TargetingOptionPayload> toOptionPayloads(SearchOutcome outcome,
                                                          TargetingCandidateType type,
                                                          String term,
                                                          int limit) {
        List<TargetingOptionPayload> options = new ArrayList<>();
        AtomicInteger index = new AtomicInteger();
        for (FacebookAdsService.FacebookTargetingSearchResult result : outcome.results()) {
            if (index.incrementAndGet() > Math.max(1, limit)) {
                break;
            }
            double score = computeMatchScore(term, result.name());
            options.add(new TargetingOptionPayload(
                result.id(),
                result.name(),
                type,
                result.audienceSize(),
                toBigDecimal(score),
                result.path(),
                outcome.locale(),
                outcome.country(),
                term
            ));
        }
        return options;
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

    private BigDecimal toBigDecimal(double score) {
        BigDecimal value = BigDecimal.valueOf(score);
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private FacebookAdsService.TargetingSearchType mapSearchType(TargetingCandidateType candidateType) {
        return switch (candidateType) {
            case BEHAVIOR -> FacebookAdsService.TargetingSearchType.AD_BEHAVIOR;
            case WORK_POSITION -> FacebookAdsService.TargetingSearchType.AD_WORK_POSITION;
            case INTEREST -> FacebookAdsService.TargetingSearchType.AD_INTEREST;
        };
    }

    private record SearchParameters(String term,
                                    FacebookAdsService.TargetingSearchType type,
                                    String adAccountId,
                                    int limit) {}

    private record SearchOutcome(List<FacebookAdsService.FacebookTargetingSearchResult> results,
                                 String locale,
                                 String country) {
        public static SearchOutcome empty() {
            return new SearchOutcome(List.of(), null, null);
        }
    }
}
