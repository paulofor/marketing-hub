package com.marketinghub.product.dto;

import java.time.Instant;
import lombok.Data;

/**
 * Data transfer object for {@link com.marketinghub.product.Product}.
 */
@Data
public class ProductDto {
    private Long id;
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
    private Instant createdAt;
    private Instant updatedAt;
}
