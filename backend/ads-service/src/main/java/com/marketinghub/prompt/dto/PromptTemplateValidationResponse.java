package com.marketinghub.prompt.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PromptTemplateValidationResponse {
    private boolean valid;
    private String message;
    private String renderedPrompt;
    private List<String> missingVariables;
    private List<String> availableVariables;
}
