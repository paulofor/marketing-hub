package com.marketinghub.differentiatedtechnology.dto;

import lombok.Data;

import java.time.Instant;

/**
 * DTO para {@link com.marketinghub.differentiatedtechnology.DifferentiatedTechnology}.
 */
@Data
public class DifferentiatedTechnologyDto {
    private Long id;
    private String name;
    private String description;
    private String promptText;
    private Instant createdAt;
    private Instant updatedAt;
}
