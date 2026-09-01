package com.marketinghub.experiment.directrecruitment.v1.service.visit;

/** Responsabilidade: confirmar se a visita alterou o contador único do convite. */
public record RegisterDirectRecruitmentVisitResponse(boolean counted, long uniqueVisits) {}
