package com.marketinghub.emailservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkEmailRequestDto(
        @NotEmpty(message = "É necessário informar ao menos um e-mail para envio em massa")
        @Size(max = 500, message = "Limite máximo de 500 envios em lote por requisição")
        List<@Valid EmailRequestDto> emails
) {
}
