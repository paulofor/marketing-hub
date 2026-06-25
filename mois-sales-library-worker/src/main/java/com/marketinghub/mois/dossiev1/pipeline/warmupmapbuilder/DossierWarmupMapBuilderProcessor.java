package com.marketinghub.mois.dossiev1.pipeline.warmupmapbuilder;

import com.marketinghub.mois.dossiev1.pipeline.StageContext;
import com.marketinghub.mois.dossiev1.pipeline.StageProcessor;
import com.marketinghub.mois.dossiev1.pipeline.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa montagem do mapa de aquecimento sem acoplar o núcleo do pipeline às demais etapas do dossiê. */
public class DossierWarmupMapBuilderProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa montagem do mapa de aquecimento. */
    @Override
    public String stageName() {
        return "warmup-map-builder";
    }

    /** Produz saída estruturada mínima para auditoria e evolução incremental da etapa. */
    @Override
    public StageResult process(StageContext context) {
        DossierWarmupMapBuilderOutput output = new DossierWarmupMapBuilderOutput(
                context.dossierId(),
                "READY_FOR_IMPLEMENTATION",
                "Organizar os recursos externos que aquecem o público antes da compra."
        );
        return StageResult.done(Map.of("warmup-map-builder", output), List.of());
    }
}
