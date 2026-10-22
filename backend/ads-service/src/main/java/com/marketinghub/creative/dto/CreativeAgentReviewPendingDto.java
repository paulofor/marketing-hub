package com.marketinghub.creative.dto;

import com.marketinghub.creative.service.reviewcontext.CreativePreparedCheckoutEvidenceDto;
import com.marketinghub.creative.service.reviewcontext.CreativeStaticUiEvidenceDto;

/**
 * Responsabilidade: transportar ao executor de revisão o contexto completo de um anúncio pendente
 * de revisão.
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
    String commercialCheckoutUrl,
    String mediaUrl,
    CreativeCommercialLineageEvidenceDto commercialLineageEvidence,
    CreativeMediaGovernanceEvidenceDto mediaGovernanceEvidence,
    String desireAssociationMapVersion,
    String desireAssociationMapJson,
    CreativeStaticUiEvidenceDto staticUiProof,
    CreativePreparedCheckoutEvidenceDto preparedCheckoutEvidence) {
  /** Preserva os consumidores anteriores que não utilizam provas de preparação estática. */
  public CreativeAgentReviewPendingDto(
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
      String commercialCheckoutUrl,
      String mediaUrl,
      CreativeCommercialLineageEvidenceDto lineage,
      CreativeMediaGovernanceEvidenceDto governance,
      String desireMapVersion,
      String desireMapJson) {
    this(
        creativeId,
        experimentId,
        experimentName,
        niche,
        hypothesis,
        format,
        headline,
        primaryText,
        description,
        cta,
        destinationUrl,
        commercialCheckoutUrl,
        mediaUrl,
        lineage,
        governance,
        desireMapVersion,
        desireMapJson,
        null,
        null);
  }
}
