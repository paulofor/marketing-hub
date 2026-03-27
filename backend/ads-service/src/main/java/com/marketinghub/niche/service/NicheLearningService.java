package com.marketinghub.niche.service;

import com.marketinghub.experiment.learning.ExperimentLearning;
import com.marketinghub.experiment.learning.LearningInsightType;
import com.marketinghub.experiment.learning.mapper.ExperimentLearningMapper;
import com.marketinghub.experiment.learning.repository.ExperimentLearningRepository;
import com.marketinghub.experiment.learning.dto.ExperimentLearningSuggestionDto;
import com.marketinghub.experiment.learning.dto.LearningInsightDto;
import com.marketinghub.niche.dto.BacklogRecommendationDto;
import com.marketinghub.niche.dto.LearningStatementDto;
import com.marketinghub.niche.dto.NicheLearningDictionaryDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Consolida aprendizados de experimentos concluídos para gerar o banco do nicho.
 */
@Service
public class NicheLearningService {

    private static final int MAX_INSIGHTS_PER_TYPE = 5;
    private static final int MAX_BACKLOG_SUGGESTIONS = 10;

    private final ExperimentLearningRepository learningRepository;
    private final ExperimentLearningMapper learningMapper;

    public NicheLearningService(ExperimentLearningRepository learningRepository,
                                ExperimentLearningMapper learningMapper) {
        this.learningRepository = learningRepository;
        this.learningMapper = learningMapper;
    }

    @Transactional(readOnly = true)
    public NicheLearningDictionaryDto summarize(Long nicheId) {
        List<ExperimentLearning> learnings = fetchLearnings(nicheId);
        Map<LearningInsightType, LinkedHashMap<String, LearningStatementDto>> grouped = new EnumMap<>(LearningInsightType.class);
        Instant updatedAt = null;
        for (ExperimentLearning learning : learnings) {
            if (learning.getCompletedAt() != null && (updatedAt == null || learning.getCompletedAt().isAfter(updatedAt))) {
                updatedAt = learning.getCompletedAt();
            }
            for (LearningInsightDto insight : learningMapper.safeInsights(learning)) {
                if (insight == null || insight.getType() == null || !StringUtils.hasText(insight.getStatement())) {
                    continue;
                }
                LinkedHashMap<String, LearningStatementDto> bucket =
                        grouped.computeIfAbsent(insight.getType(), key -> new LinkedHashMap<>());
                if (bucket.size() >= MAX_INSIGHTS_PER_TYPE) {
                    continue;
                }
                String key = insight.getStatement().trim().toLowerCase();
                if (bucket.containsKey(key)) {
                    continue;
                }
                LearningStatementDto statement = new LearningStatementDto();
                statement.setType(insight.getType());
                statement.setStatement(insight.getStatement().trim());
                statement.setConfidence(insight.getConfidence());
                statement.setEvidence(insight.getEvidence());
                statement.setMetricSignal(learning.getMetricSignal());
                statement.setExperimentId(learning.getExperiment().getId());
                statement.setExperimentName(learning.getExperiment().getName());
                statement.setCompletedAt(learning.getCompletedAt());
                bucket.put(key, statement);
            }
        }
        NicheLearningDictionaryDto dto = new NicheLearningDictionaryDto();
        dto.setUpdatedAt(updatedAt);
        dto.setPains(toList(grouped.get(LearningInsightType.PAIN)));
        dto.setResults(toList(grouped.get(LearningInsightType.RESULT)));
        dto.setMechanisms(toList(grouped.get(LearningInsightType.MECHANISM)));
        dto.setProofs(toList(grouped.get(LearningInsightType.PROOF)));
        dto.setOffers(toList(grouped.get(LearningInsightType.OFFER)));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<BacklogRecommendationDto> backlog(Long nicheId) {
        List<ExperimentLearning> learnings = fetchLearnings(nicheId);
        List<BacklogRecommendationDto> recommendations = new ArrayList<>();
        for (ExperimentLearning learning : learnings) {
            if (recommendations.size() >= MAX_BACKLOG_SUGGESTIONS) {
                break;
            }
            List<ExperimentLearningSuggestionDto> suggestions = learningMapper.safeSuggestions(learning);
            if (suggestions.isEmpty() && StringUtils.hasText(learning.getNextTest())) {
                ExperimentLearningSuggestionDto fallback = new ExperimentLearningSuggestionDto();
                fallback.setTitle("Próximo teste indicado");
                fallback.setRationale(learning.getNextTest());
                fallback.setStage(learning.getStage());
                fallback.setPrimaryMetric(learning.getPrimaryMetric());
                suggestions = List.of(fallback);
            }
            for (ExperimentLearningSuggestionDto suggestion : suggestions) {
                if (suggestion == null || !StringUtils.hasText(suggestion.getTitle())) {
                    continue;
                }
                BacklogRecommendationDto dto = new BacklogRecommendationDto();
                dto.setTitle(suggestion.getTitle().trim());
                dto.setRationale(suggestion.getRationale());
                dto.setStage(suggestion.getStage() != null ? suggestion.getStage() : learning.getStage());
                dto.setPrimaryMetric(suggestion.getPrimaryMetric() != null ? suggestion.getPrimaryMetric() : learning.getPrimaryMetric());
                dto.setPriority(suggestion.getPriority());
                dto.setExperimentId(learning.getExperiment().getId());
                dto.setExperimentName(learning.getExperiment().getName());
                dto.setCompletedAt(learning.getCompletedAt());
                recommendations.add(dto);
                if (recommendations.size() >= MAX_BACKLOG_SUGGESTIONS) {
                    break;
                }
            }
        }
        return recommendations;
    }

    private List<ExperimentLearning> fetchLearnings(Long nicheId) {
        List<ExperimentLearning> learnings = learningRepository.findTop50ByNicheIdOrderByCompletedAtDesc(nicheId);
        if (learnings.isEmpty()) {
            return List.of();
        }
        return learnings;
    }

    private List<LearningStatementDto> toList(LinkedHashMap<String, LearningStatementDto> map) {
        if (map == null) {
            return List.of();
        }
        return new ArrayList<>(map.values());
    }
}
