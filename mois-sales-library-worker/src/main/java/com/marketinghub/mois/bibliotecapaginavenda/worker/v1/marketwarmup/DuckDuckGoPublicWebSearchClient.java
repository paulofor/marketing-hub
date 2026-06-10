package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.marketwarmup;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config.WorkerProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;

/**
 * Coleta resultados públicos rastreáveis para aquecimento usando buscador primário e fallback RSS.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DuckDuckGoPublicWebSearchClient implements PublicWebSearchClient {
    private static final String BING_RSS_ENDPOINT = "https://www.bing.com/search";

    private final WorkerProperties properties;

    /**
     * Busca resultados públicos, registra o payload bruto e usa fallback rastreável quando o DuckDuckGo bloqueia robôs.
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
        List<PublicSearchResult> results = parseDuckDuckGoResults(document, limit);
        if (results.isEmpty()) {
            log.warn("MOIS market-warmup primary public search returned no traceable results. query={}, endpoint={}, blockedByChallenge={}",
                    query, properties.marketWarmupSearchEndpoint(), isDuckDuckGoChallenge(rawPayload));
            results = searchBingRss(query, limit);
        }
        log.info("MOIS market-warmup public search normalized. query={}, resultCount={}", query, results.size());
        return results;
    }

    /**
     * Coleta resultados RSS do Bing como fallback público quando o HTML primário não oferece fontes rastreáveis.
     */
    private List<PublicSearchResult> searchBingRss(String query, int limit) throws IOException {
        String endpoint = BING_RSS_ENDPOINT + "?format=rss&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        String rawPayload = Jsoup.connect(endpoint)
                .userAgent(properties.marketWarmupSearchUserAgent())
                .timeout(properties.requestTimeoutMs())
                .ignoreContentType(true)
                .execute()
                .body();
        log.info("MOIS market-warmup raw public search payload received. query={}, endpoint={}, payload={}",
                query, BING_RSS_ENDPOINT, rawPayload);
        List<PublicSearchResult> results = parseBingRssResults(rawPayload, limit);
        log.info("MOIS market-warmup fallback public search normalized. query={}, endpoint={}, resultCount={}",
                query, BING_RSS_ENDPOINT, results.size());
        return results;
    }

    /**
     * Normaliza o HTML do DuckDuckGo em resultados rastreáveis para o dossiê.
     */
    List<PublicSearchResult> parseDuckDuckGoResults(Document document, int limit) {
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
        return results;
    }

    /**
     * Normaliza o RSS público do fallback em resultados rastreáveis para o dossiê.
     */
    List<PublicSearchResult> parseBingRssResults(String rawPayload, int limit) {
        Document document = Jsoup.parse(rawPayload, "", Parser.xmlParser());
        List<PublicSearchResult> results = new ArrayList<>();
        for (Element item : document.select("item")) {
            if (results.size() >= limit) {
                break;
            }
            String title = item.selectFirst("title") == null ? "" : item.selectFirst("title").text();
            String url = item.selectFirst("link") == null ? "" : item.selectFirst("link").text();
            String snippet = item.selectFirst("description") == null ? "" : item.selectFirst("description").text();
            if (!url.isBlank()) {
                results.add(new PublicSearchResult(title, url, snippet, item.outerHtml()));
            }
        }
        return results;
    }

    /**
     * Detecta bloqueio antirobô do DuckDuckGo para explicar fallback operacional nos logs.
     */
    private boolean isDuckDuckGoChallenge(String rawPayload) {
        String normalized = rawPayload == null ? "" : rawPayload.toLowerCase();
        return normalized.contains("anomaly-modal") || normalized.contains("unfortunately, bots use duckduckgo too");
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
