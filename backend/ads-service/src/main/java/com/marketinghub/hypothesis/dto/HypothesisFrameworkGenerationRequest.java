package com.marketinghub.hypothesis.dto;

import lombok.Data;

@Data
public class HypothesisFrameworkGenerationRequest {
    private String model;
    private String customInstructions;
}
