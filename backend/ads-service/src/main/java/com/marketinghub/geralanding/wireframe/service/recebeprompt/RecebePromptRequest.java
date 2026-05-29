package com.marketinghub.geralanding.wireframe.service.recebeprompt;

import jakarta.validation.constraints.NotBlank;

/** Representa o payload interno de recebimento do prompt enviado ao provedor de IA. */
public record RecebePromptRequest(@NotBlank String prompt, @NotBlank String jobidopenai) {}
