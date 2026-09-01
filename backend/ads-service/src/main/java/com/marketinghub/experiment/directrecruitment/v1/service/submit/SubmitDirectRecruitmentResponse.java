package com.marketinghub.experiment.directrecruitment.v1.service.submit;

/** Responsabilidade: apresentar o resultado funcional da adesão sem inferir compra ou receita. */
public record SubmitDirectRecruitmentResponse(
    Long submissionId,
    String status,
    boolean qualified,
    String message,
    String offerUrl,
    long remainingContacts,
    boolean sampleComplete) {}
