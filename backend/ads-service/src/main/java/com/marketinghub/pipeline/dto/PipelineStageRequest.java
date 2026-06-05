package com.marketinghub.pipeline.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Requisição usada para criar ou atualizar uma etapa de pipeline.
 */
@Data
public class PipelineStageRequest {
    @NotNull
    @Min(1)
    private Integer position;

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Size(max = 80)
    private String code;

    private String description;

    @Size(max = 80)
    private String executionModule;

    @Size(max = 200)
    private String rootPackage;

    private boolean required = true;
    private boolean active = true;
    private Long openAiModelId;
}
