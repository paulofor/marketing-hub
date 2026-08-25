package com.marketinghub.product.service.commercialoffer;

import java.math.BigDecimal;

/** Contrato público da oferta comercial vinculada a um slot PDE validável. */
public record PublicProductCommercialOfferResponse(
    String productSlug,
    String experienceVersion,
    String layoutKey,
    Long experimentId,
    String experimentStatus,
    String acquisitionChannel,
    String pain,
    String proof,
    String promise,
    String primaryCta,
    BigDecimal priceBrl,
    String checkoutUrl,
    String salesPageUrl,
    String targetAudience,
    String productFormat,
    String deliveryMode,
    String valueUnit,
    String supplierLegalName,
    String supplierRegistrationNumber,
    String supplierAddress,
    String supportEmail,
    String termsUrl,
    String privacyUrl,
    String refundPolicyUrl) {}
