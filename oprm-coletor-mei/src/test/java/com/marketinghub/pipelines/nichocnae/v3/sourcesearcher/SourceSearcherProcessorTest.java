package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pipelines.nichocnae.v3.core.StageContext;
import com.marketinghub.pipelines.nichocnae.v3.core.StageResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Valida a decisão de avanço da etapa source-searcher no pipeline NichoCNAE v3. */
class SourceSearcherProcessorTest {
    /** Bloqueia avanço quando existem apenas queries planejadas, mas nenhuma fonte real auditável. */
    @Test
    void shouldBlockWithoutRealSources() {
        StageResult result = new SourceSearcherProcessor((query, limit) -> List.of()).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "gerente loja rotina estoque")))));

        assertThat(result.status()).isEqualTo("FONTES_NAO_COLETADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "");
        assertThat(result.output()).containsEntry("blocked", true);
        assertThat(result.output()).containsEntry("recommendedCorrectionStage", "source-searcher");
        assertThat((List<?>) result.output().get("foundSources")).isEmpty();
    }

    /** Permite avanço somente quando a entrada traz fontes reais para o source-fetcher coletar. */
    @Test
    void shouldAdvanceWithRealFoundSources() {
        StageResult result = new SourceSearcherProcessor().process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "gerente loja rotina estoque")),
                "foundSources", List.of(Map.of(
                        "url", "https://exemplo.com.br/rotina-loja",
                        "title", "Rotina de MEI autônomo em loja no Brasil",
                        "snippet", "Profissional autônomo acompanha estoque, agenda, atendimento, clientes e cobrança diariamente.")))));

        assertThat(result.status()).isEqualTo("FONTES_ENCONTRADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "source-fetcher");
        assertThat(result.output()).containsEntry("blocked", false);
        assertThat(result.output()).containsEntry("foundSourceCount", 1);
        assertThat((List<?>) result.output().get("foundSources")).hasSize(1);
        assertThat((List<?>) result.output().get("selectedSources")).hasSize(1);
    }

    /** Busca fontes públicas a partir das queries e entrega somente fontes qualificadas para a próxima etapa. */
    @Test
    void shouldSearchAndDeliverQualifiedRoutineSources() {
        SourceSearchClient searchClient = (query, limit) -> List.of(
                new SourceSearchResult(
                        "Rotina de profissional MEI no Brasil",
                        "https://rotina.example.com.br/mei",
                        "Profissional autônomo relata agenda, atendimento, clientes, cobrança e retrabalho manual.",
                        "TEST_PROVIDER",
                        "<item />"),
                new SourceSearchResult(
                        "Sistema para automatizar vendas",
                        "https://software.example.com/app",
                        "Contrate plataforma, planos, CRM, automação e teste grátis.",
                        "TEST_PROVIDER",
                        "<item />"));

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of(
                        "query", "manicure agenda clientes",
                        "intent", "TAREFA_DIARIA",
                        "objective", "Validar rotina de atendimento")))));

        assertThat(result.status()).isEqualTo("FONTES_ENCONTRADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "source-fetcher");
        List<?> selectedSources = (List<?>) result.output().get("selectedSources");
        assertThat(selectedSources).hasSize(1);
        Map<?, ?> source = (Map<?, ?>) selectedSources.getFirst();
        assertThat(source.get("sourceIntent")).isEqualTo("ROUTINE_EVIDENCE");
        assertThat(source.get("searchProvider")).isEqualTo("TEST_PROVIDER");
        assertThat(source.containsKey("routineEvidenceScore")).isTrue();
        assertThat(source.containsKey("brazilRelevanceScore")).isTrue();
    }

    /** Bloqueia avanço quando a busca retorna apenas fonte comercial ou solução contaminada. */
    @Test
    void shouldBlockCommercialSolutionSources() {
        SourceSearchClient searchClient = (query, limit) -> List.of(new SourceSearchResult(
                "Sistema de agenda para vender mais",
                "https://software.example.com.br/precos",
                "Contrate software, aplicativo, CRM, automação, planos e teste grátis.",
                "TEST_PROVIDER",
                "<item />"));

        StageResult result = new SourceSearcherProcessor(searchClient).process(new StageContext("job", "5", Map.of(
                "plannedQueries", List.of(Map.of("query", "manicure agenda clientes")))));

        assertThat(result.status()).isEqualTo("FONTES_NAO_COLETADAS");
        assertThat(result.output()).containsEntry("nextStageCode", "");
        assertThat((List<?>) result.output().get("selectedSources")).isEmpty();
    }
}
