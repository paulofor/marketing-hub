package com.marketinghub.nichocnae.sourcesearcher;

import java.net.URI;
import java.text.Normalizer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Consulta a busca HTML pública do DuckDuckGo para executar a etapa três no coletor OPRM. */
@Component
public class DuckDuckGoHtmlSourceSearchProvider implements PublicSourceSearchProvider {
    private static final Logger log = LoggerFactory.getLogger(DuckDuckGoHtmlSourceSearchProvider.class);
    private static final String PROVIDER_CODE = "DUCKDUCKGO_HTML";
    private static final String SEARCH_URL = "https://html.duckduckgo.com/html/";
    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; MarketingHubOPRM/1.0; +https://oportunidadebrasil.shop)";
    private static final int TIMEOUT_MILLIS = (int) Duration.ofSeconds(15).toMillis();

    /** Executa a busca pública HTML e retorna no máximo a quantidade solicitada de resultados úteis. */
    @Override
    public List<SourceSearchResult> search(String queryText, int maxResults) {
        try {
            Document document = Jsoup.connect(SEARCH_URL)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .data("q", buildBrazilianMarketQuery(queryText))
                    .data("kl", "br-pt")
                    .data("df", "")
                    .get();
            return parseResults(document, maxResults);
        } catch (RuntimeException ex) {
            log.error("Erro runtime ao buscar fontes no DuckDuckGo HTML (queryText={})", queryText, ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Erro de integração ao buscar fontes no DuckDuckGo HTML (queryText={}, localizedQuery={})", queryText, buildBrazilianMarketQuery(queryText), ex);
            throw new IllegalStateException("Falha ao consultar DuckDuckGo HTML: " + ex.getMessage(), ex);
        }
    }

    /** Adiciona marcador Brasil quando a query ainda não deixa explícito que o mercado pesquisado é brasileiro. */
    String buildBrazilianMarketQuery(String queryText) {
        if (!StringUtils.hasText(queryText)) {
            return "Brasil";
        }
        String normalized = Normalizer.normalize(queryText, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        if (normalized.contains("brasil")
                || normalized.contains("brasileir")
                || normalized.contains("portugues do brasil")
                || normalized.contains(".br")
                || normalized.contains("site:br")) {
            return queryText.trim();
        }
        return queryText.trim() + " Brasil";
    }

    /** Informa o código operacional do provedor DuckDuckGo HTML. */
    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    /** Extrai resultados orgânicos do documento HTML retornado pelo provedor. */
    private List<SourceSearchResult> parseResults(Document document, int maxResults) {
        List<SourceSearchResult> results = new ArrayList<>();
        for (Element link : document.select("a.result__a")) {
            if (results.size() >= maxResults) {
                break;
            }
            SourceSearchResult result = toSearchResult(link, results.size() + 1);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    /** Converte um link orgânico em resultado normalizado para o contrato da etapa três. */
    private SourceSearchResult toSearchResult(Element link, int position) {
        String sourceUrl = normalizeDuckDuckGoUrl(link.attr("href"));
        String title = link.text();
        String domain = extractDomain(sourceUrl);
        if (!StringUtils.hasText(sourceUrl) || !StringUtils.hasText(title) || !StringUtils.hasText(domain)) {
            return null;
        }
        Element resultContainer = link.closest(".result");
        String snippet = resultContainer == null ? null : textOrNull(resultContainer.selectFirst(".result__snippet"));
        return new SourceSearchResult(sourceUrl, title.trim(), snippet, domain, position, null, null, false, false);
    }

    /** Normaliza URLs diretas e redirecionamentos do DuckDuckGo para preservar a URL real da fonte. */
    private String normalizeDuckDuckGoUrl(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            return null;
        }
        String absoluteUrl = rawUrl.trim().startsWith("//") ? "https:" + rawUrl.trim() : rawUrl.trim();
        try {
            URI uri = URI.create(absoluteUrl);
            String query = uri.getRawQuery();
            if (query != null) {
                for (String parameter : query.split("&")) {
                    int separator = parameter.indexOf('=');
                    if (separator > 0 && "uddg".equals(parameter.substring(0, separator))) {
                        return URLDecoder.decode(parameter.substring(separator + 1), StandardCharsets.UTF_8);
                    }
                }
            }
            return absoluteUrl;
        } catch (RuntimeException ex) {
            log.warn("URL de resultado ignorada por formato inválido no DuckDuckGo HTML (rawUrl={})", rawUrl, ex);
            return null;
        }
    }

    /** Extrai o domínio da URL final para rastreabilidade e agrupamento no backend. */
    private String extractDomain(String sourceUrl) {
        try {
            URI uri = URI.create(sourceUrl);
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return null;
            }
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (RuntimeException ex) {
            log.warn("Domínio de fonte ignorado por URL inválida (sourceUrl={})", sourceUrl, ex);
            return null;
        }
    }

    /** Retorna texto útil do elemento HTML ou nulo quando o snippet não existe. */
    private String textOrNull(Element element) {
        if (element == null || !StringUtils.hasText(element.text())) {
            return null;
        }
        return element.text().trim();
    }
}
