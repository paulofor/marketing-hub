package com.marketinghub.nichocnaev2.pipeline.routinesynthesizer;

import com.marketinghub.nichocnaev2.pipeline.StageArtifact;
import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Sintetiza a rotina do executor somente a partir de claims aprovados e rastreáveis no executor OPRM. */
public final class RoutineSynthesizerProcessor implements StageProcessor {
    private static final Set<String> ROUTINE_TYPES = Set.of("ROUTINE_TASK", "TASK", "OPERATIONAL_TASK");
    private static final Set<String> PAIN_TYPES = Set.of("OPERATIONAL_FAILURE", "PRACTICAL_PAIN", "RECURRING_PAIN", "PAIN", "REWORK", "WAITING_TIME", "EMOTIONAL_PAIN");
    private static final Set<String> ECONOMIC_TYPES = Set.of("DIRECT_COST", "OPPORTUNITY_COST", "ECONOMIC_IMPACT", "WORKAROUND", "PRICING", "COLLECTION");
    private static final Set<String> ACQUISITION_TYPES = Set.of("CUSTOMER_ACQUISITION", "RECURRENCE", "PURCHASE_SIGNAL", "HIRING_BEHAVIOR");

    /** Produz síntese funcional com limites explícitos e sem promover hipótese sem evidência a fato. */
    @Override
    public StageResult process(StageContext context) {
        List<Map<String, Object>> acceptedClaims = acceptedClaims(context.input());
        List<Map<String, Object>> routineTasks = synthesizeItems(acceptedClaims, ROUTINE_TYPES);
        List<Map<String, Object>> practicalPains = synthesizeItems(acceptedClaims, PAIN_TYPES);
        List<Map<String, Object>> economicSignals = synthesizeItems(acceptedClaims, ECONOMIC_TYPES);
        List<Map<String, Object>> acquisitionSignals = synthesizeItems(acceptedClaims, ACQUISITION_TYPES);
        List<String> evidenceLimits = evidenceLimits(routineTasks, practicalPains, economicSignals, acquisitionSignals);

        Map<String, Object> synthesis = new LinkedHashMap<>();
        synthesis.put("executor", context.input().get("executor"));
        synthesis.put("jobContext", context.input().get("jobContext"));
        synthesis.put("routineTasks", routineTasks);
        synthesis.put("practicalPains", practicalPains);
        synthesis.put("economicSignals", economicSignals);
        synthesis.put("acquisitionSignals", acquisitionSignals);
        synthesis.put("evidenceLimits", evidenceLimits);
        synthesis.put("supportingClaimIds", supportingClaimIds(acceptedClaims));
        synthesis.put("sourceDomains", sourceDomains(acceptedClaims));

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "routine-synthesizer");
        output.put("synthesis", synthesis);
        output.put("acceptedClaimCount", acceptedClaims.size());
        output.put("routineTaskCount", routineTasks.size());
        output.put("practicalPainCount", practicalPains.size());
        output.put("economicSignalCount", economicSignals.size());
        output.put("evidenceLimits", evidenceLimits);
        output.put("nextStageCode", "commercial-evidence-gate");

        String status = evidenceLimits.isEmpty() ? "ROUTINE_SYNTHESIS_READY" : "ROUTINE_SYNTHESIS_WITH_GAPS";
        return new StageResult(status, output, List.of(new StageArtifact(
                "ROUTINE_SYNTHESIS",
                "inline://routine-synthesizer/synthesis",
                "Síntese de rotina, dores e sinais comerciais baseada exclusivamente em claims aceitos com trecho exato.")));
    }

    /** Filtra claims validados com trecho exato antes de qualquer síntese funcional. */
    private List<Map<String, Object>> acceptedClaims(Map<String, Object> input) {
        return firstNonEmpty(input, "validatedClaims", "claims", "evidenceClaims").stream()
                .filter(this::isAccepted)
                .toList();
    }

    /** Verifica se o claim pode sustentar a síntese como evidência positiva. */
    private boolean isAccepted(Map<String, Object> claim) {
        String status = text(claim.get("status")).toUpperCase(Locale.ROOT);
        String state = text(claim.get("epistemicState")).toUpperCase(Locale.ROOT);
        String evidenceSpan = text(claim.get("exactEvidenceSpan"));
        return !evidenceSpan.isBlank()
                && (status.isBlank() || status.equals("ACCEPT") || status.equals("ACCEPTED") || status.equals("VALIDATED"))
                && (state.isBlank() || state.equals("VALIDATED") || state.equals("ACCEPTED"));
    }

    /** Agrupa claims por tipo sem criar afirmações novas além do texto validado. */
    private List<Map<String, Object>> synthesizeItems(List<Map<String, Object>> claims, Set<String> acceptedTypes) {
        List<Map<String, Object>> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> claim : claims) {
            String type = text(claim.get("claimType")).toUpperCase(Locale.ROOT);
            if (!acceptedTypes.contains(type)) {
                continue;
            }
            String claimText = text(claim.getOrDefault("claimText", claim.get("canonicalClaim")));
            String key = normalize(type + ":" + claimText);
            if (claimText.isBlank() || !seen.add(key)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("claimType", type);
            item.put("text", claimText);
            item.put("supportingClaimIds", List.of(claim.getOrDefault("canonicalClaimId", claim.getOrDefault("claimId", ""))));
            item.put("exactEvidenceSpan", claim.get("exactEvidenceSpan"));
            item.put("sourceUrl", claim.get("sourceUrl"));
            item.put("canonicalDomain", claim.get("canonicalDomain"));
            items.add(item);
        }
        return items;
    }

    /** Declara gaps explicitamente quando a evidência ainda não permite afirmações funcionais. */
    private List<String> evidenceLimits(
            List<Map<String, Object>> routineTasks,
            List<Map<String, Object>> practicalPains,
            List<Map<String, Object>> economicSignals,
            List<Map<String, Object>> acquisitionSignals) {
        List<String> limits = new ArrayList<>();
        if (routineTasks.isEmpty()) {
            limits.add("Sem tarefas concretas validadas do executor para sintetizar rotina.");
        }
        if (practicalPains.isEmpty()) {
            limits.add("Sem dor prática validada; não sintetizar dor por inferência.");
        }
        if (economicSignals.isEmpty()) {
            limits.add("Sem impacto econômico ou workaround validado; não afirmar valor comercial.");
        }
        if (acquisitionSignals.isEmpty()) {
            limits.add("Sem sinal validado de aquisição, recorrência ou intenção de compra.");
        }
        return limits;
    }

    /** Lista os IDs de claims que sustentam a síntese para auditoria e relatório de usuário. */
    private List<Object> supportingClaimIds(List<Map<String, Object>> claims) {
        return claims.stream()
                .map(claim -> claim.getOrDefault("canonicalClaimId", claim.get("claimId")))
                .filter(id -> id != null && !text(id).isBlank())
                .distinct()
                .toList();
    }

    /** Lista domínios independentes presentes na síntese para o gate seguinte. */
    private List<String> sourceDomains(List<Map<String, Object>> claims) {
        return claims.stream()
                .map(claim -> text(claim.get("canonicalDomain")))
                .filter(domain -> !domain.isBlank())
                .distinct()
                .toList();
    }

    /** Extrai a primeira lista de mapas disponível entre chaves alternativas. */
    private List<Map<String, Object>> firstNonEmpty(Map<String, Object> input, String... keys) {
        for (String key : keys) {
            List<Map<String, Object>> values = mapList(input.get(key));
            if (!values.isEmpty()) {
                return values;
            }
        }
        return List.of();
    }

    /** Extrai lista de mapas de contratos flexíveis recebidos do backend. */
    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> copy = new LinkedHashMap<>();
                map.forEach((key, val) -> copy.put(String.valueOf(key), val));
                mapped.add(copy);
            }
        }
        return mapped;
    }

    /** Normaliza texto para deduplicação simples e determinística. */
    private String normalize(String value) {
        return text(value).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /** Retorna texto seguro para regras determinísticas. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
