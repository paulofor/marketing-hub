package com.marketinghub.worker.openai.core.imagegeneration;

import com.marketinghub.worker.frameworkimage.FrameworkImageJobDto;
import java.util.Objects;

/** Responsabilidade: transportar o job de imagem do framework para a etapa imagegeneration do core OpenAI. */
public record ImageGenerationInput(FrameworkImageJobDto job) {
    /** Garante que a etapa sempre receba um job de imagem válido. */
    public ImageGenerationInput {
        Objects.requireNonNull(job, "job must not be null");
    }
}
