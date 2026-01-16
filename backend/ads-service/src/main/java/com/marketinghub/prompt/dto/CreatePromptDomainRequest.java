package com.marketinghub.prompt.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreatePromptDomainRequest {
    private String code;
    private String name;
    private String description;
    private List<String> objects = new ArrayList<>();
}
