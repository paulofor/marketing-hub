package com.marketinghub.pipelines.nichocnae.v3.routinequeryplanner;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa routine-query-planner do pipeline NichoCNAE v3. */
public final class RoutineQueryPlannerProcessor implements StageProcessor {
    private static final String STATUS = "QUERIES_PLANEJADAS";
    private static final int MAX_QUERY_ITEMS = 8;

    /** Executa a etapa routine-query-planner produzindo consultas úteis para validar rotina, dor e esforço reais. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> winnerPersona = winnerPersona(context.input());
        String personaName = firstNonBlank(text(context.input().get("winningPersonaName")), text(winnerPersona.get("name")), "persona operacional priorizada");
        List<String> dailyTasks = texts(firstExisting(winnerPersona, "dailyTasks", "recurringTasks", "dailyFlow"));
        List<String> operationalPains = texts(firstExisting(winnerPersona, "operationalPains", "pains", "validationNeed"));
        List<String> buyingSignals = texts(firstExisting(winnerPersona, "buyingSignals", "toolsAndRecords", "routineDecisions"));
        List<Map<String, Object>> plannedQueries = plannedQueries(personaName, dailyTasks, operationalPains, buyingSignals);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "routine-query-planner");
        output.put("status", STATUS);
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("personaFocus", personaName);
        output.put("searchObjective", "Encontrar evidências públicas da rotina real, tarefas recorrentes, dores operacionais e sinais de compra da persona vencedora antes de qualquer oferta.");
        output.put("plannedQueries", plannedQueries);
        output.put("validationQuestions", validationQuestions(personaName));
        output.put("sourceAcceptanceCriteria", sourceAcceptanceCriteria());
        output.put("discardCriteria", discardCriteria());
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "source-searcher");
        return new StageResult(STATUS, output, List.of(new StageArtifact(STATUS, "inline://nichocnae-v3/routine-query-planner", "Plano de buscas priorizado para validar rotina, dores e esforço real da persona vencedora.")));
    }

    /** Recupera a persona vencedora gerada na etapa anterior. */
    private Map<String, Object> winnerPersona(Map<String, Object> input) {
        Object raw = input.get("winnerPersona");
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Entrada de routine-query-planner não contém winnerPersona para planejar buscas úteis.");
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        map.forEach((key, value) -> normalized.put(String.valueOf(key), value));
        return normalized;
    }

    /** Monta consultas de busca acionáveis a partir das tarefas, dores e sinais da persona. */
    private List<Map<String, Object>> plannedQueries(String personaName, List<String> dailyTasks, List<String> operationalPains, List<String> buyingSignals) {
        List<Map<String, Object>> queries = new ArrayList<>();
        addQueries(queries, personaName, dailyTasks, "TAREFA_DIARIA", "Validar tarefa recorrente e frequência operacional");
        addQueries(queries, personaName, operationalPains, "DOR_OPERACIONAL", "Confirmar dor, esforço manual e consequência prática");
        addQueries(queries, personaName, buyingSignals, "SINAL_DE_COMPRA", "Encontrar evidência de busca por solução, planilha, sistema, consultoria ou modelo pronto");
        if (queries.isEmpty()) {
            queries.add(queryItem(personaName + " MEI autônomo dono operador rotina diária tarefas problemas", "ROTINA_BASE", "Validar rotina e problemas recorrentes quando a persona não trouxe sinais detalhados", 1));
        }
        return queries.stream().limit(MAX_QUERY_ITEMS).toList();
    }

    /** Adiciona consultas mantendo prioridade de acordo com a ordem da evidência recebida. */
    private void addQueries(List<Map<String, Object>> queries, String personaName, List<String> evidences, String intent, String objective) {
        for (String evidence : evidences) {
            queries.add(queryItem(personaName + " MEI autônomo dono operador " + evidence + " rotina problema frequência", intent, objective, queries.size() + 1));
        }
    }

    /** Cria um item estruturado de consulta para a próxima etapa buscar fontes. */
    private Map<String, Object> queryItem(String query, String intent, String objective, int priority) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("priority", priority);
        item.put("query", query);
        item.put("intent", intent);
        item.put("objective", objective);
        item.put("expectedEvidence", List.of("relato de rotina", "tarefa concreta", "dor ou esforço", "frequência ou impacto"));
        return item;
    }

    /** Define perguntas que a busca precisa responder para gerar valor comercial depois. */
    private List<String> validationQuestions(String personaName) {
        return List.of(
                "Quais tarefas o " + personaName + " executa toda semana ou todo dia?",
                "Onde há perda de tempo, retrabalho, erro, estoque parado, atendimento ruim ou controle manual?",
                "Quais ferramentas, planilhas, sistemas ou serviços já são usados para reduzir esse esforço?",
                "Que evidência mostra disposição de pagar por facilidade, organização ou economia de tempo?");
    }

    /** Define critérios mínimos para aceitar uma fonte na busca. */
    private List<String> sourceAcceptanceCriteria() {
        return List.of(
                "fonte descreve rotina operacional real da persona",
                "fonte cita tarefa, dor, frequência, consequência ou ferramenta usada",
                "fonte permite extrair evidência sem criar oferta prematura");
    }

    /** Define o que deve ser descartado para evitar conteúdo inútil ou contaminado. */
    private List<String> discardCriteria() {
        return List.of(
                "conteúdo genérico de marketing sem rotina concreta",
                "oferta, campanha, landing page, promessa ou preço antes da etapa correta",
                "fonte sem relação clara com a persona vencedora");
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

    /** Converte uma lista ou texto opcional em textos limpos. */
    private List<String> texts(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(this::text).filter(item -> !item.isBlank()).toList();
        }
        String item = text(value);
        return item.isBlank() ? List.of() : List.of(item);
    }

    /** Escolhe o primeiro texto preenchido de uma lista de candidatos. */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!value.isBlank()) {
                return value;
            }
        }
        return "persona operacional priorizada";
    }

    /** Converte valores opcionais em texto seguro. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
