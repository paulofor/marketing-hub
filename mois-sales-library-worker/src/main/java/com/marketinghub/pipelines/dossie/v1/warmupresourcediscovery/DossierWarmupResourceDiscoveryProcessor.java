package com.marketinghub.pipelines.dossie.v1.warmupresourcediscovery;

import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa descoberta de recursos de aquecimento sem acoplar o núcleo do pipeline às demais etapas do dossiê. */
public class DossierWarmupResourceDiscoveryProcessor implements StageProcessor {

    /** Informa o nome canônico da etapa descoberta de recursos de aquecimento. */
    @Override
    public String stageName() {
        return "warmup-resource-discovery";
    }

    /** Produz saída estruturada mínima para auditoria e evolução incremental da etapa. */
    @Override
    public StageResult process(StageContext context) {
        DossierWarmupResourceDiscoveryOutput output = new DossierWarmupResourceDiscoveryOutput(
                context.dossierId(),
                "READY_FOR_IMPLEMENTATION",
                "Descobrir canais, comunidades, aulas, lives, reviews, afiliados e provas sociais que aquecem o público."
        );
        return StageResult.done(Map.of("warmup-resource-discovery", output), List.of());
    }
}
