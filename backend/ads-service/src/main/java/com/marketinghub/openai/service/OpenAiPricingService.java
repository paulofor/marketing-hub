package com.marketinghub.openai.service;

import com.marketinghub.openai.OpenAiCostEstimator;
import com.marketinghub.openai.OpenAiModel;
import com.marketinghub.openai.OpenAiResponse;
import com.marketinghub.openai.repository.OpenAiModelRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class OpenAiPricingService {

    private static final BigDecimal ONE_THOUSAND = BigDecimal.valueOf(1000);

    private final OpenAiModelRepository modelRepository;

    public OpenAiPricingService(OpenAiModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    public BigDecimal estimateBatchCost(String modelCode, OpenAiResponse.OpenAiUsage usage) {
        if (usage == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return modelRepository.findByCode(modelCode)
                .map(model -> calculateCost(model, usage))
                .orElse(OpenAiCostEstimator.estimateUsd(modelCode, usage));
    }

    private BigDecimal calculateCost(OpenAiModel model, OpenAiResponse.OpenAiUsage usage) {
        int inputTokens = usage.effectiveInputTokens() != null ? usage.effectiveInputTokens() : 0;
        int outputTokens = usage.effectiveOutputTokens() != null ? usage.effectiveOutputTokens() : 0;
        BigDecimal inputCost = multiplyTokens(model.getPriceInputBatch(), inputTokens);
        BigDecimal outputCost = multiplyTokens(model.getPriceOutputBatch(), outputTokens);
        return inputCost.add(outputCost).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal multiplyTokens(BigDecimal pricePerThousand, int tokens) {
        if (pricePerThousand == null || tokens <= 0) {
            return BigDecimal.ZERO;
        }
        return pricePerThousand.multiply(BigDecimal.valueOf(tokens))
                .divide(ONE_THOUSAND, 6, RoundingMode.HALF_UP);
    }
}
