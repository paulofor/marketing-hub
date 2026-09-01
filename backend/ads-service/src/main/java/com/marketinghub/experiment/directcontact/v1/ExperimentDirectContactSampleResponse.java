package com.marketinghub.experiment.directcontact.v1;

import java.util.List;

/** Responsabilidade: apresentar o placar auditável da amostra individual consentida. */
public record ExperimentDirectContactSampleResponse(
    Long experimentId,
    String platform,
    String experimentStatus,
    int targetContacts,
    long recordedContacts,
    long remainingContacts,
    boolean readyForHermesReview,
    String operationalStatus,
    List<ExperimentDirectContactResponse> contacts) {}
