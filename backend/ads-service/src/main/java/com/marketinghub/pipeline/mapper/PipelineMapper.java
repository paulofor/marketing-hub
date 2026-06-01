package com.marketinghub.pipeline.mapper;

import com.marketinghub.pipeline.Pipeline;
import com.marketinghub.pipeline.PipelineStage;
import com.marketinghub.pipeline.dto.PipelineDto;
import com.marketinghub.pipeline.dto.PipelineStageDto;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapper responsável por converter entidades de pipeline em DTOs de contrato da API.
 */
@Component
public class PipelineMapper {
    /**
     * Converte um pipeline completo em DTO com etapas ordenadas.
     */
    public PipelineDto toDto(Pipeline pipeline) {
        return PipelineDto.builder()
                .id(pipeline.getId())
                .name(pipeline.getName())
                .code(pipeline.getCode())
                .module(pipeline.getModule())
                .description(pipeline.getDescription())
                .active(pipeline.isActive())
                .stages(toStageDtos(pipeline.getStages()))
                .createdAt(pipeline.getCreatedAt())
                .updatedAt(pipeline.getUpdatedAt())
                .build();
    }

    /**
     * Converte uma etapa de pipeline em DTO de resposta.
     */
    public PipelineStageDto toStageDto(PipelineStage stage) {
        return PipelineStageDto.builder()
                .id(stage.getId())
                .pipelineId(stage.getPipeline().getId())
                .position(stage.getPosition())
                .name(stage.getName())
                .code(stage.getCode())
                .description(stage.getDescription())
                .required(stage.isRequired())
                .active(stage.isActive())
                .createdAt(stage.getCreatedAt())
                .updatedAt(stage.getUpdatedAt())
                .build();
    }

    /**
     * Converte uma lista de etapas em DTOs de resposta.
     */
    public List<PipelineStageDto> toStageDtos(List<PipelineStage> stages) {
        return stages.stream().map(this::toStageDto).toList();
    }
}
