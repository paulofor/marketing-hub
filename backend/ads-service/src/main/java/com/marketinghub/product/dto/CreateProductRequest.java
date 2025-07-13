package com.marketinghub.product.dto;

import lombok.Data;

/**
 * Request body for creating a product.
 */
@Data
public class CreateProductRequest {
    private String niche;
    private String avatar;
    private Long instagramAccountId;
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
