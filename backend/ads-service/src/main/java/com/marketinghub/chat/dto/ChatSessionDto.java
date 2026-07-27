package com.marketinghub.chat.dto;

import java.time.Instant;
import lombok.Data;

/** Data transfer object for ChatSession. */
@Data
public class ChatSessionDto {
  private Long id;
  private String userId;
  private String channel;
  private String state;
  private Instant createdAt;
  private Instant updatedAt;
}
