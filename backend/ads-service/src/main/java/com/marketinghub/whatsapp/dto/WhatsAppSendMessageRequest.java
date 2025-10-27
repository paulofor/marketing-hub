package com.marketinghub.whatsapp.dto;

import com.marketinghub.whatsapp.WhatsAppMessageType;
import lombok.Data;

/**
 * Request body for sending WhatsApp messages manually.
 */
@Data
public class WhatsAppSendMessageRequest {
    private String to;
    private WhatsAppMessageType type;
    private String textBody;
    private String imageUrl;
    private String caption;
}
