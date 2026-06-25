package com.marketinghub.mois.dossiev1.pipeline.sourceproductmatch;

import com.marketinghub.mois.dossiev1.pipeline.StageContext;
import com.marketinghub.mois.dossiev1.pipeline.StageProcessor;
import com.marketinghub.mois.dossiev1.pipeline.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa validação de relação fonte-produto sem acoplar o núcleo do pipeline às demais etapas do dossiê. */
public class DossierSourceProductMatchProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa validação de relação fonte-produto. */
    @Override
    public String stageName() {
        return "source-product-match";
    }

    /** Produz saída estruturada mínima para auditoria e evolução incremental da etapa. */
    @Override
    public StageResult process(StageContext context) {
        DossierSourceProductMatchOutput output = new DossierSourceProductMatchOutput(
                context.dossierId(),
                "READY_FOR_IMPLEMENTATION",
                "Validar se cada fonte pertence ao produto, produtor ou recurso de aquecimento antes de virar evidência."
        );
        return StageResult.done(Map.of("source-product-match", output), List.of());
    }
}
