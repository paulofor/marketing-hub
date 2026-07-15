package com.marketinghub.feo.fabricacaov1.contract;

import java.util.List;

/**
 * Representa o conteúdo final estruturado de um entregável.
 */
public record DeliverableContent(
        String code,
        String title,
        String headline,
        String buyerOutcome,
        String firstWin,
        List<DeliverableSection> sections,
        List<String> checklist,
        List<String> templateFields,
        List<String> commonMistakes,
        String completionCriteria) {
}
