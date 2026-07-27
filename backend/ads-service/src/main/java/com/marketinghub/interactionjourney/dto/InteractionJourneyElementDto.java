package com.marketinghub.interactionjourney.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  private Integer minQuantity;
  private Integer maxQuantity;

  @Builder.Default private List<InteractionJourneyElementDto> children = new ArrayList<>();
}
