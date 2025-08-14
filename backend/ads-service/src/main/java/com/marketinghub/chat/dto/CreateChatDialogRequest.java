package com.marketinghub.chat.dto;

import lombok.Data;

/**
 * Request to create a ChatDialog.
 */
@Data
public class CreateChatDialogRequest {
    private String url;
    private String description;
    private String theme;
}

