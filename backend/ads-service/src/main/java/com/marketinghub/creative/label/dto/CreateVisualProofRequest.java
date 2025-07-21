package com.marketinghub.creative.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateVisualProofRequest {
    @NotBlank
    @Size(max = 60)
    private String name;
    private String proofType;
    private String description;
}
