package com.marketinghub.imagegenerator.dto;

/** Responsabilidade: informar o asset persistido e o slot atualizado na landing em rascunho. */
public record LandingImagePromotionResponse(
    Long experimentId, String jobId, String slotId, String assetUrl) {}
