package com.marketinghub.nichocnae.evidencelevelgate;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Calcula no executor externo o gate comercial E0-E5 sem transferir inteligência para o backend. */
@Component
public class EvidenceLevelGateEngine {
    /** Classifica a evidência por camadas comerciais mínimas antes da materialização automática. */
    public EvidenceLevelGateDecision evaluate(EvidenceLevelGatePending pending) {
        int confidence = clamp(avg(value(pending.confidenceScore()), value(pending.qualityConfidenceScore()), value(pending.specificityScore())));
        boolean activity = hasText(pending.routineSummary()) && value(pending.routineEvidenceScore()) >= 45;
        boolean practicalPain = hasText(pending.painsSummary()) && value(pending.difficultyEvidenceScore()) >= 45;
        boolean economicImpact = containsAny(pending.resultsSummary(), "custo", "perda", "atras", "retrabalho", "renda", "cliente", "agenda", "cancel");
        boolean purchaseIntent = containsAny(pending.evidenceSummary(), "contratar", "preço", "orcamento", "orçamento", "whatsapp", "indicação", "pedido");
        boolean independentSources = countDomains(pending.sourceDomains()) >= 2 && value(pending.sourceDiversityScore()) >= 45;
        String level = chooseLevel(activity, practicalPain, economicImpact, purchaseIntent, independentSources);
        boolean approved = ("E3".equals(level) || "E4".equals(level) || "E5".equals(level)) && confidence >= 55;
        String status = approved ? "APPROVED_FOR_MATERIALIZATION" : "INSUFFICIENT_COMMERCIAL_EVIDENCE";
        return new EvidenceLevelGateDecision(level, status, approved, confidence, rejectionReasons(activity, practicalPain, economicImpact, purchaseIntent, independentSources), nextMovements(level));
    }

    /** Escolhe o nível mais alto sustentado pelas camadas de evidência. */
    private String chooseLevel(boolean activity, boolean pain, boolean impact, boolean intent, boolean sources) {
        if (activity && pain && impact && intent && sources) return "E5";
        if (activity && pain && impact && sources) return "E4";
        if (activity && pain && sources) return "E3";
        if (activity && sources) return "E2";
        if (activity) return "E1";
        return "E0";
    }

    /** Resume motivos que impedem avanço comercial. */
    private String rejectionReasons(boolean activity, boolean pain, boolean impact, boolean intent, boolean sources) {
        StringBuilder reasons = new StringBuilder();
        if (!activity) reasons.append("atividade_nao_comprovada;");
        if (!pain) reasons.append("dor_pratica_insuficiente;");
        if (!impact) reasons.append("impacto_economico_nao_demonstrado;");
        if (!intent) reasons.append("intencao_compra_nao_observada;");
        if (!sources) reasons.append("fontes_independentes_insuficientes;");
        return reasons.isEmpty() ? "" : reasons.toString();
    }

    /** Define o próximo movimento recomendado para reprocessamento cognitivo ou materialização. */
    private String nextMovements(String level) {
        return switch (level) {
            case "E0", "E1" -> "voltar ao planejador de queries para provar atividade e rotina real";
            case "E2" -> "buscar dor prática literal em fontes diretas";
            case "E3" -> "materialização controlada ou buscar impacto econômico para maior confiança";
            case "E4" -> "materialização controlada e buscar intenção de compra para escala";
            default -> "materialização automática permitida pela evidência comercial";
        };
    }

    /** Verifica texto útil. */
    private boolean hasText(String value) { return StringUtils.hasText(value); }

    /** Verifica presença de marcadores comerciais simples. */
    private boolean containsAny(String raw, String... needles) {
        String text = raw == null ? "" : raw.toLowerCase();
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }

    /** Conta domínios separados por vírgula. */
    private int countDomains(String domains) {
        if (!StringUtils.hasText(domains)) return 0;
        return (int) java.util.Arrays.stream(domains.split(",")).map(String::trim).filter(StringUtils::hasText).distinct().count();
    }

    /** Calcula média inteira. */
    private int avg(int a, int b, int c) { return Math.round((a + b + c) / 3.0f); }

    /** Normaliza nulos para zero. */
    private int value(Integer value) { return value == null ? 0 : value; }

    /** Limita score entre 0 e 100. */
    private int clamp(int value) { return Math.max(0, Math.min(100, value)); }
}
