package com.marketinghub.chat.dto;

import java.time.Instant;
import lombok.Data;

/** Data transfer object for ChatMessage. */
@Data
public class ChatMessageDto {
  private Long id;
  private Long sessionId;
  private String origin;
  private String content;
  private Instant createdAt;
  private Instant updatedAt;
}
