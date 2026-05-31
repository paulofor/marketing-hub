package com.marketinghub.geralanding.presetdesign.service.recebePrompt;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/** Representa o payload interno com prompt, schema e request cru enviados ao provedor de IA. */
public record RecebePromptRequest(
        Long experimentId,
        String stageCode,
        @NotBlank String prompt,
        String promptMarkdownContent,
        @NotBlank String schemaJson,
        @NotBlank @JsonAlias({"openAiRequestBody", "requestBodyJson"}) String requestBodyJson,
        String openAiModel,
        @NotBlank @JsonAlias({"jobidopenai", "openAiJobId"}) String jobidopenai) {}
