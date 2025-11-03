package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.LeadPortalQuestionType;
import lombok.Data;

import java.util.List;

/**
 * DTO exposing a question that belongs to a lead portal flow.
 */
@Data
public class LeadPortalFlowQuestionDto {
    private Long id;
    private String title;
    private String dataKey;
    private LeadPortalQuestionType type;
    private boolean required;
    private String description;
    private String placeholder;
    private int position;
    private List<String> options;
}
