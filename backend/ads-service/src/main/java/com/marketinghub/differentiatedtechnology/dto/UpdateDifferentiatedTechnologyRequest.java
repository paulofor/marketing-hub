package com.marketinghub.differentiatedtechnology.dto;

import lombok.Data;

/**
 * Dados para atualizar uma tecnologia diferenciada.
 */
@Data
public class UpdateDifferentiatedTechnologyRequest {
    private String name;
    private String description;
    private String promptText;
}
