package com.marketinghub.leadportal.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.model.SimpleFormStyleDefinition;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SimpleFormStyleDefinitionConverter implements AttributeConverter<SimpleFormStyleDefinition, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(SimpleFormStyleDefinition attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize simple form style definition", e);
        }
    }

    @Override
    public SimpleFormStyleDefinition convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, SimpleFormStyleDefinition.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to deserialize simple form style definition", e);
        }
    }
}
