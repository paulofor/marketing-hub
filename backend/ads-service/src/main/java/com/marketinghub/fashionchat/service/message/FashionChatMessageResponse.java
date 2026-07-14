package com.marketinghub.fashionchat.service.message;

import com.fasterxml.jackson.databind.JsonNode;

/** Representa a resposta preservada do serviço executor do Chat Moda. */
public record FashionChatMessageResponse(
        String answer,
        Boolean shouldGenerateImage,
        String visualBrief,
        String imagePrompt,
        String imageUrl,
        String imageError,
        String mode,
        String sandboxId,
        JsonNode research,
        String jobId) {
}
