package com.marketinghub.planning.dto;

/** Responsabilidade: expor um objetivo semanal editavel do planejamento comercial. */
public record CommercialPlanWeekObjectiveDto(Long id, Integer sequenceOrder, String objectiveText, Integer score) {}
