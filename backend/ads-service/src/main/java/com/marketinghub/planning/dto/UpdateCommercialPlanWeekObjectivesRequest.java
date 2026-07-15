package com.marketinghub.planning.dto;

import java.util.List;

/** Responsabilidade: receber a lista editada de objetivos da próxima semana do planejamento comercial. */
public record UpdateCommercialPlanWeekObjectivesRequest(List<Item> objectives) {
    /** Responsabilidade: representar um objetivo da próxima semana enviado pela tela. */
    public record Item(Long id, Integer sequenceOrder, String objectiveText, Integer score) {}
}
