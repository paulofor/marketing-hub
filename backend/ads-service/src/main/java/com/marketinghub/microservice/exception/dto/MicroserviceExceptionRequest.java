package com.marketinghub.microservice.exception.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public record MicroserviceExceptionRequest(
        @Size(max = 255) String exceptionType,
        @NotBlank String message,
        String stackTrace,
        @Size(max = 20) String severity,
        @Size(max = 100) String serviceVersion,
        @Size(max = 255) String hostname,
        Map<String, Object> context,
        Instant occurredAt
) {
}
