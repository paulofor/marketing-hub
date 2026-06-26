package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.planejabuscas;

import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageContext;
import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageProcessor;
import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Planeja termos públicos de investigação. */
public class PlanejaBuscasProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa planejabuscas. */
    @Override
    public String stageName() {
        return "planejabuscas";
    }

    /** Executa a etapa planejabuscas a partir do contexto recebido do backend. */
    @Override
    public StageResult process(StageContext context) {
        PlanejaBuscasOutput output = new PlanejaBuscasOutput(
                context.salesPageId(),
                "READY_FOR_IMPLEMENTATION",
                "Planeja termos públicos de investigação."
        );
        return StageResult.done(Map.of("planejabuscas", output), List.of());
    }
}
