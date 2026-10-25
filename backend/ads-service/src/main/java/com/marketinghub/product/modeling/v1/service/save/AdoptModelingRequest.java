package com.marketinghub.product.modeling.v1.service.save;

import jakarta.validation.constraints.*;

/** Seleciona explicitamente o ciclo que receberá as fontes congeladas da modelagem. */
public record AdoptModelingRequest(@NotNull @Positive Long cycleId) {}
