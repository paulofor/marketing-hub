package com.marketinghub.leadportal.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.leadportal.LeadPortalSimpleFormStyleDefinition;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link LeadPortalSimpleFormStyleDefinition} as JSON in the database.
 */
@Converter
public class LeadPortalSimpleFormStyleDefinitionConverter
        implements AttributeConverter<LeadPortalSimpleFormStyleDefinition, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(LeadPortalSimpleFormStyleDefinition attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize simple form style definition", ex);
        }
    }

    @Override
    public LeadPortalSimpleFormStyleDefinition convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, LeadPortalSimpleFormStyleDefinition.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize simple form style definition", ex);
        }
    }
}
