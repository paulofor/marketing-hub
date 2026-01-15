package com.marketinghub.niche.description.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO para {@link com.marketinghub.niche.description.NicheDetailedDescription}.
 */
@Data
public class NicheDetailedDescriptionDto {
    private Long id;
    private Long marketNicheId;
    private Long promptId;
    private String promptName;
    private String title;
    private String description;
    private String pains;
    private String desires;
    private String needs;
    private String prompt;
    private String model;
    private BigDecimal costUsd;
    private Boolean active;
    private Integer inputTokens;
    private Integer outputTokens;
    private Instant createdAt;
    private Instant updatedAt;
}
