package com.marketinghub.pipelines.nichocnae.v3.personaroutinematerializer;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa final persona-routine-materializer materializando o perfil funcional aprovado. */
public final class PersonaRoutineMaterializerProcessor implements StageProcessor {
    private static final String STATUS = "PERFIL_MATERIALIZAVEL";

    /** Executa a etapa persona-routine-materializer consolidando persona, rotina, dores, evidências e prontidão para persistência final. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> winnerPersona = map(context.input().get("winnerPersona"));
        List<Map<String, Object>> dailyTasks = dailyTasks(context.input(), winnerPersona);
        if (winnerPersona.isEmpty() || dailyTasks.isEmpty()) {
            throw new IllegalStateException("Entrada de persona-routine-materializer exige winnerPersona e dailyTasks para materializar o perfil.");
        }

        Map<String, Object> materializedProfile = materializedProfile(context.input(), winnerPersona, dailyTasks);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "persona-routine-materializer");
        output.put("status", STATUS);
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("cnaeCode", text(context.input().get("cnaeCode")));
        output.put("cnaeDescription", text(context.input().get("cnaeDescription")));
        output.put("materializedProfile", materializedProfile);
        output.put("marketNicheCandidate", marketNicheCandidate(context.input(), materializedProfile));
        output.put("routineSummary", materializedProfile.get("routineSummary"));
        output.put("dailyTasks", materializedProfile.get("dailyTasks"));
        output.put("personaSummary", materializedProfile.get("personaDescription"));
        output.put("evidenceSummary", materializedProfile.get("recognizableVocabularyAndScenes"));
        output.put("confidenceScore", Boolean.TRUE.equals(context.input().get("approved")) ? 80 : 55);
        output.put("routineEvidenceScore", Boolean.TRUE.equals(context.input().get("approved")) ? 80 : 45);
        output.put("sourceDiversityScore", Boolean.TRUE.equals(context.input().get("approved")) ? 60 : 20);
        output.put("materializationMode", text(context.input().get("materializationMode")).isBlank()
                ? "FAST_PERSONA_ROUTINE_PROFILE"
                : text(context.input().get("materializationMode")));
        output.put("materializationReadiness", "PRONTO_PARA_BACKEND_PERSISTIR_MARKET_NICHE_E_PROFILE");
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "");
        return new StageResult(STATUS, output, List.of(new StageArtifact(STATUS, "inline://nichocnae-v3/persona-routine-materializer", "Perfil final materializado com persona, rotina, dores e evidências para persistência backend.")));
    }

    /** Monta o perfil final sem inserir metadado técnico no artefato funcional. */
    private Map<String, Object> materializedProfile(Map<String, Object> input, Map<String, Object> winnerPersona, List<Map<String, Object>> dailyTasks) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("personaName", firstNonBlank(text(input.get("winningPersonaName")), text(winnerPersona.get("name")), "persona operacional priorizada"));
        profile.put("personaDescription", text(winnerPersona.get("description")));
        profile.put("cnaeAudienceDistinction", "CNAE é volume estatístico; o público materializado é o executor MEI/autônomo observado na rotina.");
        profile.put("routineSummary", routineSummary(profile.get("personaName"), dailyTasks));
        profile.put("dailyTasks", dailyTasks);
        profile.put("topOperationalPains", dailyTasks.stream().map(task -> text(task.get("pain"))).filter(pain -> !pain.isBlank()).distinct().toList());
        profile.put("buyingSignals", dailyTasks.stream().map(task -> text(task.get("buyingSignal"))).filter(signal -> !signal.isBlank()).distinct().toList());
        profile.put("routineActionBlocks", actionBlocks(dailyTasks));
        profile.put("channels", dailyTasks.stream().map(task -> text(task.get("channelContext"))).filter(channel -> !channel.isBlank()).distinct().toList());
        profile.put("recognizableVocabularyAndScenes", dailyTasks.stream().map(task -> text(task.get("evidenceText"))).filter(text -> !text.isBlank()).distinct().toList());
        profile.put("evidenceSources", dailyTasks.stream().map(this::evidenceSource).filter(source -> !source.isEmpty()).toList());
        profile.put("easeLevers", dailyTasks.stream().map(task -> text(task.get("easeLever"))).filter(lever -> !lever.isBlank()).distinct().toList());
        profile.put("futureBriefRole", "Brief de público para fase posterior de hipótese, oferta e campanha, sem promessa comercial gerada neste pipeline.");
        profile.put("approvedByQualityGate", Boolean.TRUE.equals(input.get("approved")));
        return profile;
    }

    /** Monta tarefas estruturadas aceitando saída completa do gate ou atalho pela persona vencedora. */
    private List<Map<String, Object>> dailyTasks(Map<String, Object> input, Map<String, Object> winnerPersona) {
        List<Map<String, Object>> directTasks = maps(input.get("dailyTasks"));
        if (!directTasks.isEmpty()) {
            return directTasks;
        }
        List<String> tasks = texts(firstExisting(winnerPersona, "dailyTasks", "recurringTasks", "dailyFlow"));
        List<String> pains = texts(firstExisting(winnerPersona, "operationalPains", "pains", "validationNeed"));
        List<String> buyingSignals = texts(firstExisting(winnerPersona, "buyingSignals", "toolsAndRecords", "routineDecisions"));
        List<Map<String, Object>> normalizedTasks = new java.util.ArrayList<>();
        for (int index = 0; index < tasks.size(); index++) {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("task", tasks.get(index));
            task.put("pain", itemAtOrBlank(pains, index));
            task.put("buyingSignal", itemAtOrBlank(buyingSignals, index));
            task.put("channelContext", channelContext(winnerPersona));
            task.put("evidenceText", tasks.get(index));
            task.put("easeLever", "reduzir esforço manual e retrabalho na rotina operacional");
            task.put("actionBlock", "EXECUTAR_ROTINA_ADMINISTRATIVA");
            normalizedTasks.add(task);
        }
        return normalizedTasks;
    }

    /** Lê o primeiro campo preenchido dentre aliases compatíveis com saídas anteriores do pipeline. */
    private Object firstExisting(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof List<?> list && !list.isEmpty()) {
                return value;
            }
            if (value != null && !text(value).isBlank()) {
                return value;
            }
        }
        return List.of();
    }

    /** Converte valor textual ou lista em lista de textos. */
    private List<String> texts(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(this::text).filter(item -> !item.isBlank()).toList();
        }
        String item = text(value);
        return item.isBlank() ? List.of() : List.of(item);
    }

    /** Retorna item da lista ou vazio quando não existir valor correspondente. */
    private String itemAtOrBlank(List<String> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : "";
    }

    /** Resume canais e ferramentas da persona para manter contexto operacional no perfil. */
    private String channelContext(Map<String, Object> winnerPersona) {
        List<String> channels = texts(firstExisting(winnerPersona, "interactions", "toolsAndRecords", "buyingSignals"));
        return channels.isEmpty() ? "" : String.join("; ", channels);
    }

    /** Agrupa tarefas por blocos operacionais para formar um brief de público acionável. */
    private Map<String, Object> actionBlocks(List<Map<String, Object>> dailyTasks) {
        Map<String, Object> blocks = new LinkedHashMap<>();
        dailyTasks.forEach(task -> {
            String block = firstNonBlank(text(task.get("actionBlock")), "EXECUTAR_SERVICO");
            blocks.computeIfAbsent(block, ignored -> new java.util.ArrayList<Map<String, Object>>());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> blockTasks = (List<Map<String, Object>>) blocks.get(block);
            blockTasks.add(task);
        });
        return blocks;
    }

    /** Cria um candidato de nicho com campos funcionais para o backend materializar nas tabelas canônicas. */
    private Map<String, Object> marketNicheCandidate(Map<String, Object> input, Map<String, Object> materializedProfile) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("sourcePipeline", "nichocnae.v3");
        candidate.put("cnaeCode", text(input.get("cnaeCode")));
        candidate.put("cnaeDescription", text(input.get("cnaeDescription")));
        candidate.put("title", materializedProfile.get("personaName"));
        candidate.put("summary", materializedProfile.get("routineSummary"));
        candidate.put("channels", materializedProfile.get("channels"));
        candidate.put("easeLevers", materializedProfile.get("easeLevers"));
        candidate.put("targetAudienceType", "MEI_PROFISSIONAIS_AUTONOMOS_NAO_CLT");
        candidate.put("profilePayload", materializedProfile);
        return candidate;
    }

    /** Gera resumo funcional curto da rotina validada. */
    private String routineSummary(Object personaName, List<Map<String, Object>> dailyTasks) {
        String firstTask = dailyTasks.stream().map(task -> text(task.get("task"))).filter(task -> !task.isBlank()).findFirst().orElse("tarefas operacionais recorrentes");
        return personaName + " executa " + firstTask + " e apresenta dores recorrentes mapeadas com evidência pública.";
    }

    /** Extrai a fonte auditável de uma tarefa diária. */
    private Map<String, Object> evidenceSource(Map<String, Object> task) {
        String sourceUrl = text(task.get("sourceUrl"));
        String evidenceText = text(task.get("evidenceText"));
        if (sourceUrl.isBlank() && evidenceText.isBlank()) {
            return Map.of();
        }
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("sourceUrl", sourceUrl);
        source.put("evidenceText", evidenceText);
        return source;
    }

    /** Normaliza objeto de entrada em mapa textual. */
    private Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        raw.forEach((key, val) -> normalized.put(String.valueOf(key), val));
        return normalized;
    }

    /** Normaliza lista de mapas preservando apenas itens estruturados. */
    private List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Map.class::isInstance).map(this::map).toList();
    }

    /** Escolhe o primeiro texto preenchido. */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!value.isBlank()) {
                return value;
            }
        }
        return "persona operacional priorizada";
    }

    /** Converte valor opcional em texto seguro. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
