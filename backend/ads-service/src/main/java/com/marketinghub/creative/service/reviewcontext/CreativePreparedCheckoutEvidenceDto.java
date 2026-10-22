package com.marketinghub.creative.service.reviewcontext;

import java.math.BigDecimal;

/** Responsabilidade: distinguir checkout candidato conciliado de ativação comercial autorizada. */
public record CreativePreparedCheckoutEvidenceDto(
    String contractVersion,
    String verificationStatus,
    String reviewMode,
    Long productId,
    Long cycleId,
    Long experimentId,
    String productVersion,
    Long slotId,
    String landingUrl,
    String checkoutUrl,
    BigDecimal priceBrl,
    String billingModel,
    boolean activationAuthorized) {}
