package com.marketinghub.leadportal.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketinghub.leadportal.model.FlowQuestion;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Converter
public class FlowQuestionListConverter implements AttributeConverter<List<FlowQuestion>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final TypeReference<List<FlowQuestion>> LIST_OF_QUESTIONS = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<FlowQuestion> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize flow questions", ex);
        }
    }

    @Override
    public List<FlowQuestion> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, LIST_OF_QUESTIONS);
        } catch (IOException ex) {
            return Collections.emptyList();
        }
    }
}
