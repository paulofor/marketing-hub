package com.marketinghub.product.dto;

import lombok.Data;

/**
 * Responsabilidade: transportar os dados necessários para cadastrar um produto comercial.
 */
@Data
public class CreateProductRequest {
    private String slug;
    private String name;
    private String publicUrl;
    private String colorPalette;
    private String targetAudience;
    private String languageStyle;
    private String codeModules;
    private String productType;
    private String commercialStatus;
    private java.math.BigDecimal currentPriceBrl;
    private java.util.UUID primaryHypothesisId;
    private String primaryHypothesis;
    private String associatedExperiments;
    private String commercialNotes;
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
    private String checkoutMonetization;
    private String funnel;
    private String creativeVolume;
    private String storytelling;
    private java.math.BigDecimal aiCost;
}
