package com.marketinghub.microservice.dto;

/** Projeção leve com dados descobertos a partir do docker-compose. */
public record DiscoveredMicroserviceDto(
    String serviceName,
    String image,
    Integer hostPort,
    Integer containerPort,
    String baseUrl,
    String healthCheckPath) {}
