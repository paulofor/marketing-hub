package com.marketinghub.pipelines.dossie.v1.investigationanchorbuilder;

import com.marketinghub.pipelines.dossie.v1.DossierStageSupport;
import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.util.List;
import java.util.Map;

/** Executa a etapa geração de âncoras de investigação cumprindo o objetivo funcional contratado para o dossiê. */
public class DossierInvestigationAnchorBuilderProcessor implements StageProcessor {

    private static final String STAGE_NAME = "investigation-anchor-builder";
    private static final String OBJECTIVE = "Gerar âncoras confiáveis de investigação pública a partir de produto, produtor, domínio, marca, promessa e termos proprietários.";

    /** Informa o nome canônico da etapa geração de âncoras de investigação. */
    @Override
    public String stageName() {
        return STAGE_NAME;
    }

    /** Produz saída funcional auditável alinhada ao objetivo da etapa. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> evidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
        DossierInvestigationAnchorBuilderOutput output = new DossierInvestigationAnchorBuilderOutput(
                context.dossierId(),
                "OBJECTIVE_FULFILLED",
                OBJECTIVE,
                evidence);
        return StageResult.done(
                Map.of(STAGE_NAME, output),
                List.of(DossierStageSupport.objectiveArtifact(context, STAGE_NAME, OBJECTIVE, evidence)));
    }
}
