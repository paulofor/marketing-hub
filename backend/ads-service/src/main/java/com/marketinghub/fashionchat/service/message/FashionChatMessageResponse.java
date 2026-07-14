package com.marketinghub.fashionchat.service.message;

import com.fasterxml.jackson.databind.JsonNode;

/** Representa a resposta preservada do serviço executor do Chat Moda. */
public record FashionChatMessageResponse(
        String answer,
        String mode,
        String sandboxId,
        JsonNode research) {
}
