package com.marketinghub.imagegenerator.dto;

import java.time.Instant;

/** Responsabilidade: devolver a imagem gerada e os metadados mínimos para download e auditoria. */
public record ImageGeneratorResponse(
        String jobId,
        String model,
        String serviceTier,
        String outputFormat,
        String imageBase64,
        Instant generatedAt
) {}
