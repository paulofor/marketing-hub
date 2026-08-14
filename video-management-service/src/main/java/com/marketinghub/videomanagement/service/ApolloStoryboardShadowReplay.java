package com.marketinghub.videomanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Responsabilidade: comparar storyboards de Apolo em replay sem executar provider ou autorizar gasto. */
@Service
public class ApolloStoryboardShadowReplay {
    private static final Set<String> REQUIRED_ROLES = Set.of("HOOK_DOR", "RESULTADO", "MECANISMO", "CTA");
    private final ApolloStoryboardPlanner planner;

    /** Recebe somente o validador determinístico usado pelo fluxo real de Apolo. */
    public ApolloStoryboardShadowReplay(ApolloStoryboardPlanner planner) {
        this.planner = planner;
    }

    /** Reproduz uma execução congelada e decide se a candidata pode avançar para modo sombra online. */
    public ReplayComparison compare(JsonNode metadata, JsonNode currentPlan, JsonNode candidatePlan,
                                    String providerName) {
        PlanEvaluation current = evaluate(metadata, currentPlan, providerName);
        PlanEvaluation candidate = evaluate(metadata, candidatePlan, providerName);
        boolean eligible = candidate.gateApproved()
                && candidate.qualityScore() >= 70
                && candidate.qualityScore() > current.qualityScore()
                && candidate.expectedCredits() <= plannedCredits(metadata);
        String decision = eligible
                ? "CANDIDATE_ELIGIBLE_FOR_ONLINE_SHADOW"
                : "CANDIDATE_BLOCKED_WITHOUT_PROVIDER_CALL";
        return new ReplayComparison("APOLLO", true, false, false, current, candidate, decision);
    }

    /** Avalia qualidade, diversidade, cobertura comercial e orçamento sem integração externa. */
    private PlanEvaluation evaluate(JsonNode metadata, JsonNode plan, String providerName) {
        ApolloStoryboardPlanner.GateDecision gate = planner.validate(metadata, plan, providerName);
        JsonNode cuts = plan.path("cuts");
        Set<String> roles = new HashSet<>();
        Set<String> objectives = new HashSet<>();
        int separatedText = 0;
        int reused = 0;
        for (JsonNode cut : cuts) {
            roles.add(cut.path("commercialRole").asText());
            objectives.add(normalize(cut.path("visualObjective").asText()));
            if (!containsEmbeddedText(cut.path("visualObjective").asText())) separatedText++;
            if (cut.path("reuseExistingMaterial").asBoolean()) reused++;
        }
        int count = cuts.isArray() ? cuts.size() : 0;
        int coverage = (int) Math.round(25.0 * roles.stream().filter(REQUIRED_ROLES::contains).count()
                / REQUIRED_ROLES.size());
        int diversity = count == 0 ? 0 : (int) Math.round(25.0 * objectives.size() / count);
        int postProduction = count == 0 ? 0 : (int) Math.round(15.0 * separatedText / count);
        int reuseScore = count == 0 ? 0 : (int) Math.round(5.0 * reused / count);
        int score = Math.min(100, (gate.approved() ? 30 : 0) + coverage + diversity + postProduction + reuseScore);
        int expectedCredits = gate.approved() ? gate.expectedCredits() : plannedCredits(metadata);
        BigDecimal expectedCost = gate.approved() ? gate.expectedCostUsd() : metadata.path("expectedCostUsd").decimalValue();
        return new PlanEvaluation(score, gate.approved(), gate.reason(), expectedCredits, expectedCost,
                roles.size(), objectives.size(), count, separatedText, reused);
    }

    /** Recupera a linha de base financeira congelada sem consultar saldo ou provider. */
    private int plannedCredits(JsonNode metadata) {
        int persisted = metadata.path("expectedCredits").asInt(0);
        if (persisted > 0) return persisted;
        return metadata.path("sceneCount").asInt(0)
                * metadata.path("providerClipDurationSeconds").asInt(0) * 30;
    }

    /** Detecta comandos visuais que deveriam ser materializados somente na pós-produção. */
    private boolean containsEmbeddedText(String value) {
        return normalize(value).matches(".*\\b(texto|legenda|palavra|preco|logo|cta escrito|interface)\\b.*");
    }

    /** Normaliza objetivos para medir repetição sem diferenças cosméticas. */
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9áàâãéêíóôõúç ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    /** Consolida a comparação auditável entre a versão atual e a candidata. */
    public record ReplayComparison(String agent, boolean shadowMode, boolean providerCalled,
                                   boolean spendingAuthorized, PlanEvaluation current,
                                   PlanEvaluation candidate, String decision) {}

    /** Expõe as métricas objetivas calculadas para uma versão do storyboard. */
    public record PlanEvaluation(int qualityScore, boolean gateApproved, String gateReason,
                                 int expectedCredits, BigDecimal expectedCostUsd, int commercialRoles,
                                 int distinctObjectives, int cuts, int postProductionSafeCuts,
                                 int reusedCuts) {}
}
