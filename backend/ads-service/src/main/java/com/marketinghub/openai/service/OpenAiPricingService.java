package com.marketinghub.openai.service;

import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.openai.repository.OpenAiModelRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class OpenAiPricingService {

    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

    private final OpenAiModelRepository modelRepository;

    public OpenAiPricingService(OpenAiModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    public BigDecimal estimateStandardCost(String modelCode, OpenAiResponse.OpenAiUsage usage) {
        return estimateCost(modelCode, usage, PricingMode.STANDARD);
    }

    public BigDecimal estimateBatchCost(String modelCode, OpenAiResponse.OpenAiUsage usage) {
        return estimateCost(modelCode, usage, PricingMode.BATCH);
    }

    private BigDecimal estimateCost(String modelCode, OpenAiResponse.OpenAiUsage usage, PricingMode mode) {
        if (usage == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        OpenAiModel model = modelRepository.findByCode(normalizeModelCode(modelCode))
                .or(() -> modelRepository.findByCode(modelCode))
                .orElseThrow(() -> new IllegalStateException(
                        "Modelo OpenAI não encontrado no catálogo para cálculo de custo: " + modelCode));
        return calculateCost(model, usage, mode);
    }

    private BigDecimal calculateCost(OpenAiModel model, OpenAiResponse.OpenAiUsage usage, PricingMode mode) {
        int inputTokens = usage.effectiveInputTokens() != null ? usage.effectiveInputTokens() : 0;
        int outputTokens = usage.effectiveOutputTokens() != null ? usage.effectiveOutputTokens() : 0;
        BigDecimal inputPrice = mode == PricingMode.BATCH
                ? model.getPriceInputBatch()
                : model.getPriceInputStandard();
        BigDecimal outputPrice = mode == PricingMode.BATCH
                ? model.getPriceOutputBatch()
                : model.getPriceOutputStandard();
        BigDecimal inputCost = multiplyTokens(inputPrice, inputTokens);
        BigDecimal outputCost = multiplyTokens(outputPrice, outputTokens);
        return inputCost.add(outputCost).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal multiplyTokens(BigDecimal pricePerMillion, int tokens) {
        if (pricePerMillion == null || tokens <= 0) {
            return BigDecimal.ZERO;
        }
        return pricePerMillion.multiply(BigDecimal.valueOf(tokens))
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
    }

    private String normalizeModelCode(String modelCode) {
        if (modelCode == null) {
            return null;
        }
        return modelCode.trim().toLowerCase(Locale.ROOT);
    }

    private enum PricingMode {
        STANDARD,
        BATCH
    }
}
