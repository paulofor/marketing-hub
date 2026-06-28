package com.marketinghub.pipelines.nichocnae.v3.personatournament;

import com.marketinghub.pipelines.nichocnae.v3.core.StageArtifact;
import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageProcessor;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Processa a etapa persona-tournament do pipeline NichoCNAE v3 selecionando a persona vencedora. */
public final class PersonaTournamentProcessor implements StageProcessor {
    private static final String STATUS = "PERSONA_PRIORIZADA";

    /** Executa a etapa persona-tournament escolhendo o candidato vencedor para guiar as próximas etapas. */
    @Override
    public StageResult process(StageContext context) {
        List<Map<String, Object>> candidates = candidatePersonas(context.input());
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Entrada de persona-tournament não contém candidatePersonas para escolher candidato vencedor.");
        }

        List<Map<String, Object>> ranking = rankCandidates(candidates);
        Map<String, Object> winner = ranking.getFirst();

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "persona-tournament");
        output.put("status", STATUS);
        output.put("jobId", context.jobId());
        output.put("stageExecutionId", context.stageExecutionId());
        output.put("inputKeys", context.input().keySet());
        output.put("winnerPersona", winner);
        output.put("winningPersonaName", text(winner.get("name")));
        output.put("selectionRationale", selectionRationale(winner));
        output.put("personaRanking", ranking);
        output.put("businessBoundary", "NAO_GERAR_OFERTA_CAMPANHA_LANDING");
        output.put("reportRole", "PERSONA_ROTINA_TAREFAS_DIARIAS");
        output.put("nextStageCode", "routine-query-planner");
        return new StageResult(STATUS, output, List.of(new StageArtifact(STATUS, "inline://nichocnae-v3/persona-tournament", "Persona vencedora selecionada para orientar rotina e tarefas diárias.")));
    }

    /** Extrai personas candidatas da entrada persistida recebida do backend. */
    private List<Map<String, Object>> candidatePersonas(Map<String, Object> input) {
        Object raw = input.get("candidatePersonas");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                map.forEach((key, value) -> normalized.put(String.valueOf(key), value));
                candidates.add(normalized);
            }
        }
        return candidates;
    }

    /** Ordena candidatos por aderência operacional usando sinais já gerados na etapa anterior. */
    private List<Map<String, Object>> rankCandidates(List<Map<String, Object>> candidates) {
        return candidates.stream()
                .map(this::withScore)
                .sorted(Comparator.comparingInt((Map<String, Object> persona) -> (Integer) persona.get("tournamentScore")).reversed()
                        .thenComparing(persona -> text(persona.get("name"))))
                .toList();
    }

    /** Calcula pontuação baseada em rotina autônoma, usando aliases reais emitidos pela etapa de IA anterior. */
    private Map<String, Object> withScore(Map<String, Object> persona) {
        int painCount = listSize(firstExisting(persona, "operationalPains", "pains"));
        int taskCount = listSize(firstExisting(persona, "dailyTasks", "recurringTasks", "dailyFlow"));
        int signalCount = listSize(firstExisting(persona, "buyingSignals", "toolsAndRecords", "routineDecisions"));
        int autonomousScore = autonomousOwnerScore(persona);
        int employeePenalty = employeeRolePenalty(persona);
        int score = painCount * 3 + taskCount * 2 + signalCount + autonomousScore - employeePenalty;
        Map<String, Object> scored = new LinkedHashMap<>(persona);
        scored.putIfAbsent("dailyTasks", texts(firstExisting(persona, "dailyTasks", "recurringTasks", "dailyFlow")));
        scored.putIfAbsent("operationalPains", texts(firstExisting(persona, "operationalPains", "pains", "validationNeed")));
        scored.putIfAbsent("buyingSignals", texts(firstExisting(persona, "buyingSignals", "toolsAndRecords", "routineDecisions")));
        scored.put("tournamentScore", score);
        scored.put("scoreBreakdown", Map.of(
                "operationalPains", painCount,
                "dailyTasks", taskCount,
                "buyingSignals", signalCount,
                "autonomousOwnerScore", autonomousScore,
                "employeeRolePenalty", employeePenalty));
        return scored;
    }

    /** Lê o primeiro campo preenchido dentre aliases compatíveis com saídas anteriores do pipeline. */
    private Object firstExisting(Map<String, Object> persona, String... keys) {
        for (String key : keys) {
            Object value = persona.get(key);
            if (value instanceof List<?> list && !list.isEmpty()) {
                return value;
            }
            if (value != null && !text(value).isBlank()) {
                return value;
            }
        }
        return List.of();
    }

    /** Pontua melhor personas de dono-operador, MEI, autônomo ou familiar por aderirem ao recorte do pipeline. */
    private int autonomousOwnerScore(Map<String, Object> persona) {
        String combined = combinedText(persona);
        int score = 0;
        if (containsAny(combined, List.of("dono", "propriet", "mei", "autônom", "autonom", "por conta própria", "conta propria", "familiar"))) {
            score += 8;
        }
        if (containsAny(combined, List.of("balcão", "balcao", "whatsapp", "instagram", "cliente", "fornecedor", "caixa", "estoque"))) {
            score += 4;
        }
        return score;
    }

    /** Penaliza cargos CLT/retaguarda que travam buscas públicas de rotina de MEI/autônomo. */
    private int employeeRolePenalty(Map<String, Object> persona) {
        String combined = combinedText(persona);
        return containsAny(combined, List.of("estoquista", "funcionário", "funcionario", "empregado", "clt", "contratado", "retaguarda", "auxiliar", "gerente contratado")) ? 18 : 0;
    }

    /** Junta campos textuais relevantes da persona para classificação simples. */
    private String combinedText(Map<String, Object> persona) {
        return persona.values().stream().map(this::text).reduce("", (left, right) -> left + " " + right).toLowerCase();
    }

    /** Verifica se algum termo aparece no texto normalizado. */
    private boolean containsAny(String text, List<String> terms) {
        return terms.stream().anyMatch(text::contains);
    }

    /** Converte valor textual ou lista em lista de textos para manter contrato downstream. */
    private List<String> texts(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(this::text).filter(item -> !item.isBlank()).toList();
        }
        String item = text(value);
        return item.isBlank() ? List.of() : List.of(item);
    }

    /** Monta justificativa objetiva para exibir ao usuário por que a persona venceu. */
    private String selectionRationale(Map<String, Object> winner) {
        Object breakdown = winner.get("scoreBreakdown");
        return "Candidato vencedor por concentrar maior evidência operacional de dores, tarefas diárias e sinais de compra. Pontuação: "
                + winner.get("tournamentScore") + ". Detalhe: " + breakdown + ".";
    }

    /** Conta itens de listas recebidas de forma tolerante. */
    private int listSize(Object value) {
        return value instanceof List<?> list ? list.size() : 0;
    }

    /** Converte valores opcionais em texto seguro. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
