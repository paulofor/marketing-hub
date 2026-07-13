package com.marketinghub.fashionchat.service.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** Representa a situação real de prontidão e autenticação do Chat Moda. */
public record FashionChatValidationStatusResponse(
        String serviceBaseUrl,
        Instant checkedAt,
        boolean ready,
        Integer readyHttpStatus,
        String readyError,
        String accountStatus,
        Boolean authenticated,
        Integer accountHttpStatus,
        String accountError,
        JsonNode accountPayload) {
}
