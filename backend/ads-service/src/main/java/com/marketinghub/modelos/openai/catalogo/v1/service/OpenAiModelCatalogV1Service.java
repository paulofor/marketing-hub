package com.marketinghub.modelos.openai.catalogo.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.modelos.openai.catalogo.v1.dto.OpenAiModelCatalogResponse;
import com.marketinghub.modelos.openai.catalogo.v1.entity.OpenAiCatalogModelV1;
import com.marketinghub.repository.jpa.modelos.openai.catalogo.v1.OpenAiCatalogModelV1Repository;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OpenAiModelCatalogV1Service {
    private final WebClient openAiWebClient;
    private final OpenAiCatalogModelV1Repository repository;

    public OpenAiModelCatalogV1Service(@Qualifier("openAiWebClient") WebClient openAiWebClient, OpenAiCatalogModelV1Repository repository) {
        this.openAiWebClient = openAiWebClient;
        this.repository = repository;
    }

    @Transactional
    public OpenAiModelCatalogResponse fetchAndPersistCatalog() {
        JsonNode response = openAiWebClient.get().uri("/models").retrieve().bodyToMono(JsonNode.class).block();
        Set<String> text = new TreeSet<>();
        Set<String> image = new TreeSet<>();
        Instant now = Instant.now();
        if (response != null && response.has("data") && response.get("data").isArray()) {
            for (JsonNode item : response.get("data")) {
                String id = item.path("id").asText("").trim();
                if (id.isBlank()) continue;
                String category = null;
                if (isImage(id)) { image.add(id); category = "IMAGE"; }
                else if (isText(id)) { text.add(id); category = "TEXT"; }
                if (category != null) {
                    OpenAiCatalogModelV1 entity = repository.findByCode(id).orElseGet(OpenAiCatalogModelV1::new);
                    entity.setCode(id);
                    entity.setCategory(category);
                    entity.setLastSeenAt(now);
                    repository.save(entity);
                }
            }
        }
        return new OpenAiModelCatalogResponse(new ArrayList<>(text), new ArrayList<>(image), "openai:/models", now.toString());
    }

    private boolean isImage(String id) { String n=id.toLowerCase(); return n.startsWith("gpt-image")||n.startsWith("dall-e"); }
    private boolean isText(String id) { String n=id.toLowerCase(); return n.startsWith("gpt-")||n.startsWith("o1")||n.startsWith("o3")||n.startsWith("o4")||n.startsWith("text-"); }
}
