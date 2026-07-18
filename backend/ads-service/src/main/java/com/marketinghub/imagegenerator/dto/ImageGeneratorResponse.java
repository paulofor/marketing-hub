package com.marketinghub.imagegenerator.dto;

import java.time.Instant;
import java.util.List;

/** Responsabilidade: devolver as imagens geradas e os metadados mínimos para download e auditoria. */
public record ImageGeneratorResponse(
        String jobId,
        List<ImageGeneratorResult> images
) {
    /** Responsabilidade: representar uma imagem gerada para uma variação de modelo. */
    public record ImageGeneratorResult(
            String jobId,
            String model,
            String serviceTier,
            String outputFormat,
            String imageBase64,
            Instant generatedAt
    ) {}
}
