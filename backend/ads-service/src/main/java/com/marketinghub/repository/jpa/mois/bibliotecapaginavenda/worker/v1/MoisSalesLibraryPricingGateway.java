package com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Define a leitura dos preços de modelos usados no cálculo de custo da Biblioteca MOIS.
 */
public interface MoisSalesLibraryPricingGateway {

    /**
     * Busca os preços batch do modelo pelo código informado pelo worker.
     */
    Optional<ModelPricing> findPricingByModelCode(String modelCode);

    /**
     * Representa os preços por milhão necessários para calcular custo batch.
     */
    record ModelPricing(BigDecimal priceInputBatch, BigDecimal priceOutputBatch) {
    }
}
