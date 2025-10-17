package com.marketinghub.appidea.dto;

import lombok.Data;

import java.time.Instant;

/**
 * Data transfer object exposing application idea details to the frontend.
 */
@Data
public class AppIdeaDto {
    private Long id;
    private String name;
    private String niche;
    private String targetAudience;
    private String problemToSolve;
    private String valueProposition;
    private String coreFeatures;
    private String differentiator;
    private String monetization;
    private String goToMarket;
    private String technologyStack;
    private String model;
    private String prompt;
    private Instant createdAt;
    private Instant updatedAt;
}
