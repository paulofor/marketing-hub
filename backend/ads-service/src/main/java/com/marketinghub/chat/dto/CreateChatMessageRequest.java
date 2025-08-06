package com.marketinghub.chat.dto;

import lombok.Data;

/**
 * Request body for creating a chat message.
 */
@Data
public class CreateChatMessageRequest {
    private Long sessionId;
    private String origin;
    private String content;
}
