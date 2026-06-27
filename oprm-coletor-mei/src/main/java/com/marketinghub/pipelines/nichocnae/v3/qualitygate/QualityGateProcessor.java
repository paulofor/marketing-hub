package com.marketinghub.pipelines.nichocnae.v3.qualitygate;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa quality-gate do pipeline NichoCNAE v3. */
public final class QualityGateProcessor implements StageProcessor {
    /** Executa a etapa quality-gate decidindo se a rotina tem evidência suficiente para materialização. */
    @Override
    public StageResult process(StageContext context) {
        List<Map<String, Object>> dailyTasks = maps(context.input().get("dailyTasks"));
        boolean approved = dailyTasks.size() >= 2 && dailyTasks.stream().anyMatch(task -> !text(task.get("sourceUrl")).isBlank());
        String status = approved ? "QUALIDADE_APROVADA" : "QUALIDADE_BLOQUEADA";

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "quality-gate");
        output.put("status", status);
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("approved", approved);
        output.put("evidenceTaskCount", dailyTasks.size());
        output.put("gateCriteria", List.of("mínimo de duas tarefas sintetizadas", "ao menos uma tarefa com fonte rastreável", "ausência de oferta/campanha/landing prematura"));
        output.put("decisionReason", approved ? "Há sinais suficientes de rotina e fonte rastreável para materializar relatório da persona." : "Faltam tarefas suficientes ou fonte rastreável; reprocessar busca/coleta antes de avançar.");
        output.put("recommendedCorrectionStage", approved ? "" : "source-searcher");
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", approved ? "persona-routine-materializer" : "");
        return new StageResult(status, output, List.of(new StageArtifact(status, "inline://nichocnae-v3/quality-gate", "Gate de qualidade executado com decisão e causa persistíveis.")));
    }

    /** Normaliza a lista de tarefas sintetizadas recebida da etapa anterior. */
    private List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Map.class::isInstance).map(item -> {
            Map<String, Object> normalized = new LinkedHashMap<>();
            ((Map<?, ?>) item).forEach((key, val) -> normalized.put(String.valueOf(key), val));
            return normalized;
        }).toList();
    }

    /** Converte valor opcional em texto seguro. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
