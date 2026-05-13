package com.marketinghub.oprm.market.dto;

import jakarta.validation.constraints.NotBlank;

public record OprmCnaeUpsertDto(@NotBlank String cnaeCode, @NotBlank String description, boolean active) {}
