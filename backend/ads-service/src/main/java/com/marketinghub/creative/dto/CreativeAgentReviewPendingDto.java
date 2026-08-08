package com.marketinghub.creative.dto;

/**
 * Responsabilidade: transportar ao AI Worker o contexto completo de um anúncio pendente de revisão.
 */
public record CreativeAgentReviewPendingDto(
    Long creativeId,
    Long experimentId,
    String experimentName,
    String niche,
    String hypothesis,
    String format,
    String headline,
    String primaryText,
    String description,
    String cta,
    String destinationUrl,
    String mediaUrl,
    String desireAssociationMapVersion,
    String desireAssociationMapJson) {}
