package com.marketinghub.mds.search;

import com.marketinghub.mds.config.MdsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PubmedSearchClient implements EvidenceSearchClient {
    private static final String SEARCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
    private static final String SUMMARY_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi";

    private final RestClient restClient;

    public PubmedSearchClient(MdsProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(500, properties.getSearch().getTimeoutMs());
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String source() {
        return "pubmed";
    }

    @Override
    public List<SourceSearchHit> search(String query, int limit) {
        try {
            JsonNode search = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(SEARCH_URL)
                            .queryParam("db", "pubmed")
                            .queryParam("term", query)
                            .queryParam("retmode", "json")
                            .queryParam("retmax", limit)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode idsNode = search.path("esearchresult").path("idlist");
            if (!idsNode.isArray() || idsNode.isEmpty()) {
                return List.of();
            }

            List<String> idList = new ArrayList<>();
            idsNode.forEach(id -> idList.add(id.asText()));
            String ids = idList.stream().collect(Collectors.joining(","));

            JsonNode summary = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(SUMMARY_URL)
                            .queryParam("db", "pubmed")
                            .queryParam("id", ids)
                            .queryParam("retmode", "json")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            List<SourceSearchHit> hits = new ArrayList<>();
            for (JsonNode idNode : idsNode) {
                String id = idNode.asText();
                JsonNode doc = summary.path("result").path(id);
                if (doc.isMissingNode()) {
                    continue;
                }

                List<String> authors = new ArrayList<>();
                JsonNode authorsNode = doc.path("authors");
                if (authorsNode.isArray()) {
                    authorsNode.forEach(a -> authors.add(a.path("name").asText("")));
                }

                hits.add(new SourceSearchHit(
                        source(),
                        "pubmed:" + id,
                        null,
                        doc.path("title").asText(""),
                        null,
                        "https://pubmed.ncbi.nlm.nih.gov/" + id + "/",
                        doc.path("pubdate").asText(""),
                        null,
                        true,
                        false,
                        false,
                        authors
                ));
            }
            return hits;
        } catch (Exception ex) {
            throw new RecoverableSourceException("pubmed source call failed", ex);
        }
    }
}
