package com.marketinghub.worker.openai.core.imagegeneration;

import java.util.Objects;

/** Responsabilidade: representar a imagem retornada pela OpenAI antes da publicação em storage. */
public record ImageGenerationOutput(
        String model,
        String prompt,
        byte[] imageContent,
        String imageUrl
) {
    /** Valida que a resposta contém ao menos bytes ou URL de imagem para publicação posterior. */
    public ImageGenerationOutput {
        boolean hasBytes = imageContent != null && imageContent.length > 0;
        boolean hasUrl = imageUrl != null && !imageUrl.isBlank();
        if (!hasBytes && !hasUrl) {
            throw new IllegalArgumentException("imageContent or imageUrl must be present");
        }
        model = Objects.toString(model, null);
        prompt = Objects.toString(prompt, null);
    }
}
