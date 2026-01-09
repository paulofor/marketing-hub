package com.marketinghub.interactionjourney.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionJourneyStepDto {
    private Long id;
    private String title;
    private String description;
    private Integer orderIndex;

    @Builder.Default
    private List<InteractionJourneyElementDto> elements = new ArrayList<>();
}
