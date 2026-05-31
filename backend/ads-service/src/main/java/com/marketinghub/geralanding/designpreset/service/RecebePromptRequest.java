package com.marketinghub.geralanding.designpreset.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/** Representa o payload interno com prompt, schema e request cru enviados ao provedor de IA na etapa design preset. */
public record RecebePromptRequest(
        Long experimentId,
        String stageCode,
        @NotBlank String prompt,
        String promptMarkdownContent,
        @NotBlank String schemaJson,
        @NotBlank @JsonAlias("requestBodyJson") String openAiRequestBody,
        String openAiModel,
        @JsonAlias("jobidopenai") String openAiJobId) {}
