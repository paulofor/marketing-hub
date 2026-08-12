package com.marketinghub.planning.dto;

/** Responsabilidade: confirmar o enfileiramento da homologação técnica de um plano comercial. */
public record CommercialPlanJourneyHomologationDto(
    Long planId, Long experimentId, String status, String requestedAt) {}
