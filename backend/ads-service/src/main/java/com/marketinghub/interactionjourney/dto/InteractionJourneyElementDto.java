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
public class InteractionJourneyElementDto {
    private Long id;
    private String label;
    private String type;
    private String notes;
    private Integer orderIndex;

    @Builder.Default
    private List<InteractionJourneyElementDto> children = new ArrayList<>();
}
