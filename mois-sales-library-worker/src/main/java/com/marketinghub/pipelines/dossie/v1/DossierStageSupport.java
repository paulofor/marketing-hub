package com.marketinghub.pipelines.dossie.v1;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Centraliza a montagem de evidências funcionais comuns às etapas do dossiê MOIS v1. */
public final class DossierStageSupport {

    /** Impede instanciação de classe utilitária. */
    private DossierStageSupport() {}

    /** Monta evidências objetivas da etapa a partir do contexto recebido do backend. */
    public static Map<String, Object> evidenceFor(StageContext context, String stageName) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("stageName", stageName);
        evidence.put("stageExecutionId", context.stageExecutionId());
        evidence.put("dossierId", context.dossierId());
        evidence.put("workspaceId", context.workspaceId());
        evidence.put("inputKeys", context.input() == null ? java.util.List.of() : context.input().keySet().stream().sorted().toList());
        evidence.put("inputAvailable", context.input() != null && !context.input().isEmpty());
        evidence.put("auditDecision", "Registrar saída estruturada suficiente para o backend persistir relatório e avançar a próxima etapa.");
        return Map.copyOf(evidence);
    }

    /** Cria artefato auditável do objetivo executado sem contaminar o artefato final publicável. */
    public static StageArtifact objectiveArtifact(
            StageContext context, String stageName, String objective, Map<String, Object> evidence) {
        String payload = "stage=" + stageName
                + ";dossierId=" + context.dossierId()
                + ";objective=" + objective
                + ";inputAvailable=" + evidence.get("inputAvailable");
        return new StageArtifact(stageName + "-objective", "dossie/v1/" + context.dossierId() + "/" + stageName, payload, Instant.now());
    }
}
