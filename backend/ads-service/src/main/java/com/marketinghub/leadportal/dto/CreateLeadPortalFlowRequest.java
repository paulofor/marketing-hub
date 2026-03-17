package com.marketinghub.leadportal.dto;

import lombok.Data;

import java.util.List;

/**
 * Request body for creating a new lead portal flow.
 */
@Data
public class CreateLeadPortalFlowRequest {
    private String name;
    private String slug;
    private String description;
    private Long marketNicheId;
    private Long experimentId;
    private String model;
    private String imagePromptModel;
    private String imagePromptTemplate;
    private Integer imagePromptBatchSize;
    private Long simpleFormStyleId;
    private List<LeadPortalFlowQuestionRequest> questions;
}
