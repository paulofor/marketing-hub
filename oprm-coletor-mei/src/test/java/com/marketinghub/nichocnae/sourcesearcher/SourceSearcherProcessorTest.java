package com.marketinghub.nichocnae.sourcesearcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a orquestração da etapa três entre provedor de busca, backend e StageProcessor. */
class SourceSearcherProcessorTest {

    /** Deve executar a query no provedor público e concluir a etapa três no backend com métricas estruturadas. */
    @Test
    void shouldSearchProviderAndCompleteSourceSearcherOutput() {
        PublicSourceSearchProvider searchProvider = mock(PublicSourceSearchProvider.class);
        SourceSearcherBackendClient backendClient = mock(SourceSearcherBackendClient.class);
        SourceSearcherProcessor processor = new SourceSearcherProcessor(searchProvider, backendClient);
        SourceSearcherPending pending = pending();
        List<SourceSearchResult> searchResults = List.of(new SourceSearchResult(
                "https://exemplo.com/agenda",
                "Como lotar agenda de manicure",
                "Resumo público da fonte",
                "exemplo.com",
                1));
        SourceSearcherOutput output = output();
        when(searchProvider.search(pending.queryText(), 20)).thenReturn(searchResults);
        when(searchProvider.providerCode()).thenReturn("DUCKDUCKGO_HTML");
        when(backendClient.completeStageExecution(pending, "DUCKDUCKGO_HTML", searchResults)).thenReturn(output);

        var result = processor.process(new StageContext<>(
                new StageExecution<>("job-1", pending, Map.of()),
                pending,
                (artifact, content) -> artifact,
                Map.of()));

        assertThat(result.output()).isEqualTo(output);
        assertThat(result.metrics())
                .containsEntry("researchQueryId", 2001L)
                .containsEntry("researchCycleId", 1001L)
                .containsEntry("resultCount", 1)
                .containsEntry("searchProvider", "DUCKDUCKGO_HTML");
        verify(backendClient).completeStageExecution(pending, "DUCKDUCKGO_HTML", searchResults);
    }

    /** Cria uma pendência mínima para a etapa três. */
    private SourceSearcherPending pending() {
        return new SourceSearcherPending(
                2001L,
                1001L,
                3001L,
                "como lotar agenda de manicure",
                "SALES_PAIN_DISCOVERY",
                "GENERAL_WEB",
                1,
                "PENDING",
                0,
                Instant.parse("2026-06-04T00:00:00Z"),
                Instant.parse("2026-06-04T00:00:00Z"));
    }

    /** Cria uma saída mínima suficiente para validar o processor. */
    private SourceSearcherOutput output() {
        return new SourceSearcherOutput(
                2001L,
                1001L,
                "como lotar agenda de manicure",
                "COMPLETED",
                1,
                1,
                List.of(new SourceCandidateResponse(
                        4001L,
                        1001L,
                        2001L,
                        "https://exemplo.com/agenda",
                        "Como lotar agenda de manicure",
                        "Resumo público da fonte",
                        "exemplo.com",
                        "GENERAL_WEB",
                        "DUCKDUCKGO_HTML",
                        1,
                        "FOUND",
                        Instant.parse("2026-06-04T00:01:00Z"),
                        Instant.parse("2026-06-04T00:01:00Z"))));
    }
}
