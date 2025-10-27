package com.marketinghub.whatsapp.dto;

import com.marketinghub.whatsapp.WhatsAppMessageDirection;
import com.marketinghub.whatsapp.WhatsAppMessageType;
import lombok.Data;

import java.time.Instant;

/**
 * DTO representation of WhatsApp message logs.
 */
@Data
public class WhatsAppMessageDto {
    private Long id;
    private Long accountId;
    private WhatsAppMessageDirection direction;
    private WhatsAppMessageType messageType;
    private String messageId;
    private String fromNumber;
    private String toNumber;
    private String status;
    private String errorCode;
    private String errorMessage;
    private String textBody;
    private String imageUrl;
    private String imageId;
    private String mimeType;
    private String caption;
    private String conversationId;
    private String contextJson;
    private String payloadJson;
    private String statusPayloadJson;
    private Instant messageTimestamp;
    private Instant sentAt;
    private Instant receivedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
