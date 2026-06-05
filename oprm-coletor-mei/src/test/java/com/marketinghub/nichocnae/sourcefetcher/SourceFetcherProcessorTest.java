package com.marketinghub.nichocnae.sourcefetcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.nichocnae.pipeline.StageContext;
import com.marketinghub.nichocnae.pipeline.StageExecution;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a orquestração da etapa quatro entre coletor público, backend e StageProcessor. */
class SourceFetcherProcessorTest {

    /** Deve coletar a fonte pública e concluir a etapa quatro no backend com métricas estruturadas. */
    @Test
    void shouldFetchSourceAndCompleteSourceFetcherOutput() {
        PublicSourceFetcher sourceFetcher = mock(PublicSourceFetcher.class);
        SourceFetcherBackendClient backendClient = mock(SourceFetcherBackendClient.class);
        SourceFetcherProcessor processor = new SourceFetcherProcessor(sourceFetcher, backendClient);
        SourceFetcherPending pending = pending();
        FetchedSourceSnapshot snapshot = snapshot();
        SourceFetcherOutput output = output();
        when(sourceFetcher.fetch(pending)).thenReturn(snapshot);
        when(backendClient.completeStageExecution(pending, snapshot)).thenReturn(output);

        var result = processor.process(new StageContext<>(
                new StageExecution<>("job-1", pending, Map.of()),
                pending,
                (artifact, content) -> artifact,
                Map.of()));

        assertThat(result.output()).isEqualTo(output);
        assertThat(result.metrics())
                .containsEntry("sourceCandidateId", 4001L)
                .containsEntry("researchCycleId", 1001L)
                .containsEntry("sourceIntent", "ROUTINE_REPORT")
                .containsEntry("routineEvidenceScore", 90)
                .containsEntry("commercialPageRisk", false)
                .containsEntry("solutionLanguageRisk", false)
                .containsEntry("httpStatus", 200)
                .containsEntry("cycleTotalSourceSnapshots", 1);
        verify(backendClient).completeStageExecution(pending, snapshot);
    }

    /** Cria uma pendência mínima para a etapa quatro. */
    private SourceFetcherPending pending() {
        return new SourceFetcherPending(
                4001L,
                1001L,
                2001L,
                "https://exemplo.com/agenda",
                "Como lotar agenda de manicure",
                "Resumo público da fonte",
                "exemplo.com",
                "GENERAL_WEB",
                "ROUTINE_REPORT",
                90,
                false,
                false,
                "DUCKDUCKGO_HTML",
                1,
                "FOUND",
                Instant.parse("2026-06-04T00:00:00Z"),
                Instant.parse("2026-06-04T00:00:00Z"));
    }

    /** Cria um snapshot curto suficiente para validar o processor. */
    private FetchedSourceSnapshot snapshot() {
        return new FetchedSourceSnapshot(
                "https://exemplo.com/agenda",
                "exemplo.com",
                "Como lotar agenda de manicure",
                "PUBLIC_CONTENT",
                "ROUTINE_REPORT",
                90,
                false,
                false,
                "Resumo público da fonte",
                "Trecho curto permitido para extração de sinais.",
                "COMPLETED",
                200,
                "SHORT_EXCERPT_ONLY",
                "PUBLIC_SNIPPET",
                95);
    }

    /** Cria uma saída mínima suficiente para validar o processor. */
    private SourceFetcherOutput output() {
        return new SourceFetcherOutput(
                4001L,
                1001L,
                true,
                95,
                1,
                new SourceSnapshotResponse(
                        5001L,
                        1001L,
                        4001L,
                        "https://exemplo.com/agenda",
                        "exemplo.com",
                        "Como lotar agenda de manicure",
                        "PUBLIC_CONTENT",
                        "ROUTINE_REPORT",
                        90,
                        false,
                        false,
                        "Resumo público da fonte",
                        "Trecho curto permitido para extração de sinais.",
                        Instant.parse("2026-06-04T00:01:00Z"),
                        "COMPLETED",
                        200,
                        "SHORT_EXCERPT_ONLY",
                        "PUBLIC_SNIPPET",
                        null,
                        Instant.parse("2026-06-04T00:01:00Z")));
    }
}
