package com.marketinghub.microservice.dto;

import lombok.Data;

/**
 * Request body for creating or updating a microservice registry.
 */
@Data
public class CreateMicroserviceRequest {
    private String name;
    private String description;
    private String baseUrl;
    private String category;
    private String status;
    private String owner;
    private String documentationUrl;
    private String healthCheckPath;
}
