package com.marketinghub.successproduct.dto;

import com.marketinghub.successproduct.SuccessProductPlatform;
import lombok.Data;

/**
 * Request body for updating a success product.
 */
@Data
public class UpdateSuccessProductRequest {
    private String description;
    private String name;
    private Boolean novo;
    private Boolean generateNicheHypothesis;
    private String niche;
    private String avatar;
    private SuccessProductPlatform platform;
    private String audienceType;
    private String salesPageUrl;
    private String instagramUrl;
    private String facebookUrl;
    private String youtubeUrl;
    private Long instagramAccountId;
    private String explicitPain;
    private String promise;
    private String uniqueMechanism;
    private String tripwire;
    private String riskReversal;
    private String socialProof;
    private String checkoutMonetization;
    private String salesFunnel;
    private String creativeVolume;
    private String storytelling;
}
