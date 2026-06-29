package com.marketinghub.pipelines.dossie.v1.warmupresourcediscovery;

import com.marketinghub.pipelines.dossie.v1.DossierStageSupport;
import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa descoberta de recursos de aquecimento cumprindo o objetivo funcional contratado para o dossiê. */
public class DossierWarmupResourceDiscoveryProcessor implements StageProcessor {

    private static final String STAGE_NAME = "warmup-resource-discovery";
    private static final String OBJECTIVE = "Mapear canais, comunidades, aulas, lives, reviews, afiliados, matérias, páginas auxiliares e provas sociais que aquecem o público, classificando recursos externos por papel comercial no aquecimento — descoberta, educação, autoridade, comunidade, prova social, comparação, objeção, demonstração, captura, oferta direta e remarketing provável.";

    /** Informa o nome canônico da etapa descoberta de recursos de aquecimento. */
    @Override
    public String stageName() {
        return STAGE_NAME;
    }

    /** Produz saída funcional auditável alinhada ao objetivo da etapa. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> evidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
        DossierWarmupResourceDiscoveryOutput output = new DossierWarmupResourceDiscoveryOutput(
                context.dossierId(),
                "OBJECTIVE_FULFILLED",
                DossierStageSupport.businessDecision(context, STAGE_NAME, OBJECTIVE),
                evidence);
        return StageResult.done(
                Map.of(STAGE_NAME, output),
                List.of(DossierStageSupport.objectiveArtifact(context, STAGE_NAME, OBJECTIVE, evidence)));
    }
}
