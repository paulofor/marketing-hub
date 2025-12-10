package com.marketinghub.emailservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record EmailRequestDto(
        @NotEmpty(message = "Ao menos um destinatário precisa ser informado")
        @Size(max = 50, message = "Limite máximo de 50 destinatários por requisição")
        List<@Email(message = "Destinatário inválido") String> to,
        List<@Email(message = "Destinatário inválido") String> cc,
        List<@Email(message = "Destinatário inválido") String> bcc,
        @NotBlank(message = "Assunto é obrigatório")
        String subject,
        @NotBlank(message = "TemplateId é obrigatório")
        String templateId,
        Map<String, Object> variables,
        List<@Valid EmailAttachmentRequest> attachments
) {
}
