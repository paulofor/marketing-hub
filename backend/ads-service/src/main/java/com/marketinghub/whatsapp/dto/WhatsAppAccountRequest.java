package com.marketinghub.whatsapp.dto;

import lombok.Data;

/**
 * Request body for creating or updating a WhatsApp account configuration.
 */
@Data
public class WhatsAppAccountRequest {
    private String displayName;
    private String phoneNumber;
    private String phoneNumberId;
    private String businessAccountId;
    private String accessToken;
    private String verifyToken;
    private String baseUrl;
    private Boolean active;
}
