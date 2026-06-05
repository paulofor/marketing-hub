package com.marketinghub.nichocnae.sourcesearcher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a classificação determinística de intenção das fontes públicas da etapa três. */
class SourceIntentClassifierTest {

    /** Deve priorizar fonte de rotina real com escore alto e sem risco comercial. */
    @Test
    void shouldClassifyRoutineEvidenceAsPrincipalPublicSource() {
        SourceIntentClassifier classifier = new SourceIntentClassifier();

        SourceSearchResult result = classifier.classify(new SourceSearchResult(
                "https://forum.example/rotina-manicure",
                "Rotina e dificuldades do dia a dia de manicure",
                "Perguntas reais sobre tarefas, atendimento e problemas operacionais.",
                "forum.example",
                1,
                null,
                null,
                false,
                false));

        assertThat(result.sourceIntent()).isEqualTo("ROUTINE_REPORT");
        assertThat(result.routineEvidenceScore()).isGreaterThanOrEqualTo(80);
        assertThat(result.commercialPageRisk()).isFalse();
    }

    /** Deve marcar página comercial com linguagem de solução como risco de contaminação. */
    @Test
    void shouldClassifyCommercialSolutionPageAsContaminationRisk() {
        SourceIntentClassifier classifier = new SourceIntentClassifier();

        SourceSearchResult result = classifier.classify(new SourceSearchResult(
                "https://hotmart.com/produto-agenda",
                "Software para vender mais serviços de manicure",
                "Compre a solução e agende uma demonstração da plataforma.",
                "hotmart.com",
                2,
                null,
                null,
                false,
                false));

        assertThat(result.sourceIntent()).isEqualTo("COMMERCIAL_PAGE_RISK");
        assertThat(result.commercialPageRisk()).isTrue();
        assertThat(result.solutionLanguageRisk()).isTrue();
        assertThat(result.routineEvidenceScore()).isLessThanOrEqualTo(20);
    }
}
