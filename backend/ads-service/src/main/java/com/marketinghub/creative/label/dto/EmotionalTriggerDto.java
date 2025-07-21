package com.marketinghub.creative.label.dto;

import com.marketinghub.creative.label.EmotionalValence;
import lombok.Data;

@Data
public class EmotionalTriggerDto {
    private Long id;
    private String name;
    private EmotionalValence valence;
    private String description;
}
