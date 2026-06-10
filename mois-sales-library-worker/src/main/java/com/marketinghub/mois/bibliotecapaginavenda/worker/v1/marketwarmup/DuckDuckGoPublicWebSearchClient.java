package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Coleta resultados na página pública HTML do DuckDuckGo ou endpoint compatível configurado.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DuckDuckGoPublicWebSearchClient implements PublicWebSearchClient {
    private final WorkerProperties properties;

    /**
     * Busca resultados públicos e registra o HTML bruto recebido antes de qualquer normalização.
     */
    @Override
    public List<PublicSearchResult> search(String query, int limit) throws IOException {
        Document document = Jsoup.connect(properties.marketWarmupSearchEndpoint())
                .data("q", query)
                .userAgent(properties.marketWarmupSearchUserAgent())
                .timeout(properties.requestTimeoutMs())
                .ignoreContentType(true)
                .get();
        String rawPayload = document.outerHtml();
        log.info("MOIS market-warmup raw public search payload received. query={}, endpoint={}, payload={}",
                query, properties.marketWarmupSearchEndpoint(), rawPayload);
        List<PublicSearchResult> results = new ArrayList<>();
        for (Element result : document.select(".result")) {
            if (results.size() >= limit) {
                break;
            }
            Element titleElement = result.selectFirst(".result__a");
            if (titleElement == null) {
                continue;
            }
            String title = titleElement.text();
            String url = normalizeDuckDuckGoUrl(titleElement.attr("href"));
            String snippet = result.select(".result__snippet").text();
            if (!url.isBlank()) {
                results.add(new PublicSearchResult(title, url, snippet, result.outerHtml()));
            }
        }
        log.info("MOIS market-warmup public search normalized. query={}, resultCount={}", query, results.size());
        return results;
    }

    /**
     * Remove redirecionamento interno do buscador quando o link público estiver no parâmetro uddg.
     */
    private String normalizeDuckDuckGoUrl(String href) {
        if (href == null || href.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(href);
            String query = uri.getRawQuery();
            if (query != null) {
                for (String part : query.split("&")) {
                    if (part.startsWith("uddg=")) {
                        return URLDecoder.decode(part.substring(5), StandardCharsets.UTF_8);
                    }
                }
            }
            return href;
        } catch (IllegalArgumentException ex) {
            log.warn("MOIS market-warmup could not normalize search url. href={}, errorClass={}, errorMessage={}",
                    href, ex.getClass().getName(), ex.getMessage(), ex);
            return href;
        }
    }
}
