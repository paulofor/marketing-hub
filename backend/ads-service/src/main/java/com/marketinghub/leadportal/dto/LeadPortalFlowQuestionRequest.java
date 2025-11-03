package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.LeadPortalQuestionType;
import lombok.Data;

import java.util.List;

/**
 * Payload describing a question to be created or updated inside a flow.
 */
@Data
public class LeadPortalFlowQuestionRequest {
    private String title;
    private String dataKey;
    private LeadPortalQuestionType type;
    private boolean required;
    private String description;
    private String placeholder;
    private List<String> options;
}
