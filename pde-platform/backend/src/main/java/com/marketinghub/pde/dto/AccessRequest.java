package com.marketinghub.pde.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Representa o pedido de acesso manual usado para validação local. */
public record AccessRequest(
        @NotBlank String productSlug,
        @Email @NotBlank String email
) {}
