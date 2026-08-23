package com.marketinghub.product.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * Responsabilidade: transportar os dados necessários para cadastrar ou atualizar um produto
 * comercial.
 */
@Data
public class CreateProductRequest {
  @Size(max = 191)
  private String slug;

  @Size(max = 191)
  private String name;

  @Size(max = 191)
  private String internalName;

  @Size(max = 20)
  private List<@Size(max = 191) String> aliases;

  @Size(max = 512)
  private String publicUrl;

  @Size(max = 512)
  private String logoUrl;

  private String colorPalette;
  private String targetAudience;
  private String languageStyle;
  private String codeModules;

  @Size(max = 64)
  private String productType;

  private Long productTypeId;

  @Size(max = 64)
  private String productFormat;

  @Size(max = 64)
  private String deliveryMode;

  @Size(max = 64)
  private String revenueModel;

  @Size(max = 191)
  private String valueUnit;

  @Size(max = 191)
  private String valueEvidenceMetric;

  @Size(max = 32)
  private String validationDefinitionVersion;

  private String validationDefinitionJson;

  @Size(max = 32)
  private String desireAssociationMapVersion;

  private String desireAssociationMapJson;

  @Size(max = 64)
  private String commercialStatus;

  private java.math.BigDecimal currentPriceBrl;
  private java.util.UUID primaryHypothesisId;
  private String primaryHypothesis;
  private String associatedExperiments;
  private String commercialNotes;
  private String sevenDayJourney;
  private String supportMaterialPositioning;

  @Size(max = 191)
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
