package com.marketinghub.leadportal.dto;

import lombok.Data;

import java.util.List;

/**
 * Request body for updating an existing lead portal flow.
 */
@Data
public class UpdateLeadPortalFlowRequest {
    private String name;
    private String slug;
    private String description;
    private Long marketNicheId;
    private Long experimentId;
    private String model;
    private List<LeadPortalFlowQuestionRequest> questions;
}
