package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

/**
 * Valida a normalização de fontes públicas usadas pelo aquecimento de mercado MOIS.
 */
class DuckDuckGoPublicWebSearchClientTest {
    /**
     * Garante que o fallback RSS entregue fontes rastreáveis quando o buscador primário não traz resultados HTML.
     */
    @Test
    void shouldParseBingRssFallbackResults() {
        DuckDuckGoPublicWebSearchClient client = new DuckDuckGoPublicWebSearchClient(properties());
        String rawPayload = """
                <?xml version=\"1.0\" encoding=\"utf-8\" ?>
                <rss version=\"2.0\">
                    <channel>
                        <item>
                            <title>Sono Profundo: Guia natural contra insônia</title>
                            <link>https://hotmart.com/pt-br/marketplace/produtos/sono-profundo</link>
                            <description>Produto público com promessa contra insônia.</description>
                        </item>
                        <item>
                            <title>Review Sono Profundo vale a pena</title>
                            <link>https://blog.example.com/review-sono-profundo</link>
                            <description>Review com comparação e depoimento.</description>
                        </item>
                    </channel>
                </rss>
                """;

        List<PublicSearchResult> results = client.parseBingRssResults(rawPayload, 1);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().title()).isEqualTo("Sono Profundo: Guia natural contra insônia");
        assertThat(results.getFirst().url()).isEqualTo("https://hotmart.com/pt-br/marketplace/produtos/sono-profundo");
        assertThat(results.getFirst().snippet()).contains("promessa contra insônia");
    }

    /**
     * Garante que o parser do DuckDuckGo ignore páginas de bloqueio sem criar fonte falsa no dossiê.
     */
    @Test
    void shouldReturnNoDuckDuckGoResultsWhenChallengePageHasNoResultItems() {
        DuckDuckGoPublicWebSearchClient client = new DuckDuckGoPublicWebSearchClient(properties());
        String challengeHtml = """
                <html>
                    <body>
                        <div class=\"anomaly-modal__modal\">Unfortunately, bots use DuckDuckGo too.</div>
                    </body>
                </html>
                """;

        List<PublicSearchResult> results = client.parseDuckDuckGoResults(Jsoup.parse(challengeHtml), 6);

        assertThat(results).isEmpty();
    }

    /**
     * Monta propriedades mínimas do worker sem acionar rede nos testes de parser.
     */
    private WorkerProperties properties() {
        return new WorkerProperties(
                "http://localhost:8080",
                "workspace-001",
                "HOTMART",
                "HOTMART",
                "HOTMART",
                "HOTMART",
                15000,
                30000,
                20000,
                1,
                false,
                true,
                60000,
                "worker-test",
                "https://duckduckgo.com/html/",
                6,
                "JUnit",
                30000);
    }
}
