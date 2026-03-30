package com.marketinghub.experiment.pipeline.dto;

import lombok.Data;

@Data
public class ExperimentPipelineGenerationRequest {
    private String customInstructions;
    private String model;
}
