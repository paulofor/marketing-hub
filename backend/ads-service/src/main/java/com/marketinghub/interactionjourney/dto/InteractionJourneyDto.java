package com.marketinghub.interactionjourney.dto;

import java.time.Instant;
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
public class InteractionJourneyDto {
  private Long id;
  private String name;
  private String description;
  private Instant createdAt;
  private Instant updatedAt;

  @Builder.Default private List<InteractionJourneyStepDto> steps = new ArrayList<>();
}
