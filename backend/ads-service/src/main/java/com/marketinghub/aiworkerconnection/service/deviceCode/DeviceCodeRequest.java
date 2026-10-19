package com.marketinghub.aiworkerconnection.service.deviceCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Recebe apenas a página oficial e o código transitório, nunca um token OAuth. */
public record DeviceCodeRequest(
    @NotBlank @Pattern(regexp = "https://auth\\.openai\\.com/codex/device") String verificationUrl,
    @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9-]{3,25}") String userCode) {}
