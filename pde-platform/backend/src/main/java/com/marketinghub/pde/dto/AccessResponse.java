package com.marketinghub.pde.dto;

/** Retorna o token e a URL de acesso da cliente à experiência PDE. */
public record AccessResponse(
        String token,
        String productSlug,
        String email,
        String source,
        String accessUrl
) {}
