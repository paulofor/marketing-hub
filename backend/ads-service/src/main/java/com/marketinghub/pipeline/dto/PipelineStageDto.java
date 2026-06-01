package com.marketinghub.pipeline.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que expõe uma etapa ordenada de pipeline para a tela administrativa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStageDto {
    private Long id;
    private Long pipelineId;
    private Integer position;
    private String name;
    private String code;
    private String description;
    private boolean required;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
