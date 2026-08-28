package com.marketinghub.agenttask;

import java.math.BigDecimal;

/** Responsabilidade: identificar o produto e a versão que um executor deve avaliar. */
public record AgentTaskTargetResponse(
    String sourceReference,
    Long experimentId,
    Long productId,
    String productSlug,
    String productName,
    String productInternalName,
    String experienceVersion,
    String publicUrl,
    String commercialCheckoutUrl,
    BigDecimal unitPriceBrl) {}
