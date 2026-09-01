package com.marketinghub.experiment.directrecruitment.v1.service.publicview;

/** Responsabilidade: expor o convite público sem revelar oferta antes da adesão. */
public record PublicDirectRecruitmentResponse(
    String token,
    Long experimentId,
    String status,
    boolean acceptingSubmissions,
    String productName,
    String headline,
    String bodyText,
    String audienceSummary,
    String consentText,
    String consentVersion,
    String privacyPolicyUrl,
    int targetContacts,
    long remainingContacts,
    String availabilityMessage) {}
