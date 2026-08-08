package com.marketinghub.imagegenerator.dto;

import java.time.Instant;

/** Responsabilidade: resumir uma geração persistida sem transferir o conteúdo pesado da imagem. */
public record ImageGenerationHistoryItem(
    String jobId,
    String batchJobId,
    String model,
    String serviceTier,
    String outputFormat,
    String prompt,
    Instant generatedAt) {}
