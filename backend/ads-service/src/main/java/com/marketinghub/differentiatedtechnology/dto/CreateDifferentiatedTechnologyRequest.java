package com.marketinghub.differentiatedtechnology.dto;

import lombok.Data;

/**
 * Dados para criar uma tecnologia diferenciada.
 */
@Data
public class CreateDifferentiatedTechnologyRequest {
    private String name;
    private String description;
    private String promptText;
}
