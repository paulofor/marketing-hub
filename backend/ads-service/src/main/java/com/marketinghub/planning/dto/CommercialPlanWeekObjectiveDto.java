package com.marketinghub.planning.dto;

/** Responsabilidade: expor um objetivo editavel da próxima semana no planejamento comercial. */
public record CommercialPlanWeekObjectiveDto(Long id, Integer sequenceOrder, String objectiveText, Integer score) {}
