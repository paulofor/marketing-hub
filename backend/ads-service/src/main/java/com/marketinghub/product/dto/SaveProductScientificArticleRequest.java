package com.marketinghub.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Responsabilidade: transportar dados para cadastrar ou atualizar artigo científico de produto. */
public record SaveProductScientificArticleRequest(
    @NotBlank @Size(max = 1024) String link,
    @NotBlank @Size(max = 512) String originalTitle,
    @NotBlank @Size(max = 512) String portugueseTitle,
    @NotBlank String summary,
    @NotBlank String mechanismApplication) {}
