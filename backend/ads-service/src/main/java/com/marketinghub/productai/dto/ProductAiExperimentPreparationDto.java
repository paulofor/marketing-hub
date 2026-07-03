package com.marketinghub.productai.dto;

import com.marketinghub.experiment.ExperimentCampaignObjective;
import com.marketinghub.experiment.ExperimentStage;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.productai.ProductAiSubtype;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Responsabilidade: expor a prontidão sistêmica de uma hipótese para virar experimento de Produto IA. */
public record ProductAiExperimentPreparationDto(
        UUID hypothesisId,
        String hypothesisTitle,
        ProductAiSubtype productAiSubtype,
        boolean ready,
        List<String> blockers,
        ProductAiExperimentDraftDto draft) {

    /** Responsabilidade: representar o rascunho canônico aplicável na tela de criação de experimento. */
    public record ProductAiExperimentDraftDto(
            ExperimentType experimentType,
            ProductAiSubtype productAiSubtype,
            ExperimentStage stage,
            ExperimentCampaignObjective campaignObjective,
            String primaryVariable,
            String primaryMetric,
            BigDecimal unitPrice) {
    }
}
