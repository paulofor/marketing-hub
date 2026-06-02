package com.marketinghub.worker.openai.core.imagegeneration;

import java.util.List;
import java.util.Objects;

/** Responsabilidade: representar as imagens retornadas pela OpenAI antes da publicação em storage. */
public record ImageGenerationOutput(List<GeneratedImage> images) {
    /** Valida que a resposta contém ao menos uma imagem útil para publicação posterior. */
    public ImageGenerationOutput {
        images = images == null ? List.of() : List.copyOf(images);
        if (images.isEmpty()) {
            throw new IllegalArgumentException("images must not be empty");
        }
    }

    /** Responsabilidade: transportar uma imagem gerada com suas chaves de vinculação ao planejamento da landing. */
    public record GeneratedImage(
            String planningItemKey,
            String sectionId,
            String elementId,
            String imageGoal,
            String prompt,
            String model,
            byte[] imageContent,
            String imageUrl
    ) {
        /** Valida que cada item gerado contém bytes ou URL para upload/publicação. */
        public GeneratedImage {
            boolean hasBytes = imageContent != null && imageContent.length > 0;
            boolean hasUrl = imageUrl != null && !imageUrl.isBlank();
            if (!hasBytes && !hasUrl) {
                throw new IllegalArgumentException("imageContent or imageUrl must be present");
            }
            planningItemKey = Objects.toString(planningItemKey, null);
            sectionId = Objects.toString(sectionId, null);
            elementId = Objects.toString(elementId, null);
            imageGoal = Objects.toString(imageGoal, null);
            prompt = Objects.toString(prompt, null);
            model = Objects.toString(model, null);
        }
    }
}
