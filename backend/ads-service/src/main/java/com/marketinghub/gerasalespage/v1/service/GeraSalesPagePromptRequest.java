package com.marketinghub.gerasalespage.v1.service;

import jakarta.validation.constraints.NotBlank;

/** Payload recebido do AI Worker com request e prompt enviados ao modelo. */
public record GeraSalesPagePromptRequest(
        @NotBlank String prompt,
        @NotBlank String promptMarkdownContent,
        @NotBlank String schemaJson,
        @NotBlank String requestBodyJson,
        String openAiModel,
        String openAiJobId
) {}
