package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.service;

import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesLibraryPricingGateway;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Calcula custos de modelos OpenAI usados pela Biblioteca de Páginas de Vendas do MOIS.
 */
@Service
@RequiredArgsConstructor
public class MoisSalesLibraryPricingService {

    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

    private final MoisSalesLibraryPricingGateway pricingGateway;

    /**
     * Estima o custo batch do modelo usado na análise da página de venda MOIS.
     */
    public BigDecimal estimateBatchCost(String modelCode, Integer inputTokens, Integer outputTokens) {
        MoisSalesLibraryPricingGateway.ModelPricing pricing = pricingGateway.findPricingByModelCode(modelCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Modelo OpenAI não encontrado no catálogo para cálculo de custo MOIS: " + modelCode));
        BigDecimal inputCost = multiplyTokens(pricing.priceInputBatch(), inputTokens);
        BigDecimal outputCost = multiplyTokens(pricing.priceOutputBatch(), outputTokens);
        return inputCost.add(outputCost).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Multiplica o preço por milhão pela quantidade de tokens informada.
     */
    private BigDecimal multiplyTokens(BigDecimal pricePerMillion, Integer tokens) {
        if (pricePerMillion == null || tokens == null || tokens <= 0) {
            return BigDecimal.ZERO;
        }
        return pricePerMillion.multiply(BigDecimal.valueOf(tokens))
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
    }
}
