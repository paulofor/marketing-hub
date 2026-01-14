package com.marketinghub.prompt.dto;

import lombok.Data;

@Data
public class UpdatePromptRequest {
    private String name;
    private String domain;
    private String template;
    private Boolean active;
}
