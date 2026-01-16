package com.marketinghub.prompt.dto;

import lombok.Data;

@Data
public class PromptDomainObjectDto {
    private String type;
    private String slug;
    private String label;
    private String contextKey;
}
