package com.marketinghub.prompt.dto;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class PromptDomainDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private List<PromptDomainObjectDto> objects = new ArrayList<>();
    private List<String> availableVariables = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
