package com.marketinghub.creative.label.dto;

import com.marketinghub.creative.label.EmotionalValence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEmotionalTriggerRequest {
    @NotBlank
    @Size(max = 60)
    private String name;
    private EmotionalValence valence;
    private String description;
}
