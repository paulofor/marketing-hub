package com.marketinghub.nichocnae.meiaudiencesegmenter;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Valida deterministicamente a saída da segmentação para impedir produto, oferta ou solução precoce. */
@Component
public class MeiAudienceSegmenterValidator {
    private static final int MAX_TEXT_LENGTH = 4000;
    private static final List<String> SOLUTION_TERMS = List.of(
            "produto",
            "oferta",
            "preço",
            "promessa",
            "campanha",
            "landing page",
            "software",
            "automacao",
            "automação",
            "inteligencia artificial",
            "inteligência artificial",
            "ia",
            "curso",
            "ferramenta",
            "aplicativo",
            "app",
            "solução",
            "solucao");

    /** Valida presença de público, comportamento, evidências e ausência de linguagem de solução. */
    public void validate(MeiAudienceSegmenterPending input, MeiAudienceSegmentDraft draft) {
        if (input == null || input.researchCycleId() == null) {
            throw new IllegalArgumentException("Entrada de segmentação MEI/autônomo inválida.");
        }
        if (draft == null) {
            throw new IllegalArgumentException("Saída de segmentação MEI/autônomo vazia.");
        }
        requireText(draft.audienceName(), "audienceName");
        requireText(draft.workMode(), "workMode");
        requireText(draft.dailyRoutineSummary(), "dailyRoutineSummary");
        requireText(draft.operationalPainsSummary(), "operationalPainsSummary");
        requireText(draft.recentSourceSummary(), "recentSourceSummary");
        validateScore(draft.autonomousProfessionalFitScore(), "autonomousProfessionalFitScore");
        validateScore(draft.behavioralEvidenceScore(), "behavioralEvidenceScore");
        validateScore(draft.sourceFreshnessScore(), "sourceFreshnessScore");
        validateScore(draft.outdatedSourceRiskScore(), "outdatedSourceRiskScore");
        validateScore(draft.structuredBusinessDriftRiskScore(), "structuredBusinessDriftRiskScore");
        validateScore(draft.solutionLanguageRiskScore(), "solutionLanguageRiskScore");
        rejectSolutionLanguage(draft);
    }

    /** Retorna o primeiro termo proibido encontrado no rascunho comportamental. */
    public String firstForbiddenTerm(MeiAudienceSegmentDraft draft) {
        if (draft == null) {
            return null;
        }
        String normalized = normalizedDraft(draft);
        for (String term : SOLUTION_TERMS) {
            if (containsTerm(normalized, term)) {
                return term;
            }
        }
        return null;
    }

    /** Exige texto preenchido dentro do limite operacional do perfil. */
    private void requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Campo obrigatório vazio na segmentação MEI/autônomo: " + fieldName);
        }
        if (value.trim().length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(fieldName + " deve conter no máximo " + MAX_TEXT_LENGTH + " caracteres.");
        }
    }

    /** Garante pontuação percentual válida. */
    private void validateScore(Integer value, String fieldName) {
        if (value == null || value < 0 || value > 100) {
            throw new IllegalArgumentException(fieldName + " deve estar entre 0 e 100.");
        }
    }

    /** Bloqueia qualquer vocabulário de oferta, produto, campanha ou solução no artefato final. */
    private void rejectSolutionLanguage(MeiAudienceSegmentDraft draft) {
        String forbiddenTerm = firstForbiddenTerm(draft);
        if (forbiddenTerm != null) {
            throw new IllegalArgumentException("Segmentação contaminada por linguagem de solução: " + forbiddenTerm);
        }
    }

    /** Junta somente campos do perfil comportamental final para verificação de contaminação. */
    private String normalizedDraft(MeiAudienceSegmentDraft draft) {
        return normalize(" " + draft.audienceName() + " " + draft.occupationTerms() + " " + draft.workMode() + " "
                + draft.customerAcquisitionBehavior() + " " + draft.dailyRoutineSummary() + " " + draft.recurringTasksSummary() + " "
                + draft.operationalPainsSummary() + " " + draft.emotionalPainsSummary() + " " + draft.dreamsSummary() + " "
                + draft.fearsSummary() + " " + draft.languagePatterns() + " " + draft.channelsUsed() + " " + draft.recentSourceSummary() + " ");
    }

    /** Verifica termo completo com bordas de espaço para reduzir falsos positivos. */
    private boolean containsTerm(String normalized, String term) {
        return (" " + normalized + " ").contains(" " + normalize(term).trim() + " ");
    }

    /** Normaliza texto removendo acentos para comparação de termos proibidos. */
    private String normalize(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").replaceAll("\\s+", " ");
    }
}
