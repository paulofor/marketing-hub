package com.marketinghub.planning.dto;

/** Responsabilidade: confirmar o início ou a retomada auditável da homologação comercial. */
public record CommercialPlanJourneyHomologationDto(
    Long planId, Long experimentId, String status, String requestedAt) {}
