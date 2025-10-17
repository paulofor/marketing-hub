package com.marketinghub.appidea.dto;

import lombok.Data;

/**
 * Request payload for creating an application idea.
 */
@Data
public class CreateAppIdeaRequest {
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
}
