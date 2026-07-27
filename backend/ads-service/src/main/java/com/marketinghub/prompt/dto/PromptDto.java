package com.marketinghub.prompt.dto;

import java.time.Instant;
import lombok.Data;

@Data
public class PromptDto {
  private Long id;
  private String name;
  private String domain;
  private String template;
  private boolean active;
  private Instant createdAt;
  private Instant updatedAt;
}
