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
            List<ImageGeneratorVariant> variants,
            Instant generatedAt
    ) {}

    /** Responsabilidade: representar uma versão exportável da imagem para uso operacional ou web. */
    public record ImageGeneratorVariant(
            String role,
            String format,
            String imageBase64,
            int width,
            int height,
            long byteSize
    ) {}
}
