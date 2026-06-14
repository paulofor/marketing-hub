package com.marketinghub.nichocnae.sourcesearcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: validar a orquestração da etapa três entre provedor de busca, backend e StageProcessor. */
class SourceSearcherProcessorTest {

    /** Deve executar a query no provedor público e concluir a etapa três no backend com métricas estruturadas. */
    @Test
    void shouldSearchProviderAndCompleteSourceSearcherOutput() {
        PublicSourceSearchProvider searchProvider = mock(PublicSourceSearchProvider.class);
        SourceSearcherBackendClient backendClient = mock(SourceSearcherBackendClient.class);
        SourceSearcherProcessor processor =
                new SourceSearcherProcessor(searchProvider, backendClient, new SourceIntentClassifier());
        SourceSearcherPending pending = pending();
        List<SourceSearchResult> searchResults = List.of(new SourceSearchResult(
                "https://exemplo.com/agenda",
                "Como lotar agenda de manicure",
                "Resumo público da fonte",
                "exemplo.com",
                1,
                null,
                null,
                false,
                false));
        SourceSearcherOutput output = output();
        when(searchProvider.search(pending.queryText(), 20)).thenReturn(searchResults);
        when(searchProvider.providerCode()).thenReturn("DUCKDUCKGO_HTML");
        when(backendClient.completeStageExecution(eq(pending), eq("DUCKDUCKGO_HTML"), any())).thenReturn(output);

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
        verify(backendClient).completeStageExecution(eq(pending), eq("DUCKDUCKGO_HTML"), any());
    }

    /** Deve ordenar fontes priorizando execução prática antes de páginas de agenda ou sistema. */
    @Test
    void shouldPrioritizePracticalExecutionSourcesBeforeSoftwareAgendaPages() {
        PublicSourceSearchProvider searchProvider = mock(PublicSourceSearchProvider.class);
        SourceSearcherBackendClient backendClient = mock(SourceSearcherBackendClient.class);
        SourceSearcherProcessor processor =
                new SourceSearcherProcessor(searchProvider, backendClient, new SourceIntentClassifier());
        SourceSearcherPending pending = pending();
        SourceSearchResult softwarePage = new SourceSearchResult(
                "https://agenda.example.com/salao",
                "Sistema com agenda online e app para salão",
                "Automação e software para vender mais com reservas online.",
                "agenda.example.com",
                1,
                null,
                null,
                false,
                false);
        SourceSearchResult practicalPage = new SourceSearchResult(
                "https://ocupacoes.example.com.br/cbo-manicure-2026",
                "CBO manicure pedicure: rotina executada",
                "Guia profissional com procedimentos de atendimento cliente, higiene e esterilização no dia a dia.",
                "ocupacoes.example.com.br",
                2,
                null,
                null,
                false,
                false);
        SourceSearcherOutput output = output();
        when(searchProvider.search(pending.queryText(), 20)).thenReturn(List.of(softwarePage, practicalPage));
        when(searchProvider.providerCode()).thenReturn("DUCKDUCKGO_HTML");
        when(backendClient.completeStageExecution(eq(pending), eq("DUCKDUCKGO_HTML"), any())).thenReturn(output);

        processor.process(new StageContext<>(
                new StageExecution<>("job-1", pending, Map.of()),
                pending,
                (artifact, content) -> artifact,
                Map.of()));

        ArgumentCaptor<List<SourceSearchResult>> captor = ArgumentCaptor.captor();
        verify(backendClient).completeStageExecution(eq(pending), eq("DUCKDUCKGO_HTML"), captor.capture());
        assertThat(captor.getValue())
                .extracting(SourceSearchResult::sourceUrl)
                .containsExactly("https://ocupacoes.example.com.br/cbo-manicure-2026", "https://agenda.example.com/salao");
        assertThat(captor.getValue().get(0).commercialPageRisk()).isFalse();
        assertThat(captor.getValue().get(1).commercialPageRisk()).isTrue();
    }

    /** Deve selecionar evidência humana de rotina real antes de fonte genérica quando os riscos são equivalentes. */
    @Test
    void shouldPrioritizeRealWorkEvidenceWhenRisksAreEquivalent() {
        PublicSourceSearchProvider searchProvider = mock(PublicSourceSearchProvider.class);
        SourceSearcherBackendClient backendClient = mock(SourceSearcherBackendClient.class);
        SourceSearcherProcessor processor =
                new SourceSearcherProcessor(searchProvider, backendClient, new SourceIntentClassifier());
        SourceSearcherPending pending = pending();
        SourceSearchResult genericRoutine = new SourceSearchResult(
                "https://blog.example.com.br/rotina-salao-2026",
                "Rotina de salão no Brasil",
                "Texto sobre tarefas, atendimento e organização de materiais.",
                "blog.example.com.br",
                1,
                null,
                null,
                false,
                false);
        SourceSearchResult realWorkEvidence = new SourceSearchResult(
                "https://relatos.example.com.br/manicure-rotina-2026",
                "Minha rotina manual de manicure autônoma no atendimento real",
                "Relato com indicação, fidelização, recorrência e medo de cliente desmarcar.",
                "relatos.example.com.br",
                2,
                null,
                null,
                false,
                false);
        SourceSearcherOutput output = output();
        when(searchProvider.search(pending.queryText(), 20)).thenReturn(List.of(genericRoutine, realWorkEvidence));
        when(searchProvider.providerCode()).thenReturn("DUCKDUCKGO_HTML");
        when(backendClient.completeStageExecution(eq(pending), eq("DUCKDUCKGO_HTML"), any())).thenReturn(output);

        processor.process(new StageContext<>(
                new StageExecution<>("job-1", pending, Map.of()),
                pending,
                (artifact, content) -> artifact,
                Map.of()));

        ArgumentCaptor<List<SourceSearchResult>> captor = ArgumentCaptor.captor();
        verify(backendClient).completeStageExecution(eq(pending), eq("DUCKDUCKGO_HTML"), captor.capture());
        assertThat(captor.getValue())
                .extracting(SourceSearchResult::sourceUrl)
                .startsWith("https://relatos.example.com.br/manicure-rotina-2026");
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
                        90,
                        null,
                        "ROUTINE_REPORT",
                        90,
                        false,
                        false,
                        Instant.parse("2026-06-04T00:01:00Z"),
                        Instant.parse("2026-06-04T00:01:00Z"))));
    }
}
