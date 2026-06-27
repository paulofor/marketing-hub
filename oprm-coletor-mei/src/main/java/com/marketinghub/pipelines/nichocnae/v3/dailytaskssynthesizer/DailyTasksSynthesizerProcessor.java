package com.marketinghub.pipelines.nichocnae.v3.dailytaskssynthesizer;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa daily-tasks-synthesizer do pipeline NichoCNAE v3. */
public final class DailyTasksSynthesizerProcessor implements StageProcessor {
    private static final String STATUS = "TAREFAS_DIARIAS_SINTETIZADAS";

    /** Executa a etapa daily-tasks-synthesizer consolidando sinais em mapa de tarefas, dores e oportunidades de facilidade. */
    @Override
    public StageResult process(StageContext context) {
        List<Map<String, Object>> signals = maps(context.input().get("routineSignals"));
        if (signals.isEmpty()) {
            throw new IllegalStateException("Entrada de daily-tasks-synthesizer não contém routineSignals para sintetizar tarefas.");
        }
        List<Map<String, Object>> dailyTasks = signals.stream().map(this::task).toList();

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "daily-tasks-synthesizer");
        output.put("status", STATUS);
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("dailyTaskCount", dailyTasks.size());
        output.put("dailyTasks", dailyTasks);
        output.put("commercialReading", "Mapa de rotina pronto para orientar produto futuro em facilidade, economia de esforço e redução de dor, sem gerar oferta nesta etapa.");
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "quality-gate");
        return new StageResult(STATUS, output, List.of(new StageArtifact(STATUS, "inline://nichocnae-v3/daily-tasks-synthesizer", "Tarefas diárias sintetizadas a partir de sinais evidenciados.")));
    }

    /** Normaliza a lista de sinais recebida da etapa anterior. */
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

    /** Cria uma tarefa diária com dor, evidência e alavanca de facilidade. */
    private Map<String, Object> task(Map<String, Object> signal) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("task", text(signal.get("routineTask")));
        task.put("pain", text(signal.get("painSignal")));
        task.put("buyingSignal", text(signal.get("buyingSignal")));
        task.put("evidenceText", text(signal.get("evidenceText")));
        task.put("sourceUrl", text(signal.get("sourceUrl")));
        task.put("easeLever", easeLever(text(signal.get("painSignal"))));
        return task;
    }

    /** Traduz a dor operacional em alavanca de facilidade para uso posterior no pipeline. */
    private String easeLever(String pain) {
        if (pain.contains("TEMPO")) {
            return "economizar tempo em tarefa recorrente";
        }
        if (pain.contains("ERRO") || pain.contains("RETRABALHO")) {
            return "reduzir erro e retrabalho";
        }
        if (pain.contains("CONTROLE")) {
            return "simplificar controle operacional";
        }
        return "reduzir esforço operacional percebido";
    }

    /** Converte valor opcional em texto seguro. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
