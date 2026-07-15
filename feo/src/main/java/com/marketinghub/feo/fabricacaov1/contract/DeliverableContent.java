package com.marketinghub.feo.fabricacaov1.contract;

import java.util.List;

/**
 * Representa o conteúdo final estruturado de um entregável.
 */
public record DeliverableContent(
        String code,
        String title,
        String componentType,
        String headline,
        String appliedPrinciple,
        String buyerOutcome,
        String firstWin,
        String readyToUseAsset,
        String tangibleProof,
        String ritualStep,
        String antiObjectionBonus,
        List<DeliverableSection> sections,
        List<String> checklist,
        List<String> templateFields,
        List<String> commonMistakes,
        String completionCriteria) {
}
