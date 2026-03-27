package com.marketinghub.experiment.learning.mapper;

import com.marketinghub.experiment.learning.ExperimentLearningRequest;
import com.marketinghub.experiment.learning.dto.ExperimentLearningRequestDetailDto;
import com.marketinghub.experiment.learning.dto.ExperimentLearningRequestDto;
import org.springframework.stereotype.Component;

/**
 * Facilita a conversão de entidades {@link ExperimentLearningRequest} para DTOs.
 */
@Component
public class ExperimentLearningRequestMapper {

    public ExperimentLearningRequestDto toDto(ExperimentLearningRequest entity) {
        if (entity == null) {
            return null;
        }
        ExperimentLearningRequestDto dto = new ExperimentLearningRequestDto();
        dto.setId(entity.getId());
        dto.setExperimentId(entity.getExperiment().getId());
        dto.setStatus(entity.getStatus());
        dto.setRequestedAt(entity.getRequestedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setRequestedBy(entity.getRequestedBy());
        dto.setFailureReason(entity.getFailureReason());
        return dto;
    }

    public ExperimentLearningRequestDetailDto toDetailDto(ExperimentLearningRequest entity) {
        ExperimentLearningRequestDetailDto dto = new ExperimentLearningRequestDetailDto();
        ExperimentLearningRequestDto base = toDto(entity);
        dto.setId(base.getId());
        dto.setExperimentId(base.getExperimentId());
        dto.setStatus(base.getStatus());
        dto.setRequestedAt(base.getRequestedAt());
        dto.setCompletedAt(base.getCompletedAt());
        dto.setRequestedBy(base.getRequestedBy());
        dto.setFailureReason(base.getFailureReason());
        dto.setPayloadSnapshot(entity.getPayloadSnapshot());
        dto.setResultPayload(entity.getResultPayload());
        return dto;
    }
}
