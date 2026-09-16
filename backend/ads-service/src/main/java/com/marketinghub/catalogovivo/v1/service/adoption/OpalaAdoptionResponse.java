package com.marketinghub.catalogovivo.v1.service.adoption;

import java.time.Instant;

/** Responsabilidade: informar adesão, impedimentos e navegação com base no estado persistido. */
public record OpalaAdoptionResponse(
    long productId,
    long cycleId,
    long experimentId,
    boolean adopted,
    boolean canAdopt,
    String reason,
    Long processDefinitionId,
    String preparationUrl,
    String catalogUrl,
    String operatorName,
    Instant adoptedAt) {}
