package com.marketinghub.chat.dto;

import lombok.Data;

/**
 * Request body for creating a chat session.
 */
@Data
public class CreateChatSessionRequest {
    private String userId;
    private String channel;
    private String state;
}
