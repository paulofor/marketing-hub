package com.marketinghub.chat.dto;

import lombok.Data;
import java.time.Instant;

/**
 * Data transfer object for ChatMessage.
 */
@Data
public class ChatMessageDto {
    private Long id;
    private Long sessionId;
    private String origin;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
