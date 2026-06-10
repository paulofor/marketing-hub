package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos.MarketWarmupCompleteRequest;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos.MarketWarmupEcosystemType;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos.MarketWarmupRecommendation;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos.MarketWarmupSignalCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos.MarketWarmupSignalType;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos.MarketWarmupSourceCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos.MarketWarmupSourceType;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos.MarketWarmupSummaryCompleteItem;
import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.dto.MoisSalesLibraryDtos.MarketWarmupTemperature;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Calcula score, temperatura, ecossistema e recomendação da pesquisa de aquecimento de mercado MOIS.
 */
class MoisSalesPageMarketWarmupScoreEngine {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal TEN = BigDecimal.TEN;
    private static final BigDecimal SATURATION_THRESHOLD = BigDecimal.valueOf(8);

    /**
     * Recalcula o resumo final usando fontes e sinais rastreáveis como base explicável do score.
     */
    MarketWarmupSummaryCompleteItem calculate(MarketWarmupCompleteRequest request) {
        DimensionScores dimensions = calculateDimensions(request.sources(), request.signals());
        BigDecimal score = clamp(dimensions.totalBeforeClamp(), ZERO, BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
        MarketWarmupTemperature temperature = classifyTemperature(score, dimensions.saturationPenalty());
        MarketWarmupRecommendation recommendation = classifyRecommendation(score, dimensions.saturationPenalty());
        MarketWarmupEcosystemType ecosystemType = classifyEcosystem(request.sources(), request.signals(), dimensions.saturationPenalty());
        MarketWarmupSummaryCompleteItem base = request.summary();
        return new MarketWarmupSummaryCompleteItem(
                score,
                temperature,
                ecosystemType,
                recommendation,
                base.mainPains(),
                base.mainObjections(),
                base.mainPromises(),
                base.mainChannels(),
                base.mainCompetitors(),
                chooseText(base.saturationRisk(), saturationText(dimensions.saturationPenalty())),
                chooseText(base.opportunityRecommendation(), recommendationText(recommendation)),
                chooseText(base.nextExperimentSuggestion(), experimentSuggestion(recommendation, ecosystemType)));
    }

    /**
     * Soma as dimensões comerciais canônicas do Market Warm-up Score.
     */
    private DimensionScores calculateDimensions(
            List<MarketWarmupSourceCompleteItem> sources, List<MarketWarmupSignalCompleteItem> signals) {
        BigDecimal activity = averageSourceScore(sources, SourceScoreKind.RECENCY).multiply(BigDecimal.valueOf(2));
        BigDecimal creatorDensity = calculateCreatorDensity(sources, signals);
        BigDecimal engagement = max(
                averageSourceScore(sources, SourceScoreKind.ENGAGEMENT).multiply(BigDecimal.valueOf(2)),
                averageSignalStrength(
                                signals,
                                Set.of(
                                        MarketWarmupSignalType.BUYING_INTENT,
                                        MarketWarmupSignalType.COMMUNITY_ACTIVITY,
                                        MarketWarmupSignalType.CHANNEL_FIT))
                        .multiply(BigDecimal.valueOf(2)));
        BigDecimal explicitPain = averageSignalStrength(
                        signals, Set.of(MarketWarmupSignalType.PAIN_EXPLICIT, MarketWarmupSignalType.OBJECTION))
                .multiply(BigDecimal.valueOf(1.5));
        BigDecimal offerMaturity = calculateOfferMaturity(sources, signals);
        BigDecimal socialProof = averageSignalStrength(
                signals, Set.of(MarketWarmupSignalType.SOCIAL_PROOF, MarketWarmupSignalType.CREATOR_AUTHORITY));
        BigDecimal saturationPenalty = calculateSaturationPenalty(sources, signals);
        BigDecimal total = clamp(activity, ZERO, BigDecimal.valueOf(20))
                .add(clamp(creatorDensity, ZERO, BigDecimal.valueOf(15)))
                .add(clamp(engagement, ZERO, BigDecimal.valueOf(20)))
                .add(clamp(explicitPain, ZERO, BigDecimal.valueOf(15)))
                .add(clamp(offerMaturity, ZERO, BigDecimal.valueOf(10)))
                .add(clamp(socialProof, ZERO, BigDecimal.valueOf(10)))
                .subtract(clamp(saturationPenalty, ZERO, TEN));
        return new DimensionScores(total, clamp(saturationPenalty, ZERO, TEN));
    }

    /**
     * Calcula densidade de criadores e especialistas pelo volume de fontes e sinais de autoridade.
     */
    private BigDecimal calculateCreatorDensity(
            List<MarketWarmupSourceCompleteItem> sources, List<MarketWarmupSignalCompleteItem> signals) {
        long creatorSources = sources.stream()
                .filter(source -> Set.of(
                                MarketWarmupSourceType.CREATOR_CONTENT,
                                MarketWarmupSourceType.SPECIALIST_CONTENT,
                                MarketWarmupSourceType.SOCIAL_POST,
                                MarketWarmupSourceType.AFFILIATE_PROMOTION)
                        .contains(source.sourceType()))
                .count();
        BigDecimal authority = averageSignalStrength(signals, Set.of(MarketWarmupSignalType.CREATOR_AUTHORITY)).multiply(BigDecimal.valueOf(0.5));
        return BigDecimal.valueOf(creatorSources).multiply(BigDecimal.valueOf(3)).add(authority);
    }

    /**
     * Calcula maturidade de oferta por presença de concorrentes, afiliados, reviews e prova social.
     */
    private BigDecimal calculateOfferMaturity(
            List<MarketWarmupSourceCompleteItem> sources, List<MarketWarmupSignalCompleteItem> signals) {
        long offerSources = sources.stream()
                .filter(source -> Set.of(
                                MarketWarmupSourceType.PRODUCT_PRESENCE,
                                MarketWarmupSourceType.COMPETITOR_OFFER,
                                MarketWarmupSourceType.AFFILIATE_PROMOTION,
                                MarketWarmupSourceType.REVIEW)
                        .contains(source.sourceType()))
                .count();
        BigDecimal signalStrength = averageSignalStrength(signals, Set.of(MarketWarmupSignalType.COMPETITOR_OFFER, MarketWarmupSignalType.SOCIAL_PROOF));
        return BigDecimal.valueOf(offerSources).multiply(BigDecimal.valueOf(2)).add(signalStrength.multiply(BigDecimal.valueOf(0.6)));
    }

    /**
     * Calcula penalidade de saturação por sinais explícitos e excesso de fontes de concorrência.
     */
    private BigDecimal calculateSaturationPenalty(
            List<MarketWarmupSourceCompleteItem> sources, List<MarketWarmupSignalCompleteItem> signals) {
        BigDecimal explicitSaturation = averageSignalStrength(signals, Set.of(MarketWarmupSignalType.SATURATION_RISK));
        long competitorSources = sources.stream()
                .filter(source -> Set.of(MarketWarmupSourceType.COMPETITOR_OFFER, MarketWarmupSourceType.AFFILIATE_PROMOTION).contains(source.sourceType()))
                .count();
        BigDecimal competitionPressure = BigDecimal.valueOf(Math.max(0, competitorSources - 2)).multiply(BigDecimal.valueOf(1.5));
        return max(explicitSaturation, competitionPressure);
    }

    /**
     * Classifica a temperatura comercial seguindo faixas canônicas e bloqueio de saturação alta.
     */
    private MarketWarmupTemperature classifyTemperature(BigDecimal score, BigDecimal saturationPenalty) {
        if (saturationPenalty.compareTo(SATURATION_THRESHOLD) >= 0) {
            return MarketWarmupTemperature.SATURATED;
        }
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return MarketWarmupTemperature.HOT;
        }
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return MarketWarmupTemperature.PROMISING;
        }
        if (score.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return MarketWarmupTemperature.WARM;
        }
        return MarketWarmupTemperature.COLD;
    }

    /**
     * Classifica a recomendação objetiva para decisão comercial da página analisada.
     */
    private MarketWarmupRecommendation classifyRecommendation(BigDecimal score, BigDecimal saturationPenalty) {
        if (saturationPenalty.compareTo(SATURATION_THRESHOLD) >= 0) {
            return MarketWarmupRecommendation.SATURATED_REQUIRES_ANGLE;
        }
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return MarketWarmupRecommendation.PRIORITIZE;
        }
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return MarketWarmupRecommendation.OBSERVE;
        }
        if (score.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return MarketWarmupRecommendation.RESEARCH_MORE;
        }
        return MarketWarmupRecommendation.DISCARD;
    }

    /**
     * Classifica o ecossistema dominante pela força média dos sinais e tipos das fontes encontradas.
     */
    private MarketWarmupEcosystemType classifyEcosystem(
            List<MarketWarmupSourceCompleteItem> sources,
            List<MarketWarmupSignalCompleteItem> signals,
            BigDecimal saturationPenalty) {
        if (saturationPenalty.compareTo(SATURATION_THRESHOLD) >= 0) {
            return MarketWarmupEcosystemType.SATURATED;
        }
        Map<MarketWarmupEcosystemType, BigDecimal> strengths = new EnumMap<>(MarketWarmupEcosystemType.class);
        strengths.put(MarketWarmupEcosystemType.SPECIALISTS_HEATED, sourceTypeCount(sources, MarketWarmupSourceType.SPECIALIST_CONTENT).multiply(BigDecimal.valueOf(2))
                .add(averageSignalStrength(signals, Set.of(MarketWarmupSignalType.CREATOR_AUTHORITY))));
        strengths.put(MarketWarmupEcosystemType.CREATORS_HEATED, sourceTypeCount(sources, MarketWarmupSourceType.CREATOR_CONTENT, MarketWarmupSourceType.SOCIAL_POST).multiply(BigDecimal.valueOf(2))
                .add(averageSignalStrength(signals, Set.of(MarketWarmupSignalType.CHANNEL_FIT))));
        strengths.put(MarketWarmupEcosystemType.RECURRING_PAIN_HEATED, sourceTypeCount(sources, MarketWarmupSourceType.COMMUNITY_DISCUSSION, MarketWarmupSourceType.COMPLAINT).multiply(BigDecimal.valueOf(2))
                .add(averageSignalStrength(signals, Set.of(MarketWarmupSignalType.PAIN_EXPLICIT, MarketWarmupSignalType.COMMUNITY_ACTIVITY))));
        strengths.put(MarketWarmupEcosystemType.COMPETITORS_HEATED, sourceTypeCount(sources, MarketWarmupSourceType.COMPETITOR_OFFER, MarketWarmupSourceType.AFFILIATE_PROMOTION).multiply(BigDecimal.valueOf(2))
                .add(averageSignalStrength(signals, Set.of(MarketWarmupSignalType.COMPETITOR_OFFER))));
        return strengths.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .filter(entry -> entry.getValue().compareTo(BigDecimal.valueOf(3)) >= 0)
                .map(Map.Entry::getKey)
                .orElse(MarketWarmupEcosystemType.COLD_OR_UNEDUCATED);
    }

    /**
     * Calcula a média de recência ou engajamento das fontes em escala de zero a dez.
     */
    private BigDecimal averageSourceScore(List<MarketWarmupSourceCompleteItem> sources, SourceScoreKind kind) {
        List<BigDecimal> values = sources.stream()
                .map(source -> kind == SourceScoreKind.RECENCY ? source.recencyScore() : source.engagementScore())
                .filter(value -> value != null)
                .map(value -> clamp(value, ZERO, TEN))
                .toList();
        return average(values);
    }

    /**
     * Calcula a força média dos sinais de tipos comerciais selecionados em escala de zero a dez.
     */
    private BigDecimal averageSignalStrength(
            List<MarketWarmupSignalCompleteItem> signals, Set<MarketWarmupSignalType> signalTypes) {
        List<BigDecimal> values = signals.stream()
                .filter(signal -> signalTypes.contains(signal.signalType()))
                .map(MarketWarmupSignalCompleteItem::signalStrength)
                .filter(value -> value != null)
                .map(value -> clamp(value, ZERO, TEN))
                .toList();
        return average(values);
    }

    /**
     * Conta fontes de tipos selecionados para inferir predominância de ecossistema.
     */
    private BigDecimal sourceTypeCount(
            List<MarketWarmupSourceCompleteItem> sources, MarketWarmupSourceType... sourceTypes) {
        Set<MarketWarmupSourceType> acceptedTypes = Set.of(sourceTypes);
        return BigDecimal.valueOf(sources.stream().filter(source -> acceptedTypes.contains(source.sourceType())).count());
    }

    /**
     * Calcula média decimal segura para coleções vazias.
     */
    private BigDecimal average(Collection<BigDecimal> values) {
        if (values.isEmpty()) {
            return ZERO;
        }
        BigDecimal total = values.stream().reduce(ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * Mantém valores numéricos dentro dos limites da dimensão comercial.
     */
    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null || value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }

    /**
     * Retorna o maior valor entre duas dimensões calculadas.
     */
    private BigDecimal max(BigDecimal first, BigDecimal second) {
        return first.compareTo(second) >= 0 ? first : second;
    }

    /**
     * Usa texto recebido pelo worker quando existe, ou texto padrão do motor quando está vazio.
     */
    private String chooseText(String received, String fallback) {
        return Optional.ofNullable(received)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElse(fallback);
    }

    /**
     * Descreve a saturação de forma simples para a UI comercial.
     */
    private String saturationText(BigDecimal saturationPenalty) {
        if (saturationPenalty.compareTo(SATURATION_THRESHOLD) >= 0) {
            return "alto";
        }
        if (saturationPenalty.compareTo(BigDecimal.valueOf(4)) >= 0) {
            return "moderado";
        }
        return "baixo";
    }

    /**
     * Gera recomendação objetiva quando o worker não trouxe texto funcional próprio.
     */
    private String recommendationText(MarketWarmupRecommendation recommendation) {
        return switch (recommendation) {
            case PRIORITIZE -> "Priorizar experimento comercial com este mercado.";
            case OBSERVE -> "Observar e refinar o ângulo antes de escalar.";
            case RESEARCH_MORE -> "Pesquisar mais fontes antes de criar oferta.";
            case DISCARD -> "Descartar ou deixar em baixa prioridade.";
            case SATURATED_REQUIRES_ANGLE -> "Avançar somente com ângulo claramente diferenciado.";
        };
    }

    /**
     * Gera próximo passo comercial quando o worker não trouxe sugestão específica.
     */
    private String experimentSuggestion(MarketWarmupRecommendation recommendation, MarketWarmupEcosystemType ecosystemType) {
        String ecosystem = ecosystemType.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return switch (recommendation) {
            case PRIORITIZE -> "Criar experimento inicial apoiado no ecossistema " + ecosystem + ".";
            case OBSERVE -> "Validar novo ângulo com mais sinais do ecossistema " + ecosystem + ".";
            case RESEARCH_MORE -> "Coletar fontes adicionais antes do experimento.";
            case DISCARD -> "Não criar experimento neste momento.";
            case SATURATED_REQUIRES_ANGLE -> "Buscar promessa e mecanismo menos comoditizados antes de vender.";
        };
    }

    /**
     * Guarda dimensões intermediárias necessárias para decisão de temperatura e saturação.
     */
    private record DimensionScores(BigDecimal totalBeforeClamp, BigDecimal saturationPenalty) {
    }

    /**
     * Diferencia qual nota operacional da fonte deve entrar na dimensão calculada.
     */
    private enum SourceScoreKind {
        RECENCY,
        ENGAGEMENT
    }
}
