package com.marketinghub.pde.dto;

import java.math.BigDecimal;

/** Espelha a oferta comercial pública entregue pelo Marketing Hub ao frontend PDE. */
public record CommercialOfferResponse(
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
