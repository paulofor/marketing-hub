package com.marketinghub.product.bigidea.v1.service.save;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Define a direção comercial, suas razões e limites sem confundir hipótese com prova. */
public record BigIdeaContent(
    @NotBlank @Size(max = 500) String centralIdea,
    @NotBlank @Size(max = 1500) String audience,
    @NotBlank @Size(max = 1500) String desire,
    @NotBlank @Size(max = 1500) String promise,
    @NotBlank @Size(max = 2000) String mechanism,
    @NotBlank @Size(max = 3000) String evidence,
    @NotBlank @Size(max = 2000) String limits) {}
