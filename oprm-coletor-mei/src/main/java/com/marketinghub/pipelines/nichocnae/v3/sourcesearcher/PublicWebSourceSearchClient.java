package com.marketinghub.pipelines.nichocnae.v3.sourcesearcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Coleta fontes públicas rastreáveis para a etapa source-searcher com Google opcional e fallback aberto. */
@Component
public class PublicWebSourceSearchClient implements SourceSearchClient {
    private static final Logger log = LoggerFactory.getLogger(PublicWebSourceSearchClient.class);
    private static final String DUCK_DUCK_GO_ENDPOINT = "https://duckduckgo.com/html/";
    private static final String BING_RSS_ENDPOINT = "https://www.bing.com/search";

    private final ObjectMapper objectMapper;
    private final boolean googleEnabled;
    private final String googleApiKey;
    private final String googleSearchEngineId;
    private final String googleBaseUrl;
    private final String googleDateRestrict;
    private final String googleCountryRestrict;
    private final String googleLanguageRestrict;
    private final String userAgent;
    private final int timeoutMs;

    /** Inicializa o cliente com configuração operacional da busca pública. */
    public PublicWebSourceSearchClient(
            ObjectMapper objectMapper,
            @Value("${oprm.nichocnae.source-searcher.google.enabled:false}") boolean googleEnabled,
            @Value("${oprm.nichocnae.source-searcher.google.api-key:}") String googleApiKey,
            @Value("${oprm.nichocnae.source-searcher.google.search-engine-id:}") String googleSearchEngineId,
            @Value("${oprm.nichocnae.source-searcher.google.base-url:https://www.googleapis.com/customsearch/v1}") String googleBaseUrl,
            @Value("${oprm.nichocnae.source-searcher.google.date-restrict:m24}") String googleDateRestrict,
            @Value("${oprm.nichocnae.source-searcher.google.country-restrict:countryBR}") String googleCountryRestrict,
            @Value("${oprm.nichocnae.source-searcher.google.language-restrict:lang_pt}") String googleLanguageRestrict,
            @Value("${oprm.nichocnae.source-searcher.user-agent:Mozilla/5.0 MarketingHubBot/1.0}") String userAgent,
            @Value("${oprm.nichocnae.source-searcher.timeout-ms:10000}") int timeoutMs) {
        this.objectMapper = objectMapper;
        this.googleEnabled = googleEnabled;
        this.googleApiKey = googleApiKey;
        this.googleSearchEngineId = googleSearchEngineId;
        this.googleBaseUrl = googleBaseUrl;
        this.googleDateRestrict = googleDateRestrict;
        this.googleCountryRestrict = googleCountryRestrict;
        this.googleLanguageRestrict = googleLanguageRestrict;
        this.userAgent = userAgent;
        this.timeoutMs = timeoutMs;
    }

    /** Busca fontes priorizando Google configurado e usando DuckDuckGo/Bing como fallback rastreável. */
    @Override
    public List<SourceSearchResult> search(String query, int limit) {
        List<SourceSearchResult> results = searchGoogle(query, limit);
        if (!results.isEmpty()) {
            return results;
        }
        results = searchDuckDuckGo(query, limit);
        if (!results.isEmpty()) {
            return results;
        }
        return searchBingRss(query, limit);
    }

    /** Executa busca no Google Custom Search quando a configuração estiver completa. */
    private List<SourceSearchResult> searchGoogle(String query, int limit) {
        if (!googleEnabled || googleApiKey.isBlank() || googleSearchEngineId.isBlank()) {
            return List.of();
        }
        try {
            String endpoint = googleBaseUrl + "?key=" + encode(googleApiKey)
                    + "&cx=" + encode(googleSearchEngineId)
                    + "&q=" + encode(query)
                    + "&num=" + Math.min(10, Math.max(1, limit))
                    + "&dateRestrict=" + encode(googleDateRestrict)
                    + "&cr=" + encode(googleCountryRestrict)
                    + "&lr=" + encode(googleLanguageRestrict);
            String rawPayload = Jsoup.connect(endpoint)
                    .userAgent(userAgent)
                    .timeout(timeoutMs)
                    .ignoreContentType(true)
                    .execute()
                    .body();
            log.info("NichoCNAE v3 source-searcher raw public search payload received. query={}, provider={}, payload={}",
                    query, "GOOGLE_CUSTOM_SEARCH_RECENT", rawPayload);
            return parseGoogleResults(rawPayload, limit);
        } catch (IOException | RuntimeException ex) {
            log.warn("Falha ao buscar fontes no Google Custom Search do NichoCNAE v3 (query={}, limit={})", query, limit, ex);
            return List.of();
        }
    }

    /** Executa busca pública no DuckDuckGo HTML. */
    private List<SourceSearchResult> searchDuckDuckGo(String query, int limit) {
        try {
            Document document = Jsoup.connect(DUCK_DUCK_GO_ENDPOINT)
                    .data("q", query)
                    .userAgent(userAgent)
                    .timeout(timeoutMs)
                    .ignoreContentType(true)
                    .get();
            String rawPayload = document.outerHtml();
            log.info("NichoCNAE v3 source-searcher raw public search payload received. query={}, provider={}, payload={}",
                    query, "DUCKDUCKGO_HTML", rawPayload);
            return parseDuckDuckGoResults(document, limit);
        } catch (IOException | RuntimeException ex) {
            log.warn("Falha ao buscar fontes no DuckDuckGo do NichoCNAE v3 (query={}, limit={})", query, limit, ex);
            return List.of();
        }
    }

    /** Executa fallback RSS do Bing quando os provedores anteriores não entregam fonte rastreável. */
    private List<SourceSearchResult> searchBingRss(String query, int limit) {
        try {
            String endpoint = BING_RSS_ENDPOINT + "?format=rss&q=" + encode(query);
            String rawPayload = Jsoup.connect(endpoint)
                    .userAgent(userAgent)
                    .timeout(timeoutMs)
                    .ignoreContentType(true)
                    .execute()
                    .body();
            log.info("NichoCNAE v3 source-searcher raw public search payload received. query={}, provider={}, payload={}",
                    query, "BING_RSS", rawPayload);
            return parseBingRssResults(rawPayload, limit);
        } catch (IOException | RuntimeException ex) {
            log.warn("Falha ao buscar fontes no Bing RSS do NichoCNAE v3 (query={}, limit={})", query, limit, ex);
            return List.of();
        }
    }

    /** Normaliza o JSON do Google Custom Search em fontes rastreáveis. */
    List<SourceSearchResult> parseGoogleResults(String rawPayload, int limit) throws IOException {
        JsonNode root = objectMapper.readTree(rawPayload);
        JsonNode items = root.path("items");
        List<SourceSearchResult> results = new ArrayList<>();
        if (!items.isArray()) {
            return results;
        }
        for (JsonNode item : items) {
            if (results.size() >= limit) {
                break;
            }
            String url = item.path("link").asText("");
            if (!url.isBlank()) {
                results.add(new SourceSearchResult(
                        item.path("title").asText(""),
                        url,
                        item.path("snippet").asText(""),
                        "GOOGLE_CUSTOM_SEARCH_RECENT",
                        item.toString()));
            }
        }
        return results;
    }

    /** Normaliza o HTML do DuckDuckGo em fontes rastreáveis. */
    List<SourceSearchResult> parseDuckDuckGoResults(Document document, int limit) {
        List<SourceSearchResult> results = new ArrayList<>();
        for (Element result : document.select(".result")) {
            if (results.size() >= limit) {
                break;
            }
            Element titleElement = result.selectFirst(".result__a");
            if (titleElement == null) {
                continue;
            }
            String url = normalizeDuckDuckGoUrl(titleElement.attr("href"));
            if (!url.isBlank()) {
                results.add(new SourceSearchResult(
                        titleElement.text(),
                        url,
                        result.select(".result__snippet").text(),
                        "DUCKDUCKGO_HTML",
                        result.outerHtml()));
            }
        }
        return results;
    }

    /** Normaliza o RSS público do Bing em fontes rastreáveis. */
    List<SourceSearchResult> parseBingRssResults(String rawPayload, int limit) {
        Document document = Jsoup.parse(rawPayload, "", Parser.xmlParser());
        List<SourceSearchResult> results = new ArrayList<>();
        for (Element item : document.select("item")) {
            if (results.size() >= limit) {
                break;
            }
            String url = item.selectFirst("link") == null ? "" : item.selectFirst("link").text();
            if (!url.isBlank()) {
                results.add(new SourceSearchResult(
                        item.selectFirst("title") == null ? "" : item.selectFirst("title").text(),
                        url,
                        item.selectFirst("description") == null ? "" : item.selectFirst("description").text(),
                        "BING_RSS",
                        item.outerHtml()));
            }
        }
        return results;
    }

    /** Remove redirecionamento interno do DuckDuckGo quando existir URL pública no parâmetro uddg. */
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
            log.warn("Não foi possível normalizar URL do source-searcher NichoCNAE v3 (href={})", href, ex);
            return href;
        }
    }

    /** Codifica texto para uso seguro em query string. */
    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
