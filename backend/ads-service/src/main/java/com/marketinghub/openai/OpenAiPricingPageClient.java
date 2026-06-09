package com.marketinghub.openai;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: consultar a API autenticada da OpenAI e extrair preços tokenizados de modelos suportados. */
@Component
public class OpenAiPricingPageClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiPricingPageClient.class);
    private static final Pattern MONEY_PATTERN = Pattern.compile("[0-9]+(?:\\.[0-9]+)?");

    private final WebClient openAiWebClient;

    /** Inicializa o cliente com o WebClient autenticado da API OpenAI. */
    @Autowired
    public OpenAiPricingPageClient(@Qualifier("openAiWebClient") WebClient openAiWebClient) {
        this.openAiWebClient = openAiWebClient;
    }

    /** Inicializa o cliente em testes focados no parser da resposta autenticada. */
    OpenAiPricingPageClient() {
        this.openAiWebClient = null;
    }

    /** Busca preços exclusivamente na API autenticada da OpenAI e falha quando a API não entregar preços. */
    public List<OpenAiModelPricing> fetchAllModelPricing() {
        if (openAiWebClient == null) {
            throw new IllegalStateException("Cliente autenticado da OpenAI não configurado para buscar preços.");
        }
        try {
            JsonNode response = openAiWebClient.get().uri("/models").retrieve().bodyToMono(JsonNode.class).block();
            List<OpenAiModelPricing> prices = parseAuthenticatedApiPricing(response);
            if (prices.isEmpty()) {
                throw new IllegalStateException("API OpenAI /models não retornou metadados de preço para sincronização.");
            }
            log.info(
                    "Preços OpenAI obtidos pela API autenticada; operation=openai-pricing-api-fetch models={}",
                    prices.size());
            return prices;
        } catch (RuntimeException ex) {
            log.error("Falha ao buscar preços pela API autenticada OpenAI; operation=openai-pricing-api-fetch endpoint=/models", ex);
            throw ex;
        }
    }

    /** Busca preços tokenizados preservando compatibilidade com chamadas antigas focadas em texto. */
    public List<OpenAiModelPricing> fetchTextModelPricing() {
        return fetchAllModelPricing();
    }

    /** Converte uma resposta autenticada de /models em preços a partir dos metadados financeiros do payload. */
    List<OpenAiModelPricing> parseAuthenticatedApiPricing(JsonNode response) {
        if (response == null || !response.path("data").isArray()) {
            return List.of();
        }
        List<OpenAiModelPricing> prices = new ArrayList<>();
        for (JsonNode model : response.path("data")) {
            String code = model.path("id").asText("").trim().toLowerCase(Locale.ROOT);
            if (!isSupportedTokenModelCode(code)) {
                continue;
            }
            Optional<PriceTriple> standard = findApiPriceTriple(model, code, PricingMode.STANDARD);
            if (standard.isEmpty()) {
                continue;
            }
            Optional<PriceTriple> batch = findApiPriceTriple(model, code, PricingMode.BATCH);
            if (batch.isEmpty()) {
                continue;
            }
            prices.add(new OpenAiModelPricing(
                    code,
                    code,
                    standard.get().input(),
                    standard.get().cachedInput(),
                    standard.get().output(),
                    batch.get().input(),
                    batch.get().cachedInput(),
                    batch.get().output()));
        }
        return prices;
    }

    /** Seleciona preço exato ou preço-base mais específico para variantes datadas retornadas por /models. */
    public Optional<OpenAiModelPricing> findBestModelPricing(List<OpenAiModelPricing> prices, String modelCode) {
        if (prices == null || modelCode == null || modelCode.isBlank()) {
            return Optional.empty();
        }
        String normalizedCode = modelCode.trim().toLowerCase(Locale.ROOT);
        Optional<OpenAiModelPricing> exact = prices.stream()
                .filter(price -> normalizedCode.equals(price.code()))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        return prices.stream()
                .filter(price -> isVersionVariantOf(normalizedCode, price.code()))
                .max((left, right) -> Integer.compare(left.code().length(), right.code().length()));
    }

    /** Seleciona preço mantendo compatibilidade com chamadas antigas focadas em texto. */
    public Optional<OpenAiModelPricing> findBestTextModelPricing(List<OpenAiModelPricing> prices, String modelCode) {
        return findBestModelPricing(prices, modelCode);
    }

    /** Localiza no JSON da API o bloco financeiro standard ou batch, priorizando modalidade operacional correta. */
    private Optional<PriceTriple> findApiPriceTriple(JsonNode model, String code, PricingMode mode) {
        JsonNode pricing = model.path("pricing");
        if (pricing.isMissingNode() || pricing.isNull()) {
            return Optional.empty();
        }
        List<JsonNode> candidates = new ArrayList<>();
        JsonNode modeNode = pricing.path(mode.name().toLowerCase(Locale.ROOT));
        addApiPricingCandidates(candidates, modeNode, code);
        addApiPricingCandidates(candidates, pricing.path(code.startsWith("gpt-image") ? "image" : "text"), code);
        addApiPricingCandidates(candidates, pricing, code);
        for (JsonNode candidate : candidates) {
            PriceTriple price = parseApiPriceTriple(code, candidate);
            if (price != null) {
                return Optional.of(price);
            }
        }
        return Optional.empty();
    }

    /** Adiciona candidatos de preço da API considerando objetos diretos e subobjetos por modalidade. */
    private void addApiPricingCandidates(List<JsonNode> candidates, JsonNode node, String code) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (code.startsWith("gpt-image")) {
            candidates.add(node.path("image"));
        } else {
            candidates.add(node.path("text"));
        }
        candidates.add(node);
    }

    /** Converte um bloco financeiro da API em trio input/cache/output por 1 milhão de tokens. */
    private PriceTriple parseApiPriceTriple(String code, JsonNode node) {
        BigDecimal input = readMoney(node, "input", "input_price", "inputPrice");
        BigDecimal cachedInput = readMoney(node, "cached_input", "input_cached", "cachedInput", "inputCached");
        BigDecimal output = readMoney(node, "output", "output_price", "outputPrice");
        if (input == null || output == null) {
            return null;
        }
        return new PriceTriple(code, input, zeroIfNull(cachedInput), output);
    }

    /** Lê campos monetários flexíveis publicados pela API como número ou texto. */
    private BigDecimal readMoney(JsonNode node, String... fieldNames) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isNumber()) {
                return value.decimalValue();
            }
            if (value.isTextual()) {
                BigDecimal parsed = parseMoney(value.asText());
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        return null;
    }

    /** Indica se o código extraído representa modelo tokenizado suportado no catálogo financeiro. */
    private boolean isSupportedTokenModelCode(String code) {
        return code.startsWith("gpt-")
                || code.startsWith("o1")
                || code.startsWith("o3")
                || code.startsWith("o4")
                || code.startsWith("text-")
                || code.startsWith("chatgpt-")
                || code.startsWith("computer-use-");
    }

    /** Verifica se o código de /models é uma variante datada do código-base publicado na API. */
    private boolean isVersionVariantOf(String requestedCode, String pricedCode) {
        if (requestedCode == null || pricedCode == null || requestedCode.equals(pricedCode)) {
            return false;
        }
        return requestedCode.startsWith(pricedCode + "-");
    }

    /** Converte valores monetários da API para decimal por 1 milhão de tokens. */
    private BigDecimal parseMoney(String value) {
        String normalized = value == null ? "" : value.replace(",", "").trim();
        Matcher matcher = MONEY_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        return new BigDecimal(matcher.group());
    }

    /** Garante valor zero para campos de preço não publicados, preservando restrição NOT NULL do banco. */
    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** Responsabilidade: indicar o modo financeiro publicado pela API de preços. */
    private enum PricingMode {
        STANDARD,
        BATCH
    }

    /** Responsabilidade: manter um trio de preços input/cache/output por modo de processamento. */
    private record PriceTriple(String name, BigDecimal input, BigDecimal cachedInput, BigDecimal output) {}
}
