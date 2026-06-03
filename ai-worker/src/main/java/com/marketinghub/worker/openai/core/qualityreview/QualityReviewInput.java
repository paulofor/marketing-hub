package com.marketinghub.worker.openai.core.qualityreview;

import java.util.List;
import java.util.Map;

/** Responsabilidade: transportar o payload de entrada da revisão visual de qualidade no core OpenAI. */
public record QualityReviewInput(
        Long experimentId,
        String stageCode,
        String idJob,
        Map<String, Object> promptData,
        List<String> imageUrls
) {
    /** Normaliza dados opcionais para evitar coleções nulas durante a montagem do prompt visual. */
    public QualityReviewInput {
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }
}
