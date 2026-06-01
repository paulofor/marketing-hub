package com.marketinghub.pipeline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Requisição usada para criar ou atualizar um pipeline operacional.
 */
@Data
public class PipelineRequest {
    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Size(max = 80)
    private String code;

    @NotBlank
    @Size(max = 60)
    private String module;

    private String description;
    private boolean active = true;
}
