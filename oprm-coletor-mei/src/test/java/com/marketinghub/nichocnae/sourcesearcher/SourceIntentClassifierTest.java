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

    /** Deve promover fonte brasileira recente e aderente a MEI/autônomo acima de conteúdo sem data. */
    @Test
    void shouldClassifyFreshBrazilianAutonomousSourceIndicators() {
        SourceIntentClassifier classifier = new SourceIntentClassifier();

        SourceSearchResult result = classifier.classify(new SourceSearchResult(
                "https://sebrae.com.br/mei-manicure-2026",
                "MEI manicure: rotina do profissional autônomo em 2026",
                "Microempreendedor individual explica atendimento, cobrança e dúvidas de clientes no Brasil.",
                "sebrae.com.br",
                1,
                null,
                null,
                false,
                false));

        assertThat(result.sourceClassificationType()).isEqualTo("BRAZILIAN_OFFICIAL_SOURCE");
        assertThat(result.sourceFreshnessScore()).isEqualTo(100);
        assertThat(result.outdatedSourceRisk()).isFalse();
        assertThat(result.brazilRelevanceScore()).isGreaterThanOrEqualTo(80);
        assertThat(result.autonomousProfessionalEvidenceScore()).isGreaterThanOrEqualTo(60);
        assertThat(result.structuredBusinessDriftRisk()).isFalse();
        assertThat(result.publishedAt()).isNotNull();
    }


    /** Deve marcar fonte antiga mesmo quando o conteúdo tem indícios de rotina operacional. */
    @Test
    void shouldClassifyOldSourceAsOutdatedRisk() {
        SourceIntentClassifier classifier = new SourceIntentClassifier();

        SourceSearchResult result = classifier.classify(new SourceSearchResult(
                "https://blog.example.com.br/mei-manicure-2020",
                "Rotina da manicure MEI em 2020",
                "Profissional autônomo relata agenda, atendimento e cobrança no Brasil.",
                "blog.example.com.br",
                3,
                null,
                null,
                false,
                false));

        assertThat(result.sourceClassificationType()).isEqualTo("OLD_OR_UNDATED_CONTENT");
        assertThat(result.outdatedSourceRisk()).isTrue();
        assertThat(result.sourceFreshnessScore()).isLessThan(70);
    }

    /** Deve sinalizar desvio corporativo quando a fonte fala de empresa estruturada em vez do dono-operador. */
    @Test
    void shouldClassifyStructuredCompanyContentAsCorporateDriftRisk() {
        SourceIntentClassifier classifier = new SourceIntentClassifier();

        SourceSearchResult result = classifier.classify(new SourceSearchResult(
                "https://pesquisa.example.com.br/franquia-beleza-2026",
                "Estudo sobre franquia de beleza em 2026",
                "Grande empresa com departamento, equipe comercial e rede de lojas no Brasil.",
                "pesquisa.example.com.br",
                4,
                null,
                null,
                false,
                false));

        assertThat(result.sourceClassificationType()).isEqualTo("STRUCTURED_COMPANY_CONTENT");
        assertThat(result.structuredBusinessDriftRisk()).isTrue();
        assertThat(result.autonomousProfessionalEvidenceScore()).isLessThan(40);
    }

    /** Deve aumentar o escore de fontes com execução prática, CBO, guias e relatos profissionais. */
    @Test
    void shouldBoostPracticalExecutionProfessionalSources() {
        SourceIntentClassifier classifier = new SourceIntentClassifier();

        SourceSearchResult result = classifier.classify(new SourceSearchResult(
                "https://ocupacoes.example.com.br/cbo-manicure-2026",
                "CBO manicure pedicure: guia profissional da rotina executada",
                "Relato de profissional sobre procedimentos de atendimento cliente, higiene e esterilização no dia a dia.",
                "ocupacoes.example.com.br",
                1,
                null,
                null,
                false,
                false));

        assertThat(result.sourceIntent()).isEqualTo("ROUTINE_REPORT");
        assertThat(result.sourceClassificationType()).isEqualTo("REAL_PROFESSIONAL_REPORT_OR_QUESTION");
        assertThat(result.routineEvidenceScore()).isGreaterThanOrEqualTo(90);
        assertThat(result.commercialPageRisk()).isFalse();
    }

    /** Deve penalizar venda de sistema quando a página não descreve tarefas concretas do executor. */
    @Test
    void shouldPenalizeSoftwareAgendaPagesWithoutConcreteExecutorTasks() {
        SourceIntentClassifier classifier = new SourceIntentClassifier();

        SourceSearchResult result = classifier.classify(new SourceSearchResult(
                "https://agenda.example.com/salao",
                "Sistema com agenda online e app para salão",
                "Automação, plataforma e software para vender mais com reservas online.",
                "agenda.example.com",
                2,
                null,
                null,
                false,
                false));

        assertThat(result.sourceIntent()).isEqualTo("COMMERCIAL_PAGE_RISK");
        assertThat(result.sourceClassificationType()).isEqualTo("COMMERCIAL_PAGE");
        assertThat(result.commercialPageRisk()).isTrue();
        assertThat(result.routineEvidenceScore()).isLessThanOrEqualTo(20);
    }

    /** Deve separar fonte de solução pública de fonte de rotina mesmo fora de marketplace comercial. */
    @Test
    void shouldClassifyPublicSolutionLanguageAsContaminationRisk() {
        SourceIntentClassifier classifier = new SourceIntentClassifier();

        SourceSearchResult result = classifier.classify(new SourceSearchResult(
                "https://blog.example.com.br/ferramenta-agenda-2026",
                "Ferramenta para automatizar agenda de manicure",
                "Solução com app, plataforma e planos para organizar clientes e vender mais.",
                "blog.example.com.br",
                3,
                null,
                null,
                false,
                false));

        assertThat(result.sourceIntent()).isEqualTo("COMMERCIAL_PAGE_RISK");
        assertThat(result.sourceClassificationType()).isEqualTo("COMMERCIAL_PAGE");
        assertThat(result.solutionLanguageRisk()).isTrue();
        assertThat(result.commercialPageRisk()).isTrue();
        assertThat(result.routineEvidenceScore()).isLessThanOrEqualTo(20);
    }

    /** Deve valorizar evidência de atendimento real, fidelização e linguagem própria do executor. */
    @Test
    void shouldBoostRealWorkEvidenceAndKeepItNonCommercial() {
        SourceIntentClassifier classifier = new SourceIntentClassifier();

        SourceSearchResult result = classifier.classify(new SourceSearchResult(
                "https://relatos.example.com.br/manicure-rotina-2026",
                "Minha rotina manual de manicure autônoma no atendimento real",
                "Relato com indicação, boca a boca, fidelização, recorrência, medo de cliente desmarcar e como cobrar cliente.",
                "relatos.example.com.br",
                1,
                null,
                null,
                false,
                false));

        assertThat(result.sourceIntent()).isEqualTo("ROUTINE_REPORT");
        assertThat(result.routineEvidenceScore()).isGreaterThanOrEqualTo(90);
        assertThat(result.commercialPageRisk()).isFalse();
        assertThat(result.solutionLanguageRisk()).isFalse();
    }

}
