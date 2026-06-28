package com.marketinghub.pipelines.nichocnae.v3.routinesignalextractor;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa routine-signal-extractor do pipeline NichoCNAE v3. */
public final class RoutineSignalExtractorProcessor implements StageProcessor {
    private static final String STATUS = "SINAIS_EXTRAIDOS";

    /** Executa a etapa routine-signal-extractor extraindo tarefas, dores e sinais de compra dos snapshots. */
    @Override
    public StageResult process(StageContext context) {
        List<Map<String, Object>> snapshots = maps(context.input().get("sourceSnapshots"));
        if (snapshots.isEmpty()) {
            throw new IllegalStateException("Entrada de routine-signal-extractor não contém sourceSnapshots para extrair sinais.");
        }
        List<Map<String, Object>> routineSignals = snapshots.stream().map(this::signal).toList();

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "routine-signal-extractor");
        output.put("status", STATUS);
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("signalCount", routineSignals.size());
        output.put("routineSignals", routineSignals);
        output.put("extractionRules", List.of("extrair apenas sinais sustentados por evidenceText", "classificar tarefa/dor/sinal de compra", "preservar referência do snapshot"));
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "daily-tasks-synthesizer");
        return new StageResult(STATUS, output, List.of(new StageArtifact(STATUS, "inline://nichocnae-v3/routine-signal-extractor", "Sinais funcionais de rotina extraídos dos snapshots coletados.")));
    }

    /** Normaliza a lista de snapshots recebida da etapa anterior. */
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

    /** Transforma um snapshot em sinal de rotina classificado. */
    private Map<String, Object> signal(Map<String, Object> snapshot) {
        String evidence = text(snapshot.get("evidenceText"));
        String lower = evidence.toLowerCase();
        Map<String, Object> signal = new LinkedHashMap<>();
        signal.put("snapshotId", text(snapshot.get("snapshotId")));
        signal.put("sourceUrl", text(snapshot.get("url")));
        signal.put("evidenceText", evidence);
        signal.put("routineTask", inferTask(evidence));
        signal.put("painSignal", inferPain(lower));
        signal.put("buyingSignal", inferBuyingSignal(lower));
        signal.put("channelContext", firstNonBlank(text(snapshot.get("mentionedChannel")), inferChannel(lower)));
        signal.put("effortIntensity", inferEffortIntensity(lower));
        signal.put("probableFrequency", inferFrequency(lower));
        signal.put("commercialOpportunityFuture", futureOpportunity(signal));
        signal.put("confidence", evidence.isBlank() ? "BAIXA" : "MEDIA");
        return signal;
    }

    /** Resume oportunidade futura sem transformar a evidência em oferta prematura. */
    private String futureOpportunity(Map<String, Object> signal) {
        return "Matéria-prima para hipótese futura: " + signal.get("painSignal") + " em " + signal.get("channelContext") + ".";
    }

    /** Infere uma tarefa objetiva sem inventar além do trecho recebido. */
    private String inferTask(String evidence) {
        if (evidence.isBlank()) {
            return "Tarefa não identificada no trecho";
        }
        return evidence.length() > 160 ? evidence.substring(0, 160) : evidence;
    }

    /** Classifica dor operacional conforme palavras observáveis no texto. */
    private String inferPain(String lowerEvidence) {
        if (lowerEvidence.contains("erro") || lowerEvidence.contains("retrabalho")) {
            return "RISCO_DE_ERRO_OU_RETRABALHO";
        }
        if (lowerEvidence.contains("tempo") || lowerEvidence.contains("demora")) {
            return "PERDA_DE_TEMPO";
        }
        if (lowerEvidence.contains("estoque") || lowerEvidence.contains("controle")) {
            return "CONTROLE_OPERACIONAL_MANUAL";
        }
        return "DOR_OPERACIONAL_A_VALIDAR";
    }

    /** Classifica sinais de compra por busca explícita de solução ou ferramenta. */
    private String inferBuyingSignal(String lowerEvidence) {
        if (lowerEvidence.contains("sistema") || lowerEvidence.contains("software") || lowerEvidence.contains("planilha")) {
            return "PROCURA_FERRAMENTA_OU_MODELO";
        }
        if (lowerEvidence.contains("consultoria") || lowerEvidence.contains("curso")) {
            return "PROCURA_AJUDA_ESPECIALIZADA";
        }
        return "SINAL_DE_COMPRA_A_VALIDAR";
    }

    /** Infere canal quando ele aparece literalmente no trecho de evidência. */
    private String inferChannel(String lowerEvidence) {
        for (String channel : List.of("whatsapp", "instagram", "agenda", "balcão", "balcao", "delivery", "cobrança", "cobranca")) {
            if (lowerEvidence.contains(channel)) {
                return channel;
            }
        }
        return "CANAL_A_VALIDAR";
    }

    /** Infere intensidade do esforço a partir de sinais textuais simples. */
    private String inferEffortIntensity(String lowerEvidence) {
        return lowerEvidence.contains("difícil") || lowerEvidence.contains("dificil") || lowerEvidence.contains("problema") || lowerEvidence.contains("retrabalho") ? "ALTA" : "MEDIA";
    }

    /** Infere frequência provável sem inventar dado não sustentado. */
    private String inferFrequency(String lowerEvidence) {
        if (lowerEvidence.contains("diário") || lowerEvidence.contains("diaria") || lowerEvidence.contains("todo dia")) {
            return "DIARIA";
        }
        if (lowerEvidence.contains("semana") || lowerEvidence.contains("recorrente")) {
            return "RECORRENTE";
        }
        return "A_VALIDAR";
    }

    /** Escolhe o primeiro texto preenchido. */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    /** Converte valor opcional em texto seguro. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
