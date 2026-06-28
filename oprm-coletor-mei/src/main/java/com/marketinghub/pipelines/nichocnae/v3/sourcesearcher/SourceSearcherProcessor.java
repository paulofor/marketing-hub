package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa source-searcher do pipeline NichoCNAE v3. */
public final class SourceSearcherProcessor implements StageProcessor {
    /** Executa a etapa source-searcher validando se há fontes reais antes de permitir a coleta de snapshots. */
    @Override
    public StageResult process(StageContext context) {
        List<Map<String, Object>> foundSources = maps(context.input().get("foundSources"));
        if (foundSources.isEmpty()) {
            foundSources = maps(context.input().get("selectedSources"));
        }
        List<Map<String, Object>> plannedQueries = maps(context.input().get("plannedQueries"));
        boolean hasSources = !foundSources.isEmpty();
        String status = hasSources ? "FONTES_ENCONTRADAS" : "FONTES_NAO_COLETADAS";

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "source-searcher");
        output.put("status", status);
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("plannedQueries", plannedQueries);
        output.put("foundSourceCount", foundSources.size());
        output.put("foundSources", foundSources);
        output.put("blocked", !hasSources);
        output.put("decisionReason", hasSources
                ? "Fontes reais disponíveis para coleta de snapshots auditáveis."
                : "A etapa source-searcher ainda não recebeu fontes reais; não é seguro avançar para source-fetcher apenas com queries planejadas.");
        output.put("recommendedCorrectionStage", hasSources ? "" : "source-searcher");
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", hasSources ? "source-fetcher" : "");
        return new StageResult(status, output, List.of(new StageArtifact(status, "inline://nichocnae-v3/source-searcher", hasSources
                ? "Fontes reais encontradas para coleta de snapshots."
                : "Busca de fontes bloqueada por ausência de fontes reais auditáveis.")));
    }

    /** Converte uma lista de objetos em mapas estruturados. */
    private List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> {
                    Map<String, Object> normalized = new LinkedHashMap<>();
                    ((Map<?, ?>) item).forEach((key, val) -> normalized.put(String.valueOf(key), val));
                    return normalized;
                })
                .toList();
    }
}
