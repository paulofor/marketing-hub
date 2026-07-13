package com.marketinghub.scientificresearch.productevidence.v1.sourcediscovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.scientificresearch.config.ScientificResearchProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Busca evidências em APIs científicas públicas sem exigir chave externa.
 */
@Component
public class ScientificSourceSearchClient {

    private static final Logger log = LoggerFactory.getLogger(ScientificSourceSearchClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ScientificResearchProperties properties;

    /**
     * Configura o client para PubMed e Crossref.
     */
    public ScientificSourceSearchClient(
            WebClient.Builder builder,
            ObjectMapper objectMapper,
            ScientificResearchProperties properties) {
        this.webClient = builder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Pesquisa fontes científicas e remove duplicidades simples por URL.
     */
    public SourceDiscoveryOutput search(String query) {
        List<ScientificSource> sources = new ArrayList<>();
        List<String> rejectedReasons = new ArrayList<>();
        sources.addAll(searchPubMed(query, rejectedReasons));
        sources.addAll(searchCrossref(query, rejectedReasons));

        Set<String> seen = new LinkedHashSet<>();
        List<ScientificSource> unique = sources.stream()
                .filter(source -> seen.add(source.url()))
                .limit(8)
                .toList();
        return new SourceDiscoveryOutput(query, unique, rejectedReasons);
    }

    /**
     * Busca artigos no PubMed usando esearch e esummary.
     */
    private List<ScientificSource> searchPubMed(String query, List<String> rejectedReasons) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI searchUri = URI.create(properties.getPubmedBaseUrl()
                    + "/esearch.fcgi?db=pubmed&retmode=json&retmax=6&term="
                    + encoded);
            log.info("Buscando fontes PubMed url={} query={}", searchUri, query);
            String searchResponse = webClient.get()
                    .uri(searchUri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(properties.getRequestTimeout());
            log.info("Resposta bruta PubMed esearch url={} response={}", searchUri, searchResponse);
            JsonNode ids = objectMapper.readTree(searchResponse).path("esearchresult").path("idlist");
            if (!ids.isArray() || ids.isEmpty()) {
                rejectedReasons.add("PubMed não retornou artigos para a consulta.");
                return List.of();
            }
            List<String> idValues = new ArrayList<>();
            ids.forEach(id -> idValues.add(id.asText()));
            URI summaryUri = URI.create(properties.getPubmedBaseUrl()
                    + "/esummary.fcgi?db=pubmed&retmode=json&id="
                    + String.join(",", idValues));
            log.info("Buscando resumos PubMed url={}", summaryUri);
            String summaryResponse = webClient.get()
                    .uri(summaryUri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(properties.getRequestTimeout());
            log.info("Resposta bruta PubMed esummary url={} response={}", summaryUri, summaryResponse);
            return parsePubMedSummary(summaryResponse, idValues);
        } catch (Exception ex) {
            log.error("Falha ao pesquisar PubMed query={}", query, ex);
            rejectedReasons.add("Falha técnica ao pesquisar PubMed: " + ex.getMessage());
            return List.of();
        }
    }

    /**
     * Converte o retorno do PubMed em fontes científicas.
     */
    private List<ScientificSource> parsePubMedSummary(String response, List<String> ids) throws Exception {
        JsonNode root = objectMapper.readTree(response).path("result");
        List<ScientificSource> sources = new ArrayList<>();
        for (String id : ids) {
            JsonNode item = root.path(id);
            String title = item.path("title").asText("");
            if (title.isBlank()) {
                continue;
            }
            String year = item.path("pubdate").asText("").replaceAll("^([0-9]{4}).*$", "$1");
            sources.add(new ScientificSource(
                    title,
                    "https://pubmed.ncbi.nlm.nih.gov/" + id + "/",
                    item.path("fulljournalname").asText("PubMed"),
                    year,
                    "PubMed",
                    classifyQuality(title),
                    "Fonte biomédica indexada útil para validar mecanismo e limites."));
        }
        return sources;
    }

    /**
     * Busca metadados acadêmicos na Crossref.
     */
    private List<ScientificSource> searchCrossref(String query, List<String> rejectedReasons) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI uri = URI.create(properties.getCrossrefBaseUrl()
                    + "/works?rows=5&filter=type:journal-article&query.title="
                    + encoded);
            log.info("Buscando fontes Crossref url={} query={}", uri, query);
            String response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(properties.getRequestTimeout());
            log.info("Resposta bruta Crossref url={} response={}", uri, response);
            return parseCrossref(response);
        } catch (Exception ex) {
            log.error("Falha ao pesquisar Crossref query={}", query, ex);
            rejectedReasons.add("Falha técnica ao pesquisar Crossref: " + ex.getMessage());
            return List.of();
        }
    }

    /**
     * Converte o retorno da Crossref em fontes científicas.
     */
    private List<ScientificSource> parseCrossref(String response) throws Exception {
        JsonNode items = objectMapper.readTree(response).path("message").path("items");
        if (!items.isArray()) {
            return List.of();
        }
        List<ScientificSource> sources = new ArrayList<>();
        for (JsonNode item : items) {
            String title = firstText(item.path("title"));
            String doi = item.path("DOI").asText("");
            if (title.isBlank() || doi.isBlank()) {
                continue;
            }
            String year = item.path("published-print").path("date-parts").path(0).path(0).asText("");
            if (year.isBlank()) {
                year = item.path("published-online").path("date-parts").path(0).path(0).asText("");
            }
            sources.add(new ScientificSource(
                    title,
                    "https://doi.org/" + doi,
                    firstText(item.path("container-title")),
                    year,
                    "Crossref",
                    classifyQuality(title),
                    "Artigo acadêmico com DOI útil para triangulação de evidência."));
        }
        return sources;
    }

    /**
     * Lê o primeiro texto de um array JSON.
     */
    private String firstText(JsonNode node) {
        if (node.isArray() && !node.isEmpty()) {
            return node.get(0).asText("");
        }
        return node.asText("");
    }

    /**
     * Classifica qualidade inicial por sinais conservadores no título.
     */
    private String classifyQuality(String title) {
        String normalized = title == null ? "" : title.toLowerCase();
        if (normalized.contains("systematic review") || normalized.contains("meta-analysis")) {
            return "alta";
        }
        if (normalized.contains("randomized") || normalized.contains("clinical trial")) {
            return "media-alta";
        }
        return "media";
    }
}
