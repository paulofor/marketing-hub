package com.marketinghub.ai.generation.mapper;

import com.marketinghub.ai.generation.AiWorkerGeneration;
import com.marketinghub.ai.generation.dto.AiWorkerGenerationDto;
import org.springframework.stereotype.Component;

@Component
public class AiWorkerGenerationMapper {
    public AiWorkerGenerationDto toDto(AiWorkerGeneration entity) {
        if (entity == null) {
            return null;
        }
        return AiWorkerGenerationDto.builder()
                .id(entity.getId())
                .domain(entity.getDomain())
                .referenceId(entity.getReferenceId())
                .model(entity.getModel())
                .prompt(entity.getPrompt())
                .rawResponse(entity.getRawResponse())
                .inputTokens(entity.getInputTokens())
                .outputTokens(entity.getOutputTokens())
                .costUsd(entity.getCostUsd())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
