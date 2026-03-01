package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class LeadPortalSimpleFormStyleGenerationResultRequest {
    private String status;
    private String generationError;
    private String textParameters;
    private BigDecimal generationCostUsd;
    private LeadPortalSimpleFormStyleDefinition definition;
}
