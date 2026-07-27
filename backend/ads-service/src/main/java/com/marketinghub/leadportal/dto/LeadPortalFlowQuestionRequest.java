package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.LeadPortalQuestionType;
import java.util.List;
import lombok.Data;

/** Payload describing a question to be created or updated inside a flow. */
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
