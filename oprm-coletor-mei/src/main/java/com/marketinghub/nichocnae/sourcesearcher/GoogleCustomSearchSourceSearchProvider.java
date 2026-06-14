package com.marketinghub.nichocnae.sourcesearcher;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/** Consulta Google Custom Search para priorizar fontes brasileiras recentes na etapa três do NichoCNAE. */
@Component
public class GoogleCustomSearchSourceSearchProvider implements PublicSourceSearchProvider {
    private static final Logger log = LoggerFactory.getLogger(GoogleCustomSearchSourceSearchProvider.class);
    private static final String PROVIDER_CODE = "GOOGLE_CUSTOM_SEARCH_RECENT";
    private static final int GOOGLE_MAX_RESULTS_PER_REQUEST = 10;

    private final GoogleCustomSearchProperties properties;
    private final RestClient restClient;

    /** Inicializa o provedor com configurações de Google Custom Search e cliente HTTP compartilhado. */
    public GoogleCustomSearchSourceSearchProvider(GoogleCustomSearchProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    /** Executa busca recente no Google Custom Search quando o provedor estiver configurado. */
    @Override
    public List<SourceSearchResult> search(String queryText, int maxResults) {
        if (!properties.configured()) {
            return List.of();
        }
        String localizedQuery = buildRecentBrazilianQuery(queryText);
        try {
            GoogleSearchResponse response = restClient.get()
                    .uri(buildSearchUri(localizedQuery, maxResults))
                    .retrieve()
                    .body(GoogleSearchResponse.class);
            if (response == null || response.items() == null) {
                return List.of();
            }
            return response.items().stream()
                    .limit(Math.max(0, maxResults))
                    .map(this::toSearchResult)
                    .filter(result -> result != null)
                    .toList();
        } catch (RestClientException ex) {
            log.error("Erro de integração ao buscar fontes recentes no Google Custom Search (queryText={}, localizedQuery={})", queryText, localizedQuery, ex);
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Erro runtime ao buscar fontes recentes no Google Custom Search (queryText={}, localizedQuery={})", queryText, localizedQuery, ex);
            throw ex;
        }
    }

    /** Informa o código operacional do provedor Google Custom Search recente. */
    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    /** Indica se o provedor está habilitado e possui credenciais para execução. */
    boolean configured() {
        return properties.configured();
    }

    /** Monta query Brasil-first com termos explícitos de recência para reforçar a intenção comercial atual. */
    String buildRecentBrazilianQuery(String queryText) {
        String baseQuery = StringUtils.hasText(queryText) ? queryText.trim() : "rotina profissional autônomo Brasil";
        String normalized = baseQuery.toLowerCase();
        String withBrazil = normalized.contains("brasil") || normalized.contains("brasileir") || normalized.contains("site:.br")
                ? baseQuery
                : baseQuery + " Brasil";
        return withBrazil + " 2025 OR 2026";
    }

    /** Monta a URI da API oficial do Google com restrição de data, país e idioma. */
    private String buildSearchUri(String localizedQuery, int maxResults) {
        int boundedResults = Math.max(1, Math.min(maxResults, GOOGLE_MAX_RESULTS_PER_REQUEST));
        return UriComponentsBuilder.fromUriString(properties.effectiveBaseUrl())
                .queryParam("key", properties.apiKey())
                .queryParam("cx", properties.searchEngineId())
                .queryParam("q", localizedQuery)
                .queryParam("num", boundedResults)
                .queryParam("dateRestrict", properties.effectiveDateRestrict())
                .queryParam("cr", properties.effectiveCountryRestrict())
                .queryParam("lr", properties.effectiveLanguageRestrict())
                .queryParam("gl", "br")
                .build()
                .toUriString();
    }

    /** Converte um item orgânico do Google em resultado normalizado para o contrato da etapa três. */
    private SourceSearchResult toSearchResult(GoogleSearchItem item) {
        if (item == null || !StringUtils.hasText(item.link()) || !StringUtils.hasText(item.title())) {
            return null;
        }
        String domain = extractDomain(item.link());
        if (!StringUtils.hasText(domain)) {
            return null;
        }
        return new SourceSearchResult(
                item.link().trim(),
                item.title().trim(),
                StringUtils.hasText(item.snippet()) ? item.snippet().trim() : null,
                domain,
                null,
                null,
                null,
                false,
                false,
                null,
                null,
                false,
                null,
                null,
                false,
                extractPublishedAt(item));
    }

    /** Extrai domínio da URL final retornada pelo Google para rastreabilidade. */
    private String extractDomain(String sourceUrl) {
        try {
            URI uri = URI.create(sourceUrl);
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return null;
            }
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (RuntimeException ex) {
            log.warn("Domínio de fonte ignorado por URL inválida no Google Custom Search (sourceUrl={})", sourceUrl, ex);
            return null;
        }
    }

    /** Extrai data de publicação exposta em metatags do Google Custom Search quando disponível. */
    private Instant extractPublishedAt(GoogleSearchItem item) {
        if (item.pagemap() == null || item.pagemap().metatags() == null) {
            return null;
        }
        for (GoogleSearchMetatag metatag : item.pagemap().metatags()) {
            Instant date = firstValidDate(
                    metatag.articlePublishedTime(),
                    metatag.datePublished(),
                    metatag.dateModified(),
                    metatag.ogUpdatedTime());
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    /** Retorna a primeira data válida encontrada em campos comuns de metadados públicos. */
    private Instant firstValidDate(String... values) {
        for (String value : values) {
            Instant parsed = parseDate(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    /** Converte data ISO completa ou data simples em instante UTC para cálculo de frescor. */
    private Instant parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value.trim()).atStartOfDay().toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    /** Representa a resposta mínima da API Google Custom Search usada pelo coletor. */
    record GoogleSearchResponse(List<GoogleSearchItem> items) {}

    /** Representa um resultado orgânico mínimo retornado pelo Google Custom Search. */
    record GoogleSearchItem(String title, String link, String snippet, GoogleSearchPagemap pagemap) {}

    /** Representa o mapa de metadados públicos retornado pelo Google Custom Search. */
    record GoogleSearchPagemap(List<GoogleSearchMetatag> metatags) {}

    /** Representa metatags comuns de data retornadas pelo Google Custom Search. */
    record GoogleSearchMetatag(
            @com.fasterxml.jackson.annotation.JsonProperty("article:published_time") String articlePublishedTime,
            @com.fasterxml.jackson.annotation.JsonProperty("datePublished") String datePublished,
            @com.fasterxml.jackson.annotation.JsonProperty("dateModified") String dateModified,
            @com.fasterxml.jackson.annotation.JsonProperty("og:updated_time") String ogUpdatedTime) {}
}
