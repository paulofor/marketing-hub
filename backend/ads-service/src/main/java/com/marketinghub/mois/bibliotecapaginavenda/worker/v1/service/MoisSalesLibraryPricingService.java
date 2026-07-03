package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.openai.service.OpenAiPricingService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Calcula custos de modelos OpenAI da Biblioteca MOIS usando o serviço comum de IA do backend.
 */
@Service
@RequiredArgsConstructor
public class MoisSalesLibraryPricingService {

    private final OpenAiPricingService openAiPricingService;

    /**
     * Estima o custo Flex/batch do modelo usado na análise da página de venda MOIS.
     */
    public BigDecimal estimateBatchCost(String modelCode, Integer inputTokens, Integer outputTokens) {
        return openAiPricingService.estimateFlexCost(modelCode, inputTokens, outputTokens);
    }
}
