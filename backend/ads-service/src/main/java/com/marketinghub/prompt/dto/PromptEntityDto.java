package com.marketinghub.prompt.dto;

import java.time.Instant;
import lombok.Data;

@Data
public class PromptEntityDto {
  private Long id;
  private String name;
  private Instant createdAt;
  private Instant updatedAt;
}
