package com.marketinghub.creative.service.visual;

/** Expõe à interface somente imagens elegíveis e suas identidades imutáveis de auditoria. */
public record ApprovedVisualAssetOption(
    Long id,
    Long commercialPlanId,
    String assetUrl,
    String label,
    String contentSha256,
    String origin,
    String rightsStatement) {}
