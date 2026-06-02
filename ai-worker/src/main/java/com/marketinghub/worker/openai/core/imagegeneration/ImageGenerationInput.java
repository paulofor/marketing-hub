package com.marketinghub.worker.openai.core.imagegeneration;

import java.util.List;
import java.util.Objects;

/** Responsabilidade: transportar os prompts de imagem do GeraLanding para a etapa imagegeneration do core OpenAI. */
public record ImageGenerationInput(
        Long experimentId,
        String stageCode,
        String idJob,
        List<ImageGenerationPromptItem> images
) {
    /** Garante que a etapa sempre receba identificadores e uma lista segura de imagens planejadas. */
    public ImageGenerationInput {
        Objects.requireNonNull(idJob, "idJob must not be null");
        images = images == null ? List.of() : List.copyOf(images);
    }

    /** Responsabilidade: representar um item planejado de imagem com prompt e chaves de vinculação da landing. */
    public record ImageGenerationPromptItem(
            String planningItemKey,
            String sectionId,
            String elementId,
            String imageGoal,
            String prompt
    ) {
        /** Normaliza campos textuais opcionais do item planejado preservando o prompt funcional. */
        public ImageGenerationPromptItem {
            planningItemKey = normalize(planningItemKey);
            sectionId = normalize(sectionId);
            elementId = normalize(elementId);
            imageGoal = normalize(imageGoal);
            prompt = normalize(prompt);
        }

        /** Retorna a melhor chave operacional para correlacionar imagem, prompt e manifesto. */
        public String effectiveKey() {
            if (hasText(planningItemKey)) {
                return planningItemKey;
            }
            if (hasText(elementId)) {
                return elementId;
            }
            if (hasText(sectionId)) {
                return sectionId;
            }
            return "item";
        }

        /** Normaliza texto vazio para nulo antes de persistir metadados no payload. */
        private static String normalize(String value) {
            return hasText(value) ? value.trim() : null;
        }

        /** Verifica se o texto possui conteúdo útil após trim. */
        private static boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }
}
