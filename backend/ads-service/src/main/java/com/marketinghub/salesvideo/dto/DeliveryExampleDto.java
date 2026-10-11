package com.marketinghub.salesvideo.dto;

/** Expõe proveniência da amostra aprovada sem entidades da biblioteca na borda HTTP. */
public record DeliveryExampleDto(
    Long visualAssetId,
    Long commercialPlanId,
    String label,
    String url,
    String sha256,
    String origin,
    String rightsStatement) {}
