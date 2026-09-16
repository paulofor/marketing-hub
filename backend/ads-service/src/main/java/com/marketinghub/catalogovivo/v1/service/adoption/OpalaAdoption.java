package com.marketinghub.catalogovivo.v1.service.adoption;

import java.time.Instant;

/** Responsabilidade: preservar a decisão explícita de incorporar Opala à passagem histórica. */
public record OpalaAdoption(
    long cycleId,
    long processDefinitionId,
    long productId,
    long experimentId,
    String productVersion,
    long cycleRevision,
    String operatorName,
    String reason,
    Instant createdAt) {}
