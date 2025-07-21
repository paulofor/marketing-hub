package com.marketinghub.creative.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAngleRequest {
    @NotBlank
    @Size(max = 60)
    private String name;
    private String description;
    private String frameType;
}
