package com.marketinghub.geralanding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GeraLandingPromptReceiveRequest(
        @NotNull Long experimentId,
        @NotBlank String stageCode,
        @NotBlank String prompt,
        String openAiJobId,
        String openAiRequestBody,
        String schemaJson,
        String promptMarkdownContent) {
}
