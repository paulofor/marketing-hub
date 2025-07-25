package com.marketinghub.successproduct.dto;

import java.time.Instant;
import lombok.Data;

/**
 * Data transfer object for {@link com.marketinghub.successproduct.SuccessProduct}.
 */
@Data
public class SuccessProductDto {
    private Long id;
    private String description;
    private String name;
    private boolean novo;
    private String niche;
    private String avatar;
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
    private String funnel;
    private String creativeVolume;
    private String storytelling;
    private Instant createdAt;
    private Instant updatedAt;
}
