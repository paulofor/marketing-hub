package com.marketinghub.mois.dossiev1.pipeline.warmupsignalextraction;

import com.marketinghub.mois.dossiev1.pipeline.StageContext;
import com.marketinghub.mois.dossiev1.pipeline.StageProcessor;
import com.marketinghub.mois.dossiev1.pipeline.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa extração de sinais de aquecimento sem acoplar o núcleo do pipeline às demais etapas do dossiê. */
public class DossierWarmupSignalExtractionProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa extração de sinais de aquecimento. */
    @Override
    public String stageName() {
        return "warmup-signal-extraction";
    }

    /** Produz saída estruturada mínima para auditoria e evolução incremental da etapa. */
    @Override
    public StageResult process(StageContext context) {
        DossierWarmupSignalExtractionOutput output = new DossierWarmupSignalExtractionOutput(
                context.dossierId(),
                "READY_FOR_IMPLEMENTATION",
                "Extrair sinais de autoridade, prova social, educação pré-venda, comunidade, distribuição e objeções."
        );
        return StageResult.done(Map.of("warmup-signal-extraction", output), List.of());
    }
}
