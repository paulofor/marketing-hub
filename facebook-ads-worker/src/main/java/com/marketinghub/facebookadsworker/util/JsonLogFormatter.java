package com.marketinghub.facebookadsworker.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Utility to produce JSON formatted representations of objects for structured logging.
 */
public final class JsonLogFormatter {

    private static final ObjectMapper FALLBACK_MAPPER = JsonMapper.builder()
        .findAndAddModules()
        .build();

    private JsonLogFormatter() {
    }

    public static Object wrap(ObjectMapper mapper, Object value) {
        return new JsonLogValue(mapper, value);
    }

    public static Object wrap(Object value) {
        return new JsonLogValue(null, value);
    }

    private static String toJson(ObjectMapper mapper, Object value) {
        if (value == null) {
            return "null";
        }
        if (mapper != null) {
            try {
                return mapper.writeValueAsString(value);
            } catch (Exception ignored) {
                // Fallback to default mapper below.
            }
        }
        try {
            return FALLBACK_MAPPER.writeValueAsString(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }

    private static final class JsonLogValue {
        private final ObjectMapper mapper;
        private final Object value;

        private JsonLogValue(ObjectMapper mapper, Object value) {
            this.mapper = mapper;
            this.value = value;
        }

        @Override
        public String toString() {
            return toJson(mapper, value);
        }
    }
}
