package com.marketinghub.mois.dossiev1.pipeline.intake;

import com.marketinghub.mois.dossiev1.pipeline.StageContext;
import com.marketinghub.mois.dossiev1.pipeline.StageProcessor;
import com.marketinghub.mois.dossiev1.pipeline.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa entrada inicial sem acoplar o núcleo do pipeline às demais etapas do dossiê. */
public class DossierIntakeProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa entrada inicial. */
    @Override
    public String stageName() {
        return "intake";
    }

    /** Produz saída estruturada mínima para auditoria e evolução incremental da etapa. */
    @Override
    public StageResult process(StageContext context) {
        DossierIntakeOutput output = new DossierIntakeOutput(
                context.dossierId(),
                "READY_FOR_IMPLEMENTATION",
                "Confirmar elegibilidade e preparar contexto mínimo para o dossiê v1."
        );
        return StageResult.done(Map.of("intake", output), List.of());
    }
}
