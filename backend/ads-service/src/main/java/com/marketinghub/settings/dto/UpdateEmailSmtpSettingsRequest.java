package com.marketinghub.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateEmailSmtpSettingsRequest(
        String providerName,
        @NotBlank(message = "Informe o host SMTP")
        String host,
        @NotNull(message = "Informe a porta do servidor")
        @Min(value = 1, message = "Porta inválida")
        @Max(value = 65535, message = "Porta inválida")
        Integer port,
        Boolean authEnabled,
        String username,
        String password,
        String fromName,
        @NotBlank(message = "Informe o e-mail do remetente")
        @Email(message = "E-mail do remetente inválido")
        String fromEmail,
        Boolean useStartTls,
        Boolean useSsl,
        @Positive(message = "Timeout de conexão deve ser maior que zero")
        Integer connectionTimeoutMs,
        @Positive(message = "Timeout de leitura deve ser maior que zero")
        Integer readTimeoutMs,
        @Positive(message = "Timeout de escrita deve ser maior que zero")
        Integer writeTimeoutMs,
        Boolean dryRun
) {
}
