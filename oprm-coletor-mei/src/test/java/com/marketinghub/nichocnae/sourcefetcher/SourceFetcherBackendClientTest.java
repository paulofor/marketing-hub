package com.marketinghub.nichocnae.sourcefetcher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Responsabilidade: validar mapeamentos de contrato entre o coletor OPRM e o backend da etapa quatro. */
class SourceFetcherBackendClientTest {

    /** Deve montar o payload de conclusão preservando somente campos oficiais do snapshot curto. */
    @Test
    void shouldBuildCompletionRequestFromFetchedSnapshot() {
        SourceFetcherBackendClient client = new SourceFetcherBackendClient(null, null);
        FetchedSourceSnapshot snapshot = new FetchedSourceSnapshot(
                "https://exemplo.com/agenda",
                "exemplo.com",
                "Como lotar agenda de manicure",
                "PUBLIC_CONTENT",
                "Resumo público",
                "Trecho curto permitido para extração de sinais.",
                "COMPLETED",
                200,
                "SHORT_EXCERPT_ONLY",
                "PUBLIC_SNIPPET",
                95);

        SourceFetcherCompletionRequest request = client.toCompletionRequest(snapshot);

        assertThat(request.sourceUrl()).isEqualTo("https://exemplo.com/agenda");
        assertThat(request.sourceDomain()).isEqualTo("exemplo.com");
        assertThat(request.storagePolicy()).isEqualTo("SHORT_EXCERPT_ONLY");
        assertThat(request.shortExcerpt()).doesNotContain("<html");
        assertThat(request.relevanceScore()).isEqualTo(95);
    }
}
