package com.marketinghub.pipelines.dossie.v1.warmupmapbuilder;

import com.marketinghub.pipelines.dossie.v1.DossierStageSupport;
import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa montagem do mapa de aquecimento cumprindo o objetivo funcional contratado para o dossiê. */
public class DossierWarmupMapBuilderProcessor implements StageProcessor {

    private static final String STAGE_NAME = "warmup-map-builder";
    private static final String OBJECTIVE = "Organizar os recursos externos por papel no aquecimento do público e transformar sinais em jornada de aquecimento e matriz de força comercial, avaliando demanda, clareza da promessa, credibilidade do mecanismo, prova, distribuição, objeções, facilidade de compra e lacunas para adaptação.";

    /** Informa o nome canônico da etapa montagem do mapa de aquecimento. */
    @Override
    public String stageName() {
        return STAGE_NAME;
    }

    /** Produz saída funcional auditável alinhada ao objetivo da etapa. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> evidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
        DossierWarmupMapBuilderOutput output = new DossierWarmupMapBuilderOutput(
                context.dossierId(),
                "OBJECTIVE_FULFILLED",
                OBJECTIVE,
                evidence);
        return StageResult.done(
                Map.of(STAGE_NAME, output),
                List.of(DossierStageSupport.objectiveArtifact(context, STAGE_NAME, OBJECTIVE, evidence)));
    }
}
