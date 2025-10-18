package com.marketinghub.appidea.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * Request payload for creating an application idea.
 */
@Data
public class CreateAppIdeaRequest {
    private String name;
    @JsonAlias({"nicheId"})
    private Long marketNicheId;
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
