package com.marketinghub.pipelines.dossie.v1.dossiersynthesis;

import com.marketinghub.pipelines.dossie.v1.DossierStageSupport;
import com.marketinghub.pipelines.dossie.v1.StageArtifact;
import com.marketinghub.pipelines.dossie.v1.StageContext;
import com.marketinghub.pipelines.dossie.v1.StageProcessor;
import com.marketinghub.pipelines.dossie.v1.StageResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Executa a etapa síntese final do dossiê gerando uma entrega de negócio ou bloqueando saída fraca. */
public class DossierDossierSynthesisProcessor implements StageProcessor {

    private static final String STAGE_NAME = "dossier-synthesis";
    private static final String OBJECTIVE = "Consolidar conclusão de negócio, evidências, recursos de aquecimento, recomendação final e próximos passos comerciais para exibição na tela.";
    private static final String INSUFFICIENT_CONTEXT_ERROR = "Dossiê final bloqueado: as etapas anteriores não entregaram evidências comerciais suficientes para gerar conclusão, recomendação e próximos passos úteis.";

    /** Informa o nome canônico da etapa síntese final do dossiê. */
    @Override
    public String stageName() {
        return STAGE_NAME;
    }

    /** Produz o dossiê final quando há evidência suficiente ou falha para impedir conclusão vazia. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> auditEvidence = DossierStageSupport.evidenceFor(context, STAGE_NAME);
        List<String> stageResponses = stageResponses(context);
        if (!hasBusinessEvidence(stageResponses)) {
            DossierDossierSynthesisOutput blockedOutput = new DossierDossierSynthesisOutput(
                    context.dossierId(),
                    "BLOCKED_INSUFFICIENT_CONTEXT",
                    OBJECTIVE,
                    "Não foi possível gerar um dossiê comercial confiável porque o backend/etapas anteriores entregaram apenas metadados operacionais, sem evidências de produto, promessa, prova, risco e recomendação.",
                    stageResponses,
                    List.of(
                            "Reexecutar o dossiê após corrigir as etapas anteriores para retornarem evidências comerciais estruturadas.",
                            "Garantir que a etapa final receba produto, promessa, público, fontes qualificadas, sinais de aquecimento e riscos.",
                            "Não usar este resultado para decisão de oferta enquanto o gate estiver bloqueado."),
                    "BLOCKED",
                    auditEvidence);
            return StageResult.failed(
                    INSUFFICIENT_CONTEXT_ERROR,
                    Map.of(STAGE_NAME, blockedOutput),
                    List.of(finalArtifact(context, blockedOutput)));
        }

        DossierDossierSynthesisOutput output = new DossierDossierSynthesisOutput(
                context.dossierId(),
                "OBJECTIVE_FULFILLED",
                OBJECTIVE,
                "O dossiê foi consolidado com base nas evidências comerciais recebidas das etapas anteriores. Use as evidências e próximos passos para decidir se o produto merece nova oferta, aquecimento ou descarte.",
                stageResponses,
                List.of(
                        "Validar as evidências principais antes de investir mídia.",
                        "Transformar os sinais de dor, prova e mecanismo em hipótese de oferta.",
                        "Registrar decisão comercial no relatório da página."),
                "APPROVED",
                auditEvidence);
        return StageResult.done(Map.of(STAGE_NAME, output), List.of(finalArtifact(context, output)));
    }

    /** Extrai do contexto as respostas anteriores entregues pelo backend para a síntese final. */
    @SuppressWarnings("unchecked")
    private List<String> stageResponses(StageContext context) {
        Object previousStageResponses = context.input() == null ? null : context.input().get("previousStageResponses");
        if (previousStageResponses instanceof List<?> responses) {
            return responses.stream()
                    .map(String::valueOf)
                    .filter(response -> !response.isBlank())
                    .toList();
        }
        Object previousStages = context.input() == null ? null : context.input().get("previousStages");
        if (previousStages instanceof Map<?, ?> stages) {
            List<String> responses = new ArrayList<>();
            stages.values().forEach(value -> {
                if (value instanceof Map<?, ?> stage && stage.get("response") != null) {
                    responses.add(String.valueOf(stage.get("response")));
                }
            });
            return responses;
        }
        return List.of();
    }

    /** Verifica se as respostas anteriores contêm sinais de negócio além de metadados operacionais. */
    private boolean hasBusinessEvidence(List<String> stageResponses) {
        if (stageResponses.size() < 3) {
            return false;
        }
        String joined = String.join(" ", stageResponses).toLowerCase();
        boolean hasOnlyOperationalMarkers = joined.contains("auditdecision") && joined.contains("inputkeys") && joined.contains("stageexecutionid");
        boolean hasBusinessMarkers = joined.contains("promessa")
                || joined.contains("público")
                || joined.contains("publico")
                || joined.contains("evidência")
                || joined.contains("evidencia")
                || joined.contains("recomendação")
                || joined.contains("recomendacao")
                || joined.contains("risco")
                || joined.contains("prova");
        return hasBusinessMarkers && !hasOnlyOperationalMarkers;
    }

    /** Cria o artefato final separado dos metadados técnicos de auditoria. */
    private StageArtifact finalArtifact(StageContext context, DossierDossierSynthesisOutput output) {
        return new StageArtifact(
                STAGE_NAME + "-final-dossier",
                "dossie/v1/" + context.dossierId() + "/" + STAGE_NAME + "/final",
                output.finalConclusion(),
                Instant.now());
    }
}
