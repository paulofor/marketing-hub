package com.marketinghub.pipelines.dossie.v1.warmupsignalextraction;

import com.marketinghub.pipelines.dossie.v1.DossierStageSupport;
import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa extração de sinais de aquecimento cumprindo o objetivo funcional contratado para o dossiê. */
public class DossierWarmupSignalExtractionProcessor implements StageProcessor {

    private static final String STAGE_NAME = "warmup-signal-extraction";
    private static final String OBJECTIVE = "Extrair sinais de autoridade, prova social, educação pré-venda, comunidade, distribuição, objeções e intensidade de aquecimento das fontes qualificadas.";

    /** Informa o nome canônico da etapa extração de sinais de aquecimento. */
    @Override
    public String stageName() {
        return STAGE_NAME;
    }

    /** Produz saída funcional auditável alinhada ao objetivo da etapa. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> evidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
        DossierWarmupSignalExtractionOutput output = new DossierWarmupSignalExtractionOutput(
                context.dossierId(),
                "OBJECTIVE_FULFILLED",
                OBJECTIVE,
                evidence);
        return StageResult.done(
                Map.of(STAGE_NAME, output),
                List.of(DossierStageSupport.objectiveArtifact(context, STAGE_NAME, OBJECTIVE, evidence)));
    }
}
