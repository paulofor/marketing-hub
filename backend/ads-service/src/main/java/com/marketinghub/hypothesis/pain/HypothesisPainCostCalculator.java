package com.marketinghub.hypothesis.pain;

import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.openai.OpenAiModelRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Responsabilidade: calcular e registrar custos flex da etapa Dor do pipeline de hipótese. */
@Component
public class HypothesisPainCostCalculator {
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
    private static final int COST_SCALE = 8;

    private final OpenAiModelRepository openAiModelRepository;
    private final MarketNicheRepository marketNicheRepository;
    private final CurrencyConversionService currencyConversionService;

    /** Inicializa o calculador com catálogo de modelos, repositório de nichos e conversão cambial. */
    public HypothesisPainCostCalculator(
            OpenAiModelRepository openAiModelRepository,
            MarketNicheRepository marketNicheRepository,
            CurrencyConversionService currencyConversionService) {
        this.openAiModelRepository = openAiModelRepository;
        this.marketNicheRepository = marketNicheRepository;
        this.currencyConversionService = currencyConversionService;
    }

    /** Calcula o custo em USD usando preços flex do modelo e tokens consumidos. */
    public BigDecimal calculateFlexCostUsd(String openAiModelCode, Integer inputTokens, Integer outputTokens) {
        if (!StringUtils.hasText(openAiModelCode)) {
            throw new IllegalStateException("Modelo OpenAI ausente para cálculo de custo da etapa Dor");
        }
        OpenAiModel model = openAiModelRepository.findByCode(openAiModelCode.trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Modelo OpenAI não encontrado para cálculo de custo da etapa Dor: " + openAiModelCode));
        BigDecimal inputCost = calculateTokenCost(inputTokens, model.getPriceInputBatch());
        BigDecimal outputCost = calculateTokenCost(outputTokens, model.getPriceOutputBatch());
        return inputCost.add(outputCost).setScale(COST_SCALE, RoundingMode.HALF_UP);
    }

    /** Registra no nicho o delta de custo USD convertido para BRL. */
    public void addFlexCostDeltaToNiche(MarketNiche niche, BigDecimal costDeltaUsd) {
        BigDecimal deltaBrl = currencyConversionService.usdToBrl(costDeltaUsd);
        if (deltaBrl == null || deltaBrl.compareTo(BigDecimal.ZERO) == 0 || niche == null) {
            return;
        }
        niche.setTotalCost(add(niche.getTotalCost(), deltaBrl));
        if (niche.getId() != null) {
            marketNicheRepository.incrementTotalCost(niche.getId(), deltaBrl);
        }
    }

    /** Calcula o custo parcial de uma classe de tokens usando preço por um milhão de tokens. */
    private BigDecimal calculateTokenCost(Integer tokens, BigDecimal pricePerMillionTokens) {
        BigDecimal normalizedTokens = tokens != null ? BigDecimal.valueOf(tokens.longValue()) : BigDecimal.ZERO;
        BigDecimal normalizedPrice = pricePerMillionTokens != null ? pricePerMillionTokens : BigDecimal.ZERO;
        return normalizedTokens.multiply(normalizedPrice).divide(ONE_MILLION, COST_SCALE, RoundingMode.HALF_UP);
    }

    /** Soma valores monetários tratando acumulado nulo como zero. */
    private BigDecimal add(BigDecimal current, BigDecimal delta) {
        if (current == null) {
            return delta;
        }
        return current.add(delta);
    }
}
