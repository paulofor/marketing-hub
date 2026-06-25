package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.model.WorkerDtos.MarketWarmupClaimedJob;
import org.junit.jupiter.api.Test;

/**
 * Valida a geração de queries comerciais da pesquisa de aquecimento.
 */
class MarketWarmupQueryBuilderTest {
    /**
     * Garante que a busca use dados da análise comercial e não apenas a URL da página.
     */
    @Test
    void shouldBuildQueriesFromCommercialAnalysis() {
        MarketWarmupQueryBuilder builder = new MarketWarmupQueryBuilder();
        MarketWarmupClaimedJob job = new MarketWarmupClaimedJob(
                1L,
                10L,
                "workspace-001",
                "https://example.com/oferta",
                "Produto Sono Profundo",
                "Especialista do Sono",
                "Curso para dormir melhor",
                "rotina de respiração guiada",
                "acabar com insônia sem remédios",
                "depoimentos de alunos");

        assertThat(builder.buildQueries(job))
                .hasSizeGreaterThanOrEqualTo(8)
                .anySatisfy(query -> assertThat(query).contains("\"Especialista do Sono\"").contains("Instagram YouTube TikTok"))
                .anySatisfy(query -> assertThat(query).contains("Produto Sono Profundo"))
                .anySatisfy(query -> assertThat(query).contains("aula gratuita"));
    }

    /**
     * Garante que nomes ambíguos usem domínio e subtítulo para evitar resultados genéricos.
     */
    @Test
    void shouldBuildSpecificQueriesForAmbiguousProductName() {
        MarketWarmupQueryBuilder builder = new MarketWarmupQueryBuilder();
        MarketWarmupClaimedJob job = new MarketWarmupClaimedJob(
                5L,
                142L,
                "workspace-001",
                "https://www.andreavermont.online/andreia?src=htm_page",
                "AndreIA - Terapeuta de Bolso",
                "Force Academy Cursos",
                null,
                null,
                null,
                null,
                "texto completo da pagina");

        assertThat(builder.buildQueries(job))
                .anySatisfy(query -> assertThat(query).contains("\"andreavermont.online\""))
                .anySatisfy(query -> assertThat(query).contains("\"AndreIA\"").contains("\"Terapeuta de Bolso\""));
    }
}
