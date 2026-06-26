package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.consolidadossie;

import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageContext;
import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageProcessor;
import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Consolida score, recomendação e próximos movimentos. */
public class ConsolidaDossieProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa consolidadossie. */
    @Override
    public String stageName() {
        return "consolidadossie";
    }

    /** Executa a etapa consolidadossie a partir do contexto recebido do backend. */
    @Override
    public StageResult process(StageContext context) {
        ConsolidaDossieOutput output = new ConsolidaDossieOutput(
                context.salesPageId(),
                "READY_FOR_IMPLEMENTATION",
                "Consolida score, recomendação e próximos movimentos."
        );
        return StageResult.done(Map.of("consolidadossie", output), List.of());
    }
}
