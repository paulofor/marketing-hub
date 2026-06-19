package com.marketinghub.nichocnaev2.pipeline.enrichednichematerializer;

import com.marketinghub.nichocnaev2.pipeline.StageArtifact;
import com.marketinghub.nichocnaev2.pipeline.StageContext;
import com.marketinghub.nichocnaev2.pipeline.StageProcessor;
import com.marketinghub.nichocnaev2.pipeline.StageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Materializa no executor externo somente nichos enriquecidos aprovados pelos gates comerciais da v2. */
public final class EnrichedNicheMaterializerProcessor implements StageProcessor {
    /** Monta a decisão final sem permitir publicação automática quando faltam gate, evidência E3 ou feature flag. */
    @Override
    public StageResult process(StageContext context) {
        Map<String, Object> input = context.input();
        boolean materializationEnabled = Boolean.TRUE.equals(input.get("materializationEnabled"));
        String gateDecision = text(input.get("gateDecision")).toUpperCase(Locale.ROOT);
        String validationLevel = firstText(input.get("validationLevel"), input.get("evidenceLevel"));
        double confidence = number(input.get("confidence"));
        boolean approvedGate = "MATERIALIZE".equals(gateDecision) || "APPROVED".equals(gateDecision);
        boolean hasCommercialEvidence = validationLevel.startsWith("E3") || validationLevel.startsWith("E4") || validationLevel.startsWith("E5");
        boolean canMaterialize = materializationEnabled && approvedGate && hasCommercialEvidence;

        Map<String, Object> enrichedNiche = new LinkedHashMap<>();
        enrichedNiche.put("executor", input.get("executor"));
        enrichedNiche.put("jobContext", input.get("jobContext"));
        enrichedNiche.put("pain", input.get("pain"));
        enrichedNiche.put("desiredResult", input.get("desiredResult"));
        enrichedNiche.put("plausibleMechanism", input.get("plausibleMechanism"));
        enrichedNiche.put("supportingClaimIds", input.get("supportingClaimIds"));
        enrichedNiche.put("sourceDomains", input.get("sourceDomains"));
        enrichedNiche.put("validationLevel", validationLevel);
        enrichedNiche.put("confidence", confidence);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stage", "enriched-niche-materializer");
        output.put("materializationDecision", canMaterialize ? "MATERIALIZE" : "DO_NOT_MATERIALIZE");
        output.put("validationLevel", validationLevel);
        output.put("confidence", confidence);
        output.put("materializationEnabled", materializationEnabled);
        output.put("blockingReasons", blockingReasons(materializationEnabled, approvedGate, hasCommercialEvidence));
        if (canMaterialize) {
            output.put("enrichedNiche", enrichedNiche);
        }

        return new StageResult(canMaterialize ? "ENRICHED_NICHE_READY" : "MATERIALIZATION_BLOCKED", output, List.of(new StageArtifact(
                "ENRICHED_NICHE_MATERIALIZATION_DECISION",
                "inline://enriched-niche-materializer/decision",
                "Decisão final da v2 calculada no executor, preservando backend apenas como leitura e escrita.")));
    }

    /** Explica objetivamente por que a publicação comercial foi bloqueada. */
    private List<String> blockingReasons(boolean materializationEnabled, boolean approvedGate, boolean hasCommercialEvidence) {
        java.util.ArrayList<String> reasons = new java.util.ArrayList<>();
        if (!materializationEnabled) {
            reasons.add("Feature flag de materialização automática desativada para calibração da v2.");
        }
        if (!approvedGate) {
            reasons.add("Gate comercial anterior não aprovou materialização.");
        }
        if (!hasCommercialEvidence) {
            reasons.add("Nível mínimo E3 de evidência comercial não foi comprovado.");
        }
        return reasons;
    }

    /** Retorna o primeiro texto não vazio entre campos equivalentes do contrato. */
    private String firstText(Object first, Object second) {
        String firstValue = text(first);
        return firstValue.isBlank() ? text(second) : firstValue;
    }

    /** Converte valores numéricos flexíveis do contrato para double seguro. */
    private double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(text(value));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    /** Retorna texto seguro para regras determinísticas. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
