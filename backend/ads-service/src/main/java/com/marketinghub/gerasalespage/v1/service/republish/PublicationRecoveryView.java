package com.marketinghub.gerasalespage.v1.service.republish;

import java.time.Instant;

/** Responsabilidade: apresentar a disponibilidade e o envio da mesma publicação auditada. */
public record PublicationRecoveryView(
    Long publicationId,
    boolean available,
    String reason,
    String sourceSha256,
    String salesPageUrl,
    Instant submittedAt) {}
