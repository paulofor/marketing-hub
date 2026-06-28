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
        boolean hasTraceableSource = dailyTasks.stream().anyMatch(task -> !text(task.get("sourceUrl")).isBlank());
        boolean hasCommercialRoutineContext = dailyTasks.stream().anyMatch(this::hasCommercialRoutineContext);
        boolean hasRecognizableLanguage = dailyTasks.stream().anyMatch(task -> !text(task.get("evidenceText")).isBlank());
        boolean approved = dailyTasks.size() >= 2 && hasTraceableSource && hasCommercialRoutineContext && hasRecognizableLanguage;
        String status = approved ? "QUALIDADE_APROVADA" : "QUALIDADE_BLOQUEADA";

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "quality-gate");
        output.put("status", status);
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("approved", approved);
        output.put("evidenceTaskCount", dailyTasks.size());
        output.put("hasTraceableSource", hasTraceableSource);
        output.put("hasCommercialRoutineContext", hasCommercialRoutineContext);
        output.put("hasRecognizableLanguage", hasRecognizableLanguage);
        output.put("gateCriteria", List.of("mínimo de duas tarefas sintetizadas", "ao menos uma tarefa com fonte rastreável", "contexto MEI/autônomo com canal, atendimento, cobrança, agenda, recorrência ou cliente", "linguagem/evidência reconhecível do público", "ausência de oferta/campanha/landing prematura"));
        output.put("decisionReason", approved ? "Há sinais suficientes de rotina, canal comercial cotidiano e fonte rastreável para materializar o brief de público." : "Faltam tarefas, fonte rastreável, linguagem reconhecível ou contexto comercial cotidiano; reprocessar busca/coleta antes de avançar.");
        output.put("recommendedCorrectionStage", approved ? "" : "source-searcher");
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", approved ? "persona-routine-materializer" : "");
        return new StageResult(status, output, List.of(new StageArtifact(status, "inline://nichocnae-v3/quality-gate", "Gate de qualidade executado com decisão e causa persistíveis.")));
    }

    /** Verifica se a tarefa traz canal ou situação comercial cotidiana útil para a próxima fase. */
    private boolean hasCommercialRoutineContext(Map<String, Object> task) {
        String combined = (text(task.get("channelContext")) + " " + text(task.get("task")) + " " + text(task.get("buyingSignal"))).toLowerCase();
        return List.of("whatsapp", "instagram", "cliente", "agenda", "cobrança", "cobranca", "balcão", "balcao", "recorr", "indicação", "indicacao", "atendimento").stream().anyMatch(combined::contains);
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
