package com.marketinghub.modelos.openai.catalogo.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketinghub.modelos.openai.catalogo.v1.dto.OpenAiModelCatalogPriceResponse;
import com.marketinghub.modelos.openai.catalogo.v1.dto.OpenAiModelCatalogResponse;
import com.marketinghub.modelos.openai.catalogo.v1.entity.OpenAiCatalogModelV1;
import com.marketinghub.openai.OpenAiModelPricing;
import com.marketinghub.openai.OpenAiPricingPageClient;
import com.marketinghub.repository.jpa.modelos.openai.catalogo.v1.OpenAiCatalogModelV1Repository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: consultar a API oficial /models da OpenAI e persistir o catálogo técnico retornado. */
@Service
public class OpenAiModelCatalogV1Service {
    private static final Logger log = LoggerFactory.getLogger(OpenAiModelCatalogV1Service.class);

    private final WebClient openAiWebClient;
    private final OpenAiCatalogModelV1Repository repository;
    private final OpenAiPricingPageClient pricingPageClient;

    /** Inicializa o serviço com o WebClient autenticado, repositório de catálogo técnico e cliente oficial de preços. */
    public OpenAiModelCatalogV1Service(
            @Qualifier("openAiWebClient") WebClient openAiWebClient,
            OpenAiCatalogModelV1Repository repository,
            OpenAiPricingPageClient pricingPageClient) {
        this.openAiWebClient = openAiWebClient;
        this.repository = repository;
        this.pricingPageClient = pricingPageClient;
    }

    /** Busca modelos em /models, separa texto/imagem e salva os códigos reconhecidos para auditoria local. */
    @Transactional
    public OpenAiModelCatalogResponse fetchAndPersistCatalog() {
        try {
            JsonNode response = openAiWebClient.get().uri("/models").retrieve().bodyToMono(JsonNode.class).block();
            Set<String> text = new TreeSet<>();
            Set<String> image = new TreeSet<>();
            Instant now = Instant.now();
            if (response != null && response.has("data") && response.get("data").isArray()) {
                for (JsonNode item : response.get("data")) {
                    persistRecognizedModel(item, text, image, now);
                }
            }
            return new OpenAiModelCatalogResponse(
                    new ArrayList<>(text),
                    new ArrayList<>(image),
                    fetchPricingByModel(),
                    "openai:/models + openai:pricing",
                    now.toString());
        } catch (RuntimeException ex) {
            log.error("Falha ao consultar catálogo oficial OpenAI; operation=openai-model-catalog-fetch endpoint=/models", ex);
            throw ex;
        }
    }

    /** Busca preços oficiais de texto e indexa por código do modelo para enriquecer a lista de seleção. */
    private Map<String, OpenAiModelCatalogPriceResponse> fetchPricingByModel() {
        try {
            List<OpenAiModelPricing> pricingRows = pricingPageClient.fetchTextModelPricing();
            Map<String, OpenAiModelCatalogPriceResponse> pricingByModel = new LinkedHashMap<>();
            for (OpenAiModelPricing pricing : pricingRows) {
                pricingByModel.put(pricing.code(), toPriceResponse(pricing));
            }
            return pricingByModel;
        } catch (RuntimeException ex) {
            log.error(
                    "Falha ao consultar preços oficiais OpenAI para catálogo; operation=openai-model-catalog-pricing source=pricing-page",
                    ex);
            return Map.of();
        }
    }

    /** Converte a linha de preço interna no DTO exposto para o frontend. */
    private OpenAiModelCatalogPriceResponse toPriceResponse(OpenAiModelPricing pricing) {
        return new OpenAiModelCatalogPriceResponse(
                pricing.priceInputStandard(),
                pricing.priceInputCachedStandard(),
                pricing.priceOutputStandard(),
                pricing.priceInputBatch(),
                pricing.priceInputCachedBatch(),
                pricing.priceOutputBatch());
    }

    /** Classifica um item bruto da API /models e persiste quando o código pertence a uma família reconhecida. */
    private void persistRecognizedModel(JsonNode item, Set<String> text, Set<String> image, Instant now) {
        String id = item.path("id").asText("").trim();
        if (id.isBlank()) {
            return;
        }
        String category = null;
        if (isImage(id)) {
            image.add(id);
            category = "IMAGE";
        } else if (isText(id)) {
            text.add(id);
            category = "TEXT";
        }
        if (category != null) {
            OpenAiCatalogModelV1 entity = repository.findByCode(id).orElseGet(OpenAiCatalogModelV1::new);
            entity.setCode(id);
            entity.setCategory(category);
            entity.setLastSeenAt(now);
            repository.save(entity);
        }
    }

    /** Indica se o código da API representa um modelo de geração/edição de imagem. */
    private boolean isImage(String id) {
        String normalized = id.toLowerCase();
        return normalized.startsWith("gpt-image") || normalized.startsWith("dall-e");
    }

    /** Indica se o código da API representa um modelo textual/reasoning usado no pipeline operacional. */
    private boolean isText(String id) {
        String normalized = id.toLowerCase();
        return normalized.startsWith("gpt-")
                || normalized.startsWith("o1")
                || normalized.startsWith("o3")
                || normalized.startsWith("o4")
                || normalized.startsWith("text-");
    }
}
