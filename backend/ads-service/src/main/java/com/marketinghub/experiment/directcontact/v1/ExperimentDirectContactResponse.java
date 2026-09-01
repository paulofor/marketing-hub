package com.marketinghub.experiment.directcontact.v1;

import java.time.Instant;

/** Responsabilidade: expor um contato auditado sem revelar sua identidade original. */
public record ExperimentDirectContactResponse(
    Long id,
    String contactFingerprintSuffix,
    String consentEvidenceReference,
    Instant consentRecordedAt,
    Instant contactedAt,
    boolean audienceFitConfirmed,
    String recordedBy,
    Instant createdAt) {}
