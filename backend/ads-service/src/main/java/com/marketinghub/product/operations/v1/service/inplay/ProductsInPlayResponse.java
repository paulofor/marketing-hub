package com.marketinghub.product.operations.v1.service.inplay;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: fornecer uma fotografia consultiva com contagens calculadas pelo backend. */
public record ProductsInPlayResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant observedAt,
    int total,
    int executing,
    int waiting,
    int blocked,
    int paused,
    int unavailable,
    List<ProductInPlayResponse> products) {}
