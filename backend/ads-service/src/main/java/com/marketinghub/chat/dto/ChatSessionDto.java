package com.marketinghub.chat.dto;

import lombok.Data;
import java.time.Instant;

/**
 * Data transfer object for ChatSession.
 */
@Data
public class ChatSessionDto {
    private Long id;
    private String userId;
    private String channel;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;
}
