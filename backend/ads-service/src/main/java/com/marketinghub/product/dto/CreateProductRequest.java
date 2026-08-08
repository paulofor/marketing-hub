package com.marketinghub.product.dto;

import lombok.Data;

/**
 * Responsabilidade: transportar os dados necessários para cadastrar ou atualizar um produto
 * comercial.
 */
@Data
public class CreateProductRequest {
  private String slug;
  private String name;
  private String publicUrl;
  private String logoUrl;
  private String colorPalette;
  private String targetAudience;
  private String languageStyle;
  private String codeModules;
  private String productType;
  private String productFormat;
  private String deliveryMode;
  private String revenueModel;
  private String valueUnit;
  private String valueEvidenceMetric;
  private String validationDefinitionVersion;
  private String validationDefinitionJson;
  private String desireAssociationMapVersion;
  private String desireAssociationMapJson;
  private String commercialStatus;
  private java.math.BigDecimal currentPriceBrl;
  private java.util.UUID primaryHypothesisId;
  private String primaryHypothesis;
  private String associatedExperiments;
  private String commercialNotes;
  private String sevenDayJourney;
  private String supportMaterialPositioning;
  private String primaryCta;
  private String niche;
  private String avatar;
  private Long instagramAccountId;
  private Long marketNicheId;
  private String explicitPain;
  private String promise;
  private String uniqueMechanism;
  private String tripwire;
  private String riskReversal;
  private String socialProof;
  private String scientificEvidencePack;
  private String pdeExperienceJson;
  private String checkoutMonetization;
  private String funnel;
  private String creativeVolume;
  private String storytelling;
  private java.math.BigDecimal aiCost;
}
