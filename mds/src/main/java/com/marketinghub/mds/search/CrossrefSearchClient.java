package com.marketinghub.mds.search;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class CrossrefSearchClient implements EvidenceSearchClient {
    private static final String WORKS_URL = "https://api.crossref.org/works";

    private final RestClient restClient;

    public CrossrefSearchClient() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String source() {
        return "crossref";
    }

    @Override
    public List<SourceSearchHit> search(String query, int limit) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(WORKS_URL)
                            .queryParam("query.bibliographic", query)
                            .queryParam("rows", limit)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            List<SourceSearchHit> hits = new ArrayList<>();
            JsonNode items = response.path("message").path("items");
            if (!items.isArray()) {
                return List.of();
            }

            for (JsonNode item : items) {
                String doi = item.path("DOI").asText("");
                JsonNode titleNode = item.path("title");
                String title = (titleNode.isArray() && !titleNode.isEmpty()) ? titleNode.get(0).asText("") : "";
                String url = item.path("URL").asText("https://doi.org/" + doi);

                List<String> authors = new ArrayList<>();
                JsonNode authorsNode = item.path("author");
                if (authorsNode.isArray()) {
                    for (JsonNode author : authorsNode) {
                        authors.add((author.path("given").asText("") + " " + author.path("family").asText(""))
                                .trim());
                    }
                }

                boolean open = item.path("is-oa").asBoolean(false);

                hits.add(new SourceSearchHit(
                        source(),
                        "crossref:" + (doi.isBlank() ? item.path("indexed").path("timestamp").asText("unknown") : doi),
                        doi.isBlank() ? null : doi,
                        title,
                        item.path("abstract").asText(null),
                        url,
                        item.path("published").path("date-parts").isArray() ? item.path("published").path("date-parts").get(0).get(0).asText("") : "",
                        item.path("license").isArray() && !item.path("license").isEmpty() ? item.path("license").get(0).path("URL").asText(null) : null,
                        open,
                        open,
                        open,
                        authors
                ));
            }

            return hits;
        } catch (Exception ex) {
            return List.of();
        }
    }
}
