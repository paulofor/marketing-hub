package com.marketinghub.experiment.frameworkimage.dto.internal;

import jakarta.validation.constraints.NotBlank;

public record FrameworkImageWebReadyRequest(
        @NotBlank(message = "webUrl é obrigatório")
        String webUrl) {
}
