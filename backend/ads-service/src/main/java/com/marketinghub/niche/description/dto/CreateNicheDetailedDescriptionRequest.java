package com.marketinghub.niche.description.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload para registrar uma nova descrição detalhada de nicho.
 */
@Data
public class CreateNicheDetailedDescriptionRequest {
    private Long marketNicheId;
    private Long promptId;
    private String title;
    private String description;
    private String pains;
    private String desires;
    private String needs;
    private String prompt;
    private String model;
    private BigDecimal costUsd;
    private Integer inputTokens;
    private Integer outputTokens;
}
