package com.marketinghub.hypothesis.pain.service.recebePrompt;

import jakarta.validation.constraints.NotBlank;

/** Payload interno com prompt, schema e request bruto enviados para a OpenAI. */
public record RecebePromptRequest(
        @NotBlank String prompt,
        String promptMarkdownContent,
        @NotBlank String schemaJson,
        @NotBlank String requestBodyJson,
        String openAiModel,
        String jobidopenai
) {
}
