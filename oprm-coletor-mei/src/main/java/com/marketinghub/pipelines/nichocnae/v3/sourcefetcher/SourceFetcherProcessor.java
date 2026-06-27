package com.marketinghub.pipelines.nichocnae.v3.sourcefetcher;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/** Processa a etapa source-fetcher do pipeline NichoCNAE v3. */
public final class SourceFetcherProcessor implements StageProcessor {
    private static final String STATUS = "SNAPSHOTS_COLETADOS";

    /** Executa a etapa source-fetcher materializando snapshots de evidência a partir das fontes selecionadas. */
    @Override
    public StageResult process(StageContext context) {
        List<Map<String, Object>> selectedSources = maps(context.input().get("selectedSources"));
        if (selectedSources.isEmpty()) {
            selectedSources = maps(context.input().get("foundSources"));
        }
        if (selectedSources.isEmpty()) {
            throw new IllegalStateException("Entrada de source-fetcher não contém selectedSources/foundSources para coletar snapshots úteis.");
        }
        AtomicInteger sequence = new AtomicInteger(1);
        List<Map<String, Object>> snapshots = selectedSources.stream()
                .map(source -> snapshot(source, sequence.getAndIncrement()))
                .filter(Objects::nonNull)
                .toList();
        if (snapshots.isEmpty()) {
            throw new IllegalStateException("Source-fetcher não conseguiu transformar fontes em snapshots auditáveis.");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "source-fetcher");
        output.put("status", STATUS);
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("snapshotCount", snapshots.size());
        output.put("sourceSnapshots", snapshots);
        output.put("collectionCriteria", List.of("preservar URL/título/trecho de evidência", "separar evidência funcional de metadado técnico", "não inventar fatos ausentes na fonte"));
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "routine-signal-extractor");
        return new StageResult(STATUS, output, List.of(new StageArtifact(STATUS, "inline://nichocnae-v3/source-fetcher", "Snapshots auditáveis coletados para extração de sinais de rotina.")));
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

    /** Monta um snapshot funcional mínimo da fonte para a etapa de extração. */
    private Map<String, Object> snapshot(Map<String, Object> source, int sequence) {
        String url = text(first(source, "url", "sourceUrl", "link"));
        String title = text(first(source, "title", "sourceTitle", "name"));
        String evidence = text(first(source, "snippet", "excerpt", "evidence", "description"));
        if (url.isBlank() && title.isBlank() && evidence.isBlank()) {
            return null;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("snapshotId", "snapshot-" + sequence);
        snapshot.put("url", url);
        snapshot.put("title", title);
        snapshot.put("evidenceText", evidence);
        snapshot.put("sourceType", text(first(source, "sourceType", "type", "category")));
        snapshot.put("routineRelevance", text(first(source, "routineRelevance", "objective", "whyRelevant")));
        snapshot.put("capturedFields", List.of("url", "title", "evidenceText", "sourceType", "routineRelevance"));
        return snapshot;
    }

    /** Retorna o primeiro campo existente no mapa de fonte. */
    private Object first(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key)) {
                return source.get(key);
            }
        }
        return "";
    }

    /** Converte valor opcional em texto sem nulos. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
