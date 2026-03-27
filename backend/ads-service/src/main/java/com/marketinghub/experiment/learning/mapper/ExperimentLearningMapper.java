package com.marketinghub.experiment.learning.mapper;

import com.marketinghub.experiment.learning.ExperimentLearning;
import com.marketinghub.experiment.learning.dto.ExperimentLearningDto;
import com.marketinghub.experiment.learning.dto.ExperimentLearningSuggestionDto;
import com.marketinghub.experiment.learning.dto.LearningInsightDto;
import com.marketinghub.experiment.learning.service.ExperimentLearningJsonCodec;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Converte entidades {@link ExperimentLearning} em DTOs prontos para o frontend.
 */
@Component
public class ExperimentLearningMapper {
    private final ExperimentLearningJsonCodec codec;

    public ExperimentLearningMapper(ExperimentLearningJsonCodec codec) {
        this.codec = codec;
    }

    public ExperimentLearningDto toDto(ExperimentLearning entity) {
        if (entity == null) {
            return null;
        }
        ExperimentLearningDto dto = new ExperimentLearningDto();
        dto.setId(entity.getId());
        dto.setExperimentId(entity.getExperiment().getId());
        dto.setRequestId(entity.getRequest().getId());
        dto.setNicheId(entity.getNiche().getId());
        dto.setHypothesisId(resolveHypothesisId(entity));
        dto.setStage(entity.getStage());
        dto.setPrimaryMetric(entity.getPrimaryMetric());
        dto.setMetricSignal(entity.getMetricSignal());
        dto.setSummary(entity.getSummary());
        dto.setWhatWorked(entity.getWhatWorked());
        dto.setWhatBlocked(entity.getWhatBlocked());
        dto.setNextTest(entity.getNextTest());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setInsights(safeInsights(entity));
        dto.setSuggestions(safeSuggestions(entity));
        return dto;
    }

    public List<LearningInsightDto> safeInsights(ExperimentLearning entity) {
        return codec.readInsights(entity.getInsightsJson());
    }

    public List<ExperimentLearningSuggestionDto> safeSuggestions(ExperimentLearning entity) {
        return codec.readSuggestions(entity.getSuggestionsJson());
    }

    private UUID resolveHypothesisId(ExperimentLearning entity) {
        return entity.getHypothesis() != null ? entity.getHypothesis().getId() : null;
    }
}
