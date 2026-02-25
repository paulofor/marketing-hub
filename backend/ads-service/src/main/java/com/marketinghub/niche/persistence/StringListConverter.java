package com.marketinghub.niche.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null) {
            return null;
        }
        List<String> normalized = attribute.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList();
        try {
            return MAPPER.writeValueAsString(normalized);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize string list", ex);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<String> parsed = MAPPER.readValue(dbData, new TypeReference<List<String>>() {
            });
            return parsed == null ? new ArrayList<>() : parsed.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(String::trim)
                    .toList();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to deserialize string list", ex);
        }
    }
}
