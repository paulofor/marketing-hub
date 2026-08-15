package com.marketinghub.planning.dto;

/** Responsabilidade: receber o vínculo de uma imagem ao kit visual do plano comercial. */
public record CreateCommercialPlanVisualAssetRequest(
    String assetUrl, String label, String purpose, String origin, String rightsStatement) {}
