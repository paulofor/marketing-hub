package com.marketinghub.leadportal.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * DTO summarizing a lead portal flow with its questions.
 */
@Data
public class LeadPortalFlowDto {
    private Long id;
    private String name;
    private String slug;
    private String publicUrl;
    private String description;
    private String model;
    private String prompt;
    private boolean approved;
    private Instant approvedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<LeadPortalFlowQuestionDto> questions;
}
