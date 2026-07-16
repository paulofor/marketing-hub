package com.marketinghub.pde.dto;

import jakarta.validation.constraints.NotBlank;

/** Representa o pedido de acesso social feito com credencial do Google. */
public record GoogleAccessRequest(
        @NotBlank String productSlug,
        @NotBlank String idToken
) {}
