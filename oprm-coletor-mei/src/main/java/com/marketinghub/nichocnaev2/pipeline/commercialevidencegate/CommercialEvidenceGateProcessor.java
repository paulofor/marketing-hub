package com.marketinghub.nichocnaev2.pipeline.commercialevidencegate;

import com.marketinghub.nichocnaev2.pipeline.StageArtifact;
import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Calcula o gate comercial E0-E5 do NichoCNAE v2 com regras de negócio mantidas no executor externo. */
public final class CommercialEvidenceGateProcessor implements StageProcessor {
    private static final Set<String> TASK_TYPES = Set.of("TASK", "ROUTINE", "OPERATIONAL_TASK");
    private static final Set<String> PAIN_TYPES = Set.of("PAIN", "PRACTICAL_PAIN", "RECURRING_PAIN");
    private static final Set<String> ECONOMIC_TYPES = Set.of("ECONOMIC_IMPACT", "WORKAROUND", "COST", "LOST_REVENUE");
    private static final Set<String> PURCHASE_TYPES = Set.of("PURCHASE_INTENT", "HIRING_BEHAVIOR", "SOLUTION_SEARCH", "SPEND");
    private static final Set<String> OFFER_TYPES = Set.of("REAL_PURCHASE", "PAID_OFFER", "MVP_PURCHASE");

    /** Avalia claims aceitos e snapshot acumulado para decidir materialização, revisão humana ou nova pesquisa. */
    @Override
    public StageResult process(StageContext context) {
        List<Map<String, Object>> allClaims = candidateClaims(context.input());
        List<Map<String, Object>> claims = acceptedClaims(allClaims);
        EvidenceCounts counts = countEvidence(claims, allClaims);
        String evidenceLevel = evidenceLevel(counts);
        double confidence = confidence(counts);
        double informationGain = informationGain(context.input(), evidenceLevel, counts.totalSignals());
        boolean materializationEnabled = booleanValue(context.input().get("materializationEnabled"));
        boolean hasMinimumCommercialIndependence = counts.independentDomainCount() >= 3;
        boolean automaticMaterializationAllowed = materializationEnabled
                && levelRank(evidenceLevel) >= 3
                && confidence >= 0.70
                && hasMinimumCommercialIndependence
                && counts.contradictionSignals() == 0;
        boolean humanReviewRequired = levelRank(evidenceLevel) >= 3
                && !automaticMaterializationAllowed
                && (confidence >= 0.55 || counts.contradictionSignals() > 0);
        List<String> missingEvidence = missingEvidence(counts);
        String gateDecision = gateDecision(automaticMaterializationAllowed, humanReviewRequired, evidenceLevel, informationGain);
        String nextStageCode = nextStageCode(gateDecision);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "commercial-evidence-gate");
        output.put("evidenceLevel", evidenceLevel);
        output.put("confidence", confidence);
        output.put("automaticMaterializationAllowed", automaticMaterializationAllowed);
        output.put("humanReviewRequired", humanReviewRequired);
        output.put("informationGain", informationGain);
        output.put("gateDecision", gateDecision);
        output.put("nextStageCode", nextStageCode);
        output.put("missingEvidence", missingEvidence);
        output.put("evidenceCounts", counts.asMap());
        output.put(
                "commercialSeparation",
                Map.of(
                        "activityExists", counts.activitySignals(),
                        "recurringPain", counts.painSignals(),
                        "economicImpact", counts.economicSignals(),
                        "purchaseIntent", counts.purchaseSignals(),
                        "contradictions", counts.contradictionSignals()));
        return new StageResult(gateDecision, output, List.of(new StageArtifact(
                "COMMERCIAL_EVIDENCE_GATE",
                "inline://commercial-evidence-gate/decision",
                "Nível E0-E5, confiança calculada, gaps e decisão de materialização calculados no executor.")));
    }

    /** Filtra claims aceitos e validados sem transformar rejeição em sinal positivo. */
    private List<Map<String, Object>> candidateClaims(Map<String, Object> input) {
        List<Map<String, Object>> claims = mapList(input.get("claims"));
        if (claims.isEmpty()) {
            claims = mapList(input.get("validatedClaims"));
        }
        return claims;
    }

    /** Filtra somente os claims que podem ser usados como sinal comercial positivo. */
    private List<Map<String, Object>> acceptedClaims(List<Map<String, Object>> claims) {
        return claims.stream().filter(this::isAccepted).toList();
    }

    /** Verifica se o claim pode contar como evidência comercial positiva. */
    private boolean isAccepted(Map<String, Object> claim) {
        String status = text(claim.get("status")).toUpperCase(Locale.ROOT);
        String epistemicState = text(claim.get("epistemicState")).toUpperCase(Locale.ROOT);
        String evidenceSpan = text(claim.get("exactEvidenceSpan"));
        return !evidenceSpan.isBlank()
                && (status.isBlank() || status.equals("ACCEPT") || status.equals("ACCEPTED"))
                && (epistemicState.isBlank() || epistemicState.equals("VALIDATED"));
    }

    /** Conta sinais separados de atividade, dor, impacto econômico, intenção e compra real. */
    private EvidenceCounts countEvidence(List<Map<String, Object>> claims, List<Map<String, Object>> allClaims) {
        int activities = 0;
        int tasks = 0;
        int pains = 0;
        int economic = 0;
        int purchase = 0;
        int offers = 0;
        int contradictions = 0;
        List<String> domains = new ArrayList<>();
        for (Map<String, Object> claim : claims) {
            String type = text(claim.get("claimType")).toUpperCase(Locale.ROOT);
            if (type.equals("ACTIVITY_EXISTS") || type.equals("AUDIENCE_EXISTS")) {
                activities++;
            }
            if (TASK_TYPES.contains(type)) {
                tasks++;
            }
            if (PAIN_TYPES.contains(type)) {
                pains++;
            }
            if (ECONOMIC_TYPES.contains(type)) {
                economic++;
            }
            if (PURCHASE_TYPES.contains(type)) {
                purchase++;
            }
            if (OFFER_TYPES.contains(type)) {
                offers++;
            }
            String domain = text(claim.get("canonicalDomain"));
            if (!domain.isBlank() && !domains.contains(domain)) {
                domains.add(domain);
            }
        }
        contradictions = (int) allClaims.stream().filter(this::isContradictory).count();
        return new EvidenceCounts(activities, tasks, pains, economic, purchase, offers, contradictions, domains.size());
    }

    /** Identifica contradições explícitas para impedir promoção automática de evidência comercial. */
    private boolean isContradictory(Map<String, Object> claim) {
        String state = text(claim.get("epistemicState")).toUpperCase(Locale.ROOT);
        String relation = text(claim.get("relationToTargetClaim")).toUpperCase(Locale.ROOT);
        return state.equals("CONTRADICTED")
                || relation.equals("CONTRADICTS")
                || Boolean.TRUE.equals(claim.get("contradictsTarget"));
    }

    /** Define o nível E0-E5 sem misturar dor, impacto econômico e intenção de compra. */
    private String evidenceLevel(EvidenceCounts counts) {
        if (counts.offerSignals() > 0) {
            return "E5_REAL_PURCHASE";
        }
        if (counts.purchaseSignals() > 0) {
            return "E4_PURCHASE_INTENT";
        }
        if (counts.economicSignals() > 0
                && counts.painSignals() >= 2
                && counts.taskSignals() >= 3
                && counts.independentDomainCount() >= 3) {
            return "E3_ECONOMIC_PAIN";
        }
        if (counts.painSignals() > 0 && counts.taskSignals() > 0) {
            return "E2_ROUTINE_AND_PAIN";
        }
        if (counts.activitySignals() > 0 || counts.taskSignals() > 0) {
            return "E1_ACTIVITY_EXISTS";
        }
        return "E0_MODEL_HYPOTHESIS";
    }

    /** Calcula confiança explicável por quantidade, diversidade e nível atingido, sem score especulativo do modelo. */
    private double confidence(EvidenceCounts counts) {
        double raw = (counts.taskSignals() * 0.08)
                + (counts.painSignals() * 0.10)
                + (counts.economicSignals() * 0.18)
                + (counts.purchaseSignals() * 0.20)
                + (counts.offerSignals() * 0.24)
                + (Math.min(counts.independentDomainCount(), 5) * 0.06)
                - (counts.contradictionSignals() * 0.18);
        return Math.round(Math.max(0.0, Math.min(0.95, raw)) * 100.0) / 100.0;
    }

    /** Calcula ganho informacional da tentativa atual em relação ao nível anterior informado no snapshot. */
    private double informationGain(Map<String, Object> input, String evidenceLevel, int signalCount) {
        int previousRank = levelRank(text(input.get("previousEvidenceLevel")));
        int currentRank = levelRank(evidenceLevel);
        double gain = Math.max(0, currentRank - previousRank) * 0.20 + Math.min(signalCount, 10) * 0.01;
        return Math.round(gain * 100.0) / 100.0;
    }

    /** Lista lacunas comerciais que impedem avanço ou priorização. */
    private List<String> missingEvidence(EvidenceCounts counts) {
        List<String> missing = new ArrayList<>();
        if (counts.taskSignals() < 3) {
            missing.add("CONCRETE_EXECUTOR_TASKS");
        }
        if (counts.painSignals() < 2) {
            missing.add("DISTINCT_RECURRING_PAINS");
        }
        if (counts.economicSignals() < 1) {
            missing.add("ECONOMIC_IMPACT_OR_WORKAROUND");
        }
        if (counts.purchaseSignals() < 1) {
            missing.add("DIRECT_PURCHASE_INTENT_OR_HIRING_BEHAVIOR");
        }
        if (counts.independentDomainCount() < 3) {
            missing.add("THREE_INDEPENDENT_DOMAINS");
        }
        if (counts.contradictionSignals() > 0) {
            missing.add("RESOLVE_CONTRADICTORY_EVIDENCE");
        }
        return missing;
    }

    /** Decide o próximo movimento operacional a partir do gate calculado. */
    private String gateDecision(boolean automaticMaterializationAllowed, boolean humanReviewRequired, String evidenceLevel, double informationGain) {
        if (automaticMaterializationAllowed) {
            return "MATERIALIZE";
        }
        if (humanReviewRequired) {
            return "HUMAN_REVIEW";
        }
        if (levelRank(evidenceLevel) < 2 || informationGain < 0.10) {
            return "NO_PUBLIC_EVIDENCE";
        }
        return "NEEDS_MORE_RESEARCH";
    }

    /** Traduz decisão comercial para o estágio canônico seguinte. */
    private String nextStageCode(String gateDecision) {
        return switch (gateDecision) {
            case "MATERIALIZE" -> "enriched-niche-materializer";
            case "HUMAN_REVIEW" -> "human-review";
            case "NEEDS_MORE_RESEARCH" -> "adaptive-query-planner";
            default -> "end";
        };
    }

    /** Converte nível E0-E5 em ordem numérica. */
    private int levelRank(String evidenceLevel) {
        String normalized = evidenceLevel.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("E5")) return 5;
        if (normalized.startsWith("E4")) return 4;
        if (normalized.startsWith("E3")) return 3;
        if (normalized.startsWith("E2")) return 2;
        if (normalized.startsWith("E1")) return 1;
        return 0;
    }

    /** Extrai lista de mapas de contratos flexíveis. */
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

    /** Converte valor flexível para booleano seguro. */
    private boolean booleanValue(Object value) {
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(text(value));
    }

    /** Retorna texto seguro para decisões determinísticas. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /** Agrega contagens comerciais separadas para manter a regra auditável. */
    private record EvidenceCounts(
            int activitySignals,
            int taskSignals,
            int painSignals,
            int economicSignals,
            int purchaseSignals,
            int offerSignals,
            int contradictionSignals,
            int independentDomainCount) {
        /** Soma todos os sinais aceitos para cálculo de ganho informacional. */
        int totalSignals() {
            return activitySignals + taskSignals + painSignals + economicSignals + purchaseSignals + offerSignals;
        }

        /** Expõe as contagens no payload persistível de auditoria. */
        Map<String, Object> asMap() {
            return Map.of(
                    "activitySignals", activitySignals,
                    "taskSignals", taskSignals,
                    "painSignals", painSignals,
                    "economicSignals", economicSignals,
                    "purchaseSignals", purchaseSignals,
                    "offerSignals", offerSignals,
                    "contradictionSignals", contradictionSignals,
                    "independentDomainCount", independentDomainCount);
        }
    }
}
