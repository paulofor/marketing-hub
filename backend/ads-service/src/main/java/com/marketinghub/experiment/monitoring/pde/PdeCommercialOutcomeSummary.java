package com.marketinghub.experiment.monitoring.pde;

import java.math.BigDecimal;

/**
 * Responsabilidade: resumir vendas, reembolsos e satisfação atribuídos ao experimento sem expor
 * identificadores financeiros.
 */
public record PdeCommercialOutcomeSummary(
    long purchases,
    long refunds,
    BigDecimal grossRevenueBrl,
    BigDecimal refundedRevenueBrl,
    BigDecimal netRevenueBrl,
    boolean financialReferencesComplete,
    boolean financialAmountsComplete,
    boolean refundReferencesMatchPurchases,
    boolean valueReferencesComplete,
    long accessReleasedNetSales,
    long deliveredNetSales,
    long firstUseNetSales,
    long satisfactionResponses,
    long positiveSatisfactionResponses) {}
