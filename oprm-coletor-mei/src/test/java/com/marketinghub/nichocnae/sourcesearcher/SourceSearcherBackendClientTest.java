package com.marketinghub.nichocnae.sourcesearcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar mapeamentos de contrato entre o coletor OPRM e o backend da etapa três. */
class SourceSearcherBackendClientTest {

    /** Deve montar o payload de conclusão preservando somente campos contratuais de fonte candidata. */
    @Test
    void shouldBuildCompletionRequestFromSearchResults() {
        SourceSearcherBackendClient client = new SourceSearcherBackendClient(null, null);
        SourceSearcherPending pending = new SourceSearcherPending(
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

        SourceSearcherCompletionRequest request = client.toCompletionRequest(
                pending,
                "DUCKDUCKGO_HTML",
                List.of(new SourceSearchResult(
                        "https://exemplo.com/agenda",
                        "Como lotar agenda de manicure",
                        "Resumo público da fonte",
                        "exemplo.com",
                        1)));

        assertThat(request.searchProvider()).isEqualTo("DUCKDUCKGO_HTML");
        assertThat(request.results()).hasSize(1);
        assertThat(request.results().getFirst().sourceGroup()).isEqualTo("GENERAL_WEB");
        assertThat(request.results().getFirst().status()).isEqualTo("FOUND");
    }
}
