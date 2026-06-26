package com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.qualificafontes;

import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageContext;
import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageProcessor;
import com.marketinghub.mois.bibliotecapaginavenda.worker.pipelines.dossieproduto.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Qualifica fontes externas relacionadas ao produto. */
public class QualificaFontesProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa qualificafontes. */
    @Override
    public String stageName() {
        return "qualificafontes";
    }

    /** Executa a etapa qualificafontes a partir do contexto recebido do backend. */
    @Override
    public StageResult process(StageContext context) {
        QualificaFontesOutput output = new QualificaFontesOutput(
                context.salesPageId(),
                "READY_FOR_IMPLEMENTATION",
                "Qualifica fontes externas relacionadas ao produto."
        );
        return StageResult.done(Map.of("qualificafontes", output), List.of());
    }
}
