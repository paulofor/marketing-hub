package com.marketinghub.nichocnae.routinequalitygate;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Avalia deterministicamente se um cartão representa público MEI/autônomo real, atual e sem contaminação de solução. */
@Component
public class RoutineQualityGateEngine {
    private static final String MEI_AUDIENCE_READY = "MEI_AUDIENCE_READY";
    private static final String NEEDS_MORE_MEI_RESEARCH = "NEEDS_MORE_MEI_RESEARCH";
    private static final String OUTDATED_SOURCES = "OUTDATED_SOURCES";
    private static final String TOO_CORPORATE = "TOO_CORPORATE";
    private static final String SOLUTION_CONTAMINATED = "SOLUTION_CONTAMINATED";
    private static final String GENERIC = "GENERIC";
    private static final int MAX_ACCEPTABLE_SOLUTION_RISK = 35;
    private static final int MAX_ACCEPTABLE_OUTDATED_RISK = 45;
    private static final int MAX_ACCEPTABLE_CORPORATE_RISK = 45;

    /** Calcula a decisão da etapa sete exigindo sinais humanos/comportamentais de MEI/autônomo antes da materialização. */
    public RoutineQualityDecision evaluate(RoutineQualityGatePending pending) {
        int sourceCount = value(pending.sourceCount());
        int signalCount = value(pending.signalCount());
        int solutionRiskScore = calculateSolutionLanguageRiskScore(pending);
        int textualSolutionRiskScore = calculateTextualSolutionLanguageRiskScore(pending);
        int effectiveSolutionRiskScore = Math.max(Math.max(solutionRiskScore, textualSolutionRiskScore), value(pending.profileSolutionLanguageRiskScore()));
        int outdatedRiskScore = calculateOutdatedSourceRiskScore(pending, sourceCount);
        int corporateRiskScore = calculateCorporateDriftRiskScore(pending, sourceCount);
        int specificityScore = calculateSpecificityScore(pending);
        int confidenceScore = calculateConfidenceScore(pending, sourceCount, signalCount, effectiveSolutionRiskScore, outdatedRiskScore, corporateRiskScore);
        int duplicationScore = calculateDuplicationScore(pending);
        boolean hasRequiredSummaries = hasText(pending.routineSummary()) && hasText(pending.painsSummary());
        boolean hasBrazilianSources = value(pending.brazilianSourceCount()) >= 3;
        boolean hasRecentSources = value(pending.recentSourceCount()) >= 2 || value(pending.sourceFreshnessScore()) >= 70;
        boolean hasAuditableEvidence = hasText(pending.evidenceSummary()) && distinctDomainCount(pending.sourceDomains()) >= 2 && hasBrazilianSources;
        boolean hasRoutineTask = value(pending.routineTaskCount()) > 0;
        boolean hasUsefulCustomerBehaviorSummary = hasUsefulCommercialSummary(pending.customerBehaviorSummary());
        boolean hasUsefulChannelsSummary = hasUsefulCommercialSummary(pending.channelsSummary());
        boolean hasAcquisitionOrChannelCounter = value(pending.customerAcquisitionEvidenceCount()) > 0;
        boolean hasCommercialAcquisitionEvidence = hasAcquisitionOrChannelCounter
                && hasUsefulCustomerBehaviorSummary
                && hasUsefulChannelsSummary;
        boolean hasPracticalPain = value(pending.operationalDifficultyCount()) > 0 || value(pending.painSignalCount()) > 0;
        boolean hasHumanOutcome = value(pending.emotionalOutcomeEvidenceCount()) > 0;
        boolean hasMinimumSignalMix = hasRoutineTask && hasCommercialAcquisitionEvidence && hasPracticalPain && hasHumanOutcome;
        boolean weakAcquisitionOrChannels = !hasCommercialAcquisitionEvidence;
        boolean dominatedBySolution = effectiveSolutionRiskScore > MAX_ACCEPTABLE_SOLUTION_RISK
                || value(pending.solutionLanguageRiskCount()) > signalCount / 2;
        boolean outdated = outdatedRiskScore > MAX_ACCEPTABLE_OUTDATED_RISK || !hasRecentSources;
        boolean tooCorporate = corporateRiskScore > MAX_ACCEPTABLE_CORPORATE_RISK || value(pending.autonomousProfessionalFitScore()) < 50;
        boolean generic = specificityScore < 40 || duplicationScore >= 70 || !hasRequiredSummaries || !hasAuditableEvidence;
        boolean approved = sourceCount >= 3
                && signalCount >= 6
                && specificityScore >= 60
                && confidenceScore >= 50
                && hasRequiredSummaries
                && hasAuditableEvidence
                && hasRecentSources
                && hasMinimumSignalMix
                && value(pending.behavioralEvidenceScore()) >= 55
                && !dominatedBySolution
                && !outdated
                && !tooCorporate
                && !generic;
        String status = chooseStatus(generic, weakAcquisitionOrChannels, dominatedBySolution, outdated, tooCorporate, approved, hasMinimumSignalMix);
        return new RoutineQualityDecision(
                status,
                approved,
                specificityScore,
                confidenceScore,
                duplicationScore,
                buildNotes(
                        pending,
                        status,
                        hasMinimumSignalMix,
                        hasUsefulCustomerBehaviorSummary,
                        hasUsefulChannelsSummary,
                        weakAcquisitionOrChannels,
                        hasAuditableEvidence,
                        hasRecentSources,
                        dominatedBySolution,
                        outdatedRiskScore,
                        corporateRiskScore,
                        effectiveSolutionRiskScore,
                        textualSolutionRiskScore));
    }

    /** Escolhe o status mais útil para orientar nova pesquisa ou bloqueio operacional. */
    private String chooseStatus(
            boolean generic,
            boolean weakAcquisitionOrChannels,
            boolean dominatedBySolution,
            boolean outdated,
            boolean tooCorporate,
            boolean approved,
            boolean hasMinimumSignalMix) {
        if (dominatedBySolution) {
            return SOLUTION_CONTAMINATED;
        }
        if (tooCorporate) {
            return TOO_CORPORATE;
        }
        if (outdated) {
            return OUTDATED_SOURCES;
        }
        if (weakAcquisitionOrChannels) {
            return NEEDS_MORE_MEI_RESEARCH;
        }
        if (generic) {
            return GENERIC;
        }
        if (approved) {
            return MEI_AUDIENCE_READY;
        }
        return hasMinimumSignalMix ? NEEDS_MORE_MEI_RESEARCH : GENERIC;
    }

    /** Calcula especificidade combinando rotina, dores, comportamento MEI/autônomo e variedade de fontes brasileiras. */
    private int calculateSpecificityScore(RoutineQualityGatePending pending) {
        int score = 0;
        score += cappedLengthScore(pending.routineSummary(), 16);
        score += cappedLengthScore(pending.painsSummary(), 16);
        score += cappedLengthScore(pending.resultsSummary(), 10);
        score += Math.min(16, value(pending.sourceDiversityScore()) / 5 + distinctDomainCount(pending.sourceDomains()) * 2);
        score += Math.min(14, value(pending.brazilianSourceCount()) * 3 + value(pending.recentSourceCount()) * 2);
        score += Math.min(28, value(pending.routineTaskCount()) * 4
                + (value(pending.operationalDifficultyCount()) + value(pending.painSignalCount())) * 3
                + value(pending.customerAcquisitionEvidenceCount()) * 5
                + value(pending.emotionalOutcomeEvidenceCount()) * 4
                + (value(pending.questionSignalCount()) + value(pending.languageMarkerCount())));
        if (containsConcreteMarker(pending.routineSummary() + " " + pending.painsSummary() + " " + pending.resultsSummary())) {
            score += 8;
        }
        return clamp(score);
    }

    /** Calcula confiança efetiva com bônus para aderência MEI/autônomo, comportamento e atualidade, penalizando riscos. */
    private int calculateConfidenceScore(
            RoutineQualityGatePending pending, int sourceCount, int signalCount, int solutionRiskScore, int outdatedRiskScore, int corporateRiskScore) {
        int base = value(pending.cardConfidenceScore());
        int sourceScore = Math.min(18, sourceCount * 4 + value(pending.brazilianSourceCount()) * 2 + value(pending.recentSourceCount()) * 2);
        int signalScore = Math.min(18, signalCount * 2);
        int evidenceScore = Math.min(32,
                (value(pending.routineEvidenceScore()) + value(pending.difficultyEvidenceScore())
                        + value(pending.autonomousProfessionalFitScore()) + value(pending.behavioralEvidenceScore())) / 12);
        int riskPenalty = Math.min(45, solutionRiskScore / 3 + outdatedRiskScore / 4 + corporateRiskScore / 4);
        return clamp(Math.round((base * 0.30f) + sourceScore + signalScore + evidenceScore - riskPenalty));
    }

    /** Calcula risco de duplicação/genérico por repetição simples entre os blocos principais. */
    private int calculateDuplicationScore(RoutineQualityGatePending pending) {
        List<String> normalized = List.of(
                normalize(pending.routineSummary()),
                normalize(pending.painsSummary()),
                normalize(pending.resultsSummary()),
                normalize(pending.mechanismOpportunitiesSummary()));
        int duplicatePairs = 0;
        for (int i = 0; i < normalized.size(); i++) {
            for (int j = i + 1; j < normalized.size(); j++) {
                if (StringUtils.hasText(normalized.get(i)) && normalized.get(i).equals(normalized.get(j))) {
                    duplicatePairs++;
                }
            }
        }
        int score = duplicatePairs * 25;
        if (normalized.stream().filter(StringUtils::hasText).distinct().count() <= 2) {
            score += 25;
        }
        return clamp(score);
    }

    /** Calcula o risco consolidado de linguagem de solução vindo do backend e dos contadores de sinais. */
    private int calculateSolutionLanguageRiskScore(RoutineQualityGatePending pending) {
        int requestScore = value(pending.solutionLanguageRiskScore());
        int signalCount = Math.max(1, value(pending.signalCount()));
        int counterScore = clamp((int) Math.round((value(pending.solutionLanguageRiskCount()) * 100.0) / signalCount));
        return Math.max(requestScore, counterScore);
    }

    /** Calcula risco por fontes antigas usando contadores de snapshots e score do perfil MEI/autônomo. */
    private int calculateOutdatedSourceRiskScore(RoutineQualityGatePending pending, int sourceCount) {
        int counterScore = sourceCount <= 0 ? 0 : clamp((int) Math.round((value(pending.outdatedSourceRiskCount()) * 100.0) / sourceCount));
        return Math.max(value(pending.outdatedSourceRiskScore()), counterScore);
    }

    /** Calcula risco de desvio para empresa estruturada usando snapshots e score do perfil MEI/autônomo. */
    private int calculateCorporateDriftRiskScore(RoutineQualityGatePending pending, int sourceCount) {
        int counterScore = sourceCount <= 0 ? 0 : clamp((int) Math.round((value(pending.structuredBusinessDriftRiskCount()) * 100.0) / sourceCount));
        return Math.max(value(pending.structuredBusinessDriftRiskScore()), counterScore);
    }

    /** Calcula risco textual quando o card usa termos de solução mesmo sem contador de risco persistido. */
    private int calculateTextualSolutionLanguageRiskScore(RoutineQualityGatePending pending) {
        String text = normalize(String.join(
                " ",
                nullToEmpty(pending.routineSummary()),
                nullToEmpty(pending.painsSummary()),
                nullToEmpty(pending.resultsSummary()),
                nullToEmpty(pending.mechanismOpportunitiesSummary())));
        int matches = 0;
        for (String term : List.of(
                " ia ",
                " inteligencia artificial ",
                " automacao ",
                " software ",
                " sistema ",
                " app ",
                " ferramenta ",
                " curso ",
                " template ",
                " oferta ",
                " landing page ")) {
            if ((" " + text + " ").contains(term)) {
                matches++;
            }
        }
        return clamp(matches * 12);
    }

    /** Monta notas objetivas para explicar a decisão operacional do gate. */
    private String buildNotes(
            RoutineQualityGatePending pending,
            String status,
            boolean hasMinimumSignalMix,
            boolean hasUsefulCustomerBehaviorSummary,
            boolean hasUsefulChannelsSummary,
            boolean weakAcquisitionOrChannels,
            boolean hasAuditableEvidence,
            boolean hasRecentSources,
            boolean dominatedBySolution,
            int outdatedRiskScore,
            int corporateRiskScore,
            int solutionRiskScore,
            int textualSolutionRiskScore) {
        List<String> notes = new ArrayList<>();
        notes.add("status=" + status);
        notes.add("fontes=" + value(pending.sourceCount()));
        notes.add("fontesBrasileiras=" + value(pending.brazilianSourceCount()));
        notes.add("fontesRecentes=" + value(pending.recentSourceCount()));
        notes.add("sinais=" + value(pending.signalCount()));
        notes.add("tarefasRotina=" + value(pending.routineTaskCount()));
        notes.add("aquisicaoOuCanal=" + value(pending.customerAcquisitionEvidenceCount()));
        notes.add("resumoComportamentoClienteUtil=" + hasUsefulCustomerBehaviorSummary);
        notes.add("resumoCanaisUtil=" + hasUsefulChannelsSummary);
        notes.add("faltaEvidenciaAquisicaoCanaisRecorrenciaOuComportamentoClientes=" + weakAcquisitionOrChannels);
        notes.add("dorPratica=" + (value(pending.operationalDifficultyCount()) + value(pending.painSignalCount())));
        notes.add("dorEmocionalSonhoOuMedo=" + value(pending.emotionalOutcomeEvidenceCount()));
        notes.add("mixMinimoMeiAutonomo=" + hasMinimumSignalMix);
        notes.add("evidenciaAuditavelBrasil=" + hasAuditableEvidence);
        notes.add("fontesRecentesSuficientes=" + hasRecentSources);
        notes.add("riscoFonteAntiga=" + outdatedRiskScore);
        notes.add("riscoEmpresaEstruturada=" + corporateRiskScore);
        notes.add("riscoLinguagemSolucao=" + solutionRiskScore);
        notes.add("riscoTextualSolucao=" + textualSolutionRiskScore);
        notes.add("dominadoPorSolucao=" + dominatedBySolution);
        notes.add("fitMeiAutonomo=" + value(pending.autonomousProfessionalFitScore()));
        notes.add("evidenciaComportamental=" + value(pending.behavioralEvidenceScore()));
        notes.add("fontesDistintas=" + distinctDomainCount(pending.sourceDomains()));
        return String.join("; ", notes);
    }

    /** Converte texto em pontuação por tamanho útil com teto informado. */
    private int cappedLengthScore(String value, int cap) {
        if (!hasText(value)) {
            return 0;
        }
        return Math.min(cap, value.trim().length() / 30);
    }

    /** Conta domínios distintos declarados na síntese para estimar variedade de fontes. */
    private int distinctDomainCount(String sourceDomains) {
        if (!hasText(sourceDomains)) {
            return 0;
        }
        return (int) List.of(sourceDomains.split(",")).stream().map(String::trim).filter(StringUtils::hasText).distinct().count();
    }


    /** Verifica se o resumo comercial contém evidência acionável e não apenas placeholder de ausência de evidência. */
    private boolean hasUsefulCommercialSummary(String value) {
        if (!hasText(value)) {
            return false;
        }
        String normalized = normalize(value);
        if (normalized.length() < 35 || containsInsufficientEvidencePlaceholder(normalized)) {
            return false;
        }
        return containsCommercialActionMarker(normalized);
    }

    /** Identifica frases genéricas que explicitam falta de evidência e não podem virar sinal positivo. */
    private boolean containsInsufficientEvidencePlaceholder(String normalized) {
        return normalized.contains("sem evidencia suficiente")
                || normalized.contains("sem evidencias suficientes")
                || normalized.contains("nao ha evidencia")
                || normalized.contains("nao existe evidencia")
                || normalized.contains("sem dados suficientes")
                || normalized.contains("informacao insuficiente")
                || normalized.contains("nao identificado")
                || normalized.contains("nao foi identificado");
    }

    /** Procura marcadores de aquisição, canal, recorrência ou comportamento de clientes em linguagem operacional. */
    private boolean containsCommercialActionMarker(String normalized) {
        return List.of(
                        "whatsapp",
                        "instagram",
                        "indicacao",
                        "cliente",
                        "clientes",
                        "canal",
                        "agenda",
                        "retorno",
                        "recorr",
                        "orcamento",
                        "atendimento",
                        "telefone",
                        "google",
                        "rede social",
                        "redes sociais",
                        "bairro",
                        "fidelizacao")
                .stream()
                .anyMatch(normalized::contains);
    }

    /** Detecta marcadores concretos que normalmente indicam texto menos genérico e mais comportamental. */
    private boolean containsConcreteMarker(String value) {
        return hasText(value) && value.matches(".*(\\d|WhatsApp|Instagram|agenda|cliente|preço|pacote|retorno|horário|cancelamento|atraso|falta).*");
    }

    /** Normaliza texto para comparação de duplicidade simples. */
    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toLowerCase()
                .replace('á', 'a')
                .replace('à', 'a')
                .replace('ã', 'a')
                .replace('â', 'a')
                .replace('é', 'e')
                .replace('ê', 'e')
                .replace('í', 'i')
                .replace('ó', 'o')
                .replace('õ', 'o')
                .replace('ô', 'o')
                .replace('ú', 'u')
                .replace('ç', 'c')
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Retorna texto vazio para campos opcionais ausentes antes de unir os blocos avaliados. */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Verifica se existe texto útil no valor recebido. */
    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    /** Converte nulo para zero para cálculos determinísticos. */
    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    /** Garante que a pontuação calculada permaneça na escala percentual. */
    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
