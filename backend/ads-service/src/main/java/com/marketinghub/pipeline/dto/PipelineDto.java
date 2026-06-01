package com.marketinghub.pipeline.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que expõe um pipeline com suas etapas para a tela administrativa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineDto {
    private Long id;
    private String name;
    private String code;
    private String module;
    private String description;
    private boolean active;
    private List<PipelineStageDto> stages;
    private Instant createdAt;
    private Instant updatedAt;
}
