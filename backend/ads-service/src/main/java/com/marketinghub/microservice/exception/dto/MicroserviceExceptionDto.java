package com.marketinghub.microservice.exception.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class MicroserviceExceptionDto {
    private Long id;
    private Long microserviceId;
    private String microserviceName;
    private String exceptionType;
    private String message;
    private String stackTrace;
    private String severity;
    private String serviceVersion;
    private String hostname;
    private String context;
    private Instant occurredAt;
    private Instant createdAt;
}
