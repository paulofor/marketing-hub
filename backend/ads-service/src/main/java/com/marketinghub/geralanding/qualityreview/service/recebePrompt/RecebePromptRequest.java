package com.marketinghub.geralanding.qualityreview.service.recebePrompt;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/** Representa o payload interno com prompt, schema e request visual enviados ao provedor de IA. */
public record RecebePromptRequest(
        @NotBlank String prompt,
        String promptMarkdownContent,
        @NotBlank String schemaJson,
        @NotBlank @JsonAlias("openAiRequestBody") String requestBodyJson,
        @NotBlank String jobidopenai,
        Map<String, Object> qualityReviewAudit
) {
    /** Mantém o contrato imutável do recebimento de prompt da revisão visual. */
    public RecebePromptRequest {}
}
