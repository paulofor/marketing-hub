package com.marketinghub.microservice.dto;

/**
 * Lightweight projection with data discovered from docker-compose.
 */
public record DiscoveredMicroserviceDto(
        String serviceName,
        String image,
        Integer hostPort,
        Integer containerPort,
        String baseUrl,
        String healthCheckPath
) {
}
