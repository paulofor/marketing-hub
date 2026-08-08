package com.marketinghub.hypothesis.dto;

import java.math.BigDecimal;

/** Responsabilidade: transportar os ajustes comerciais permitidos ao criar uma nova versão. */
public record CreateHypothesisVersionRequest(
    String problem,
    String persona,
    String promise,
    String mechanism,
    String uniqueMechanism,
    String entrega,
    String successRule,
    String offerType,
    BigDecimal price) {}
