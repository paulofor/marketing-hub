package com.marketinghub.repository.jpa.oprm.generalaudience;

import java.time.Instant;
import java.util.UUID;

/** Referência da hipótese criada para manter o OPRM desacoplado do domínio de hipótese. */
public record OprmGeneralAudienceMaterializedHypothesis(
        UUID id,
        String title,
        String status,
        Instant createdAt
) {
}
