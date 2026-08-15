package com.marketinghub.planning.dto;

/** Responsabilidade: receber o vínculo de uma imagem ou vídeo à biblioteca do plano comercial. */
public record CreateCommercialPlanVisualAssetRequest(
    String assetUrl,
    String mediaType,
    String label,
    String purpose,
    String origin,
    String rightsStatement) {}
