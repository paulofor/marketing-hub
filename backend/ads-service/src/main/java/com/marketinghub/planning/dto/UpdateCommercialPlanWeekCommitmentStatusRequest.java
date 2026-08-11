package com.marketinghub.planning.dto;

/** Responsabilidade: receber a evolução do estado de um compromisso semanal. */
public record UpdateCommercialPlanWeekCommitmentStatusRequest(String status, Integer score) {}
