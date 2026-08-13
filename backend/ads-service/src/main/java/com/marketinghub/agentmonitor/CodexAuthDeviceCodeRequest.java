package com.marketinghub.agentmonitor;

import jakarta.validation.constraints.NotBlank;

/** Responsabilidade: receber URL e código temporários emitidos pelo Codex App Server. */
public record CodexAuthDeviceCodeRequest(
    @NotBlank String verificationUrl, @NotBlank String userCode) {}
