package com.marketinghub.pde.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Representa o pedido de acesso manual usado para validação local. */
public record AccessRequest(
        @NotBlank String productSlug,
        @Email @NotBlank String email,
        String experienceVersion
) {
    /** Mantém compatibilidade com integrações que ainda não informam a versão da experiência. */
    public AccessRequest(String productSlug, String email) {
        this(productSlug, email, null);
    }
}
