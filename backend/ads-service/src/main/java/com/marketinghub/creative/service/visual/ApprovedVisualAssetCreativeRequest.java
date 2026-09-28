package com.marketinghub.creative.service.visual;

import jakarta.validation.constraints.NotBlank;

/** Entrada comercial para transformar um ativo visual aprovado em controle estático auditável. */
public record ApprovedVisualAssetCreativeRequest(
    @NotBlank String headline, @NotBlank String primaryText, String description) {}
