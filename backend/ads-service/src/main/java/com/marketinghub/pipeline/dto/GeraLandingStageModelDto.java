package com.marketinghub.pipeline.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que expõe o modelo OpenAI configurado no banco para uma etapa operacional do Gera Landing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeraLandingStageModelDto {
    private String stageCode;
    private Long pipelineId;
    private String pipelineCode;
    private Long pipelineStageId;
    private String pipelineStageCode;
    private Long openAiModelId;
    private String openAiModelName;
    private String openAiModelCode;
}
