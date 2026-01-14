package com.marketinghub.prompt.dto;

import lombok.Data;

@Data
public class CreatePromptRequest {
    private String name;
    private String domain;
    private String template;
    private Boolean active;
}
