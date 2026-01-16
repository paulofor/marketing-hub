package com.marketinghub.prompt.dto;

import lombok.Data;

@Data
public class PromptTemplateValidationRequest {
    private String domain;
    private String template;
}
