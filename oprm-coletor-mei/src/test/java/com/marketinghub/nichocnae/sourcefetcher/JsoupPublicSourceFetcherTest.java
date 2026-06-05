package com.marketinghub.nichocnae.sourcefetcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar regras locais de normalização de snapshot curto da etapa quatro. */
class JsoupPublicSourceFetcherTest {

    /** Garante que a etapa quatro limita snippets e trechos para evitar persistência de HTML completo. */
    @Test
    void shouldKeepOnlyContractedShortFieldsWhenBuildingSnapshotRequest() {
        SourceFetcherBackendClient client = new SourceFetcherBackendClient(null, null);
        String longExcerpt = "dor ".repeat(400);
        FetchedSourceSnapshot snapshot = new FetchedSourceSnapshot(
                "https://exemplo.com/agenda",
                "exemplo.com",
                "Agenda cheia",
                "PUBLIC_CONTENT",
                "ROUTINE_REPORT",
                90,
                false,
                false,
                "snippet",
                longExcerpt.substring(0, 1200),
                "COMPLETED",
                200,
                "SHORT_EXCERPT_ONLY",
                "PUBLIC_SNIPPET",
                90);

        SourceFetcherCompletionRequest request = client.toCompletionRequest(snapshot);

        assertThat(request.shortExcerpt()).hasSizeLessThanOrEqualTo(1200);
        assertThat(request.shortExcerpt()).doesNotContain("<!DOCTYPE").doesNotContain("<body");
    }

    /** Cria uma pendência com campos de rastreabilidade para futuras extensões de teste. */
    private SourceFetcherPending pending() {
        return new SourceFetcherPending(
                4001L,
                1001L,
                2001L,
                "https://exemplo.com/agenda",
                "Agenda cheia",
                "snippet",
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
}
