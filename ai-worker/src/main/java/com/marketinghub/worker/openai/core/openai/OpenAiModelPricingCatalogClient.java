package com.marketinghub.worker.openai.core.openai;

import com.marketinghub.worker.openai.core.exception.StageWorkerException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: consultar no backend o catálogo de modelos OpenAI persistido no banco de dados. */
public class OpenAiModelPricingCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiModelPricingCatalogClient.class);

    private final WebClient webClient;
    private final OpenAiClientProperties properties;
    private final AtomicReference<Map<String, OpenAiModelPricing>> cachedPricingByCode = new AtomicReference<>(Map.of());

    /** Inicializa o cliente HTTP com a URL do endpoint backend que expõe a tabela openai_model. */
    public OpenAiModelPricingCatalogClient(WebClient.Builder builder, OpenAiClientProperties properties) {
        this.webClient = Objects.requireNonNull(builder, "builder must not be null").build();
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /** Busca o preço do modelo por código, recarregando o catálogo do backend quando o cache ainda não contém o modelo. */
    public Optional<OpenAiModelPricing> findByCode(String modelCode) {
        String normalizedCode = normalizeModelCode(modelCode);
        if (normalizedCode == null) {
            return Optional.empty();
        }

        OpenAiModelPricing cached = cachedPricingByCode.get().get(normalizedCode);
        if (cached != null) {
            return Optional.of(cached);
        }

        Map<String, OpenAiModelPricing> refreshed = refreshCatalog();
        return Optional.ofNullable(refreshed.get(normalizedCode));
    }

    /** Recarrega do backend os modelos e preços cadastrados na tabela openai_model. */
    private Map<String, OpenAiModelPricing> refreshCatalog() {
        String catalogUrl = properties.pricingCatalogUrl();
        if (catalogUrl == null || catalogUrl.isBlank()) {
            throw new StageWorkerException("URL do catálogo de preços OpenAI do backend não configurada");
        }

        try {
            List<OpenAiModelPricing> models = webClient.get()
                    .uri(catalogUrl)
                    .retrieve()
                    .bodyToFlux(OpenAiModelPricing.class)
                    .collectList()
                    .block(properties.timeout());

            Map<String, OpenAiModelPricing> pricingByCode = (models == null ? List.<OpenAiModelPricing>of() : models)
                    .stream()
                    .filter(model -> normalizeModelCode(model.code()) != null)
                    .collect(Collectors.toUnmodifiableMap(
                            model -> normalizeModelCode(model.code()),
                            Function.identity(),
                            (first, ignored) -> first));
            cachedPricingByCode.set(pricingByCode);
            log.info(
                    "Catálogo de preços OpenAI carregado do backend [pricingCatalogUrl={}, modelCount={}]",
                    catalogUrl,
                    pricingByCode.size());
            return pricingByCode;
        } catch (RuntimeException ex) {
            log.error(
                    "Falha ao consultar catálogo de preços OpenAI no backend [pricingCatalogUrl={}]",
                    catalogUrl,
                    ex);
            throw ex;
        }
    }

    /** Normaliza o código de modelo para comparar com o cadastro persistido no banco. */
    private String normalizeModelCode(String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return null;
        }
        return modelCode.trim().toLowerCase(Locale.ROOT);
    }

    /** DTO mínimo do catálogo backend com preços batch/flex por milhão de tokens. */
    public record OpenAiModelPricing(
            String code,
            BigDecimal priceInputBatch,
            BigDecimal priceOutputBatch
    ) {}
}
