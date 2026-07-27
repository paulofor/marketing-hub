package com.marketinghub.differentiatedtechnology.dto;

import java.time.Instant;
import lombok.Data;

/** DTO para {@link com.marketinghub.differentiatedtechnology.DifferentiatedTechnology}. */
@Data
public class DifferentiatedTechnologyDto {
  private Long id;
  private String name;
  private String description;
  private String promptText;
  private Instant createdAt;
  private Instant updatedAt;
}
