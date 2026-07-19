package com.marketinghub.product.dto;

import java.time.Instant;
import lombok.Data;

/**
 * Responsabilidade: expor os dados do produto comercial para o frontend.
 */
@Data
public class ProductDto {
    private Long id;
    private String slug;
    private String name;
    private String publicUrl;
    private String logoUrl;
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
    private String checkoutMonetization;
    private String funnel;
    private String creativeVolume;
    private String storytelling;
    private java.math.BigDecimal aiCost;
    private Instant createdAt;
    private Instant updatedAt;
}
