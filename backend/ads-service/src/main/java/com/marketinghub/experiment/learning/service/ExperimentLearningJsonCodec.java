package com.marketinghub.experiment.learning.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.learning.dto.ExperimentLearningPayloadDto;
import com.marketinghub.experiment.learning.dto.ExperimentLearningSuggestionDto;
import com.marketinghub.experiment.learning.dto.LearningInsightDto;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Serializa e desserializa os campos em JSON ligados aos aprendizados.
 */
@Component
public class ExperimentLearningJsonCodec {
    private static final Logger log = LoggerFactory.getLogger(ExperimentLearningJsonCodec.class);

    private final ObjectMapper objectMapper;

    public ExperimentLearningJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String writeInsights(List<LearningInsightDto> insights) {
        if (insights == null || insights.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(insights);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar insights do experimento", ex);
        }
    }

    public List<LearningInsightDto> readInsights(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            log.error("Falha ao desserializar insights de experimento", ex);
            return Collections.emptyList();
        }
    }

    public String writeSuggestions(List<ExperimentLearningSuggestionDto> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(suggestions);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar sugestões de backlog", ex);
        }
    }

    public List<ExperimentLearningSuggestionDto> readSuggestions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            log.error("Falha ao desserializar sugestões de backlog", ex);
            return Collections.emptyList();
        }
    }

    public String writePayload(ExperimentLearningPayloadDto payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar payload do aprendizado", ex);
        }
    }
}
