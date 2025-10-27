package com.marketinghub.whatsapp.dto;

import lombok.Data;

import java.time.Instant;

/**
 * DTO representation of a WhatsApp account.
 */
@Data
public class WhatsAppAccountDto {
    private Long id;
    private String displayName;
    private String phoneNumber;
    private String phoneNumberId;
    private String businessAccountId;
    private String accessToken;
    private String verifyToken;
    private String baseUrl;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
