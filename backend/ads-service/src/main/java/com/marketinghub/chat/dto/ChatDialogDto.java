package com.marketinghub.chat.dto;

import lombok.Data;
import java.time.Instant;

/**
 * DTO for ChatDialog.
 */
@Data
public class ChatDialogDto {
    private Long id;
    private String url;
    private String description;
    private String theme;
    private Instant createdAt;
    private Instant updatedAt;
}

