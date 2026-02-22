package com.marketinghub.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestEmailRequest(
        @NotBlank(message = "Informe o destinatário do teste")
        @Email(message = "Destinatário inválido")
        String recipient,
        @Size(max = 150, message = "Use até 150 caracteres no assunto")
        String subject,
        @Size(max = 2000, message = "Mensagem de teste muito longa")
        String message
) {
}
