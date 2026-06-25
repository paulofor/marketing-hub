package com.marketinghub.mois.dossiev1.pipeline.productunderstanding;

import com.marketinghub.mois.dossiev1.pipeline.StageContext;
import com.marketinghub.mois.dossiev1.pipeline.StageProcessor;
import com.marketinghub.mois.dossiev1.pipeline.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa entendimento do produto sem acoplar o núcleo do pipeline às demais etapas do dossiê. */
public class DossierProductUnderstandingProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa entendimento do produto. */
    @Override
    public String stageName() {
        return "product-understanding";
    }

    /** Produz saída estruturada mínima para auditoria e evolução incremental da etapa. */
    @Override
    public StageResult process(StageContext context) {
        DossierProductUnderstandingOutput output = new DossierProductUnderstandingOutput(
                context.dossierId(),
                "READY_FOR_IMPLEMENTATION",
                "Identificar produto, público, dor, promessa, mecanismo, formato, oferta e prova antes da pesquisa externa."
        );
        return StageResult.done(Map.of("product-understanding", output), List.of());
    }
}
