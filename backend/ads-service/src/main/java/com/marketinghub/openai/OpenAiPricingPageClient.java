package com.marketinghub.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: consultar a página oficial de preços da OpenAI e extrair preços tokenizados de modelos suportados. */
@Component
public class OpenAiPricingPageClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiPricingPageClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern MONEY_PATTERN = Pattern.compile("[0-9]+(?:\\.[0-9]+)?");
    private static final Pattern CONTEXT_SUFFIX_PATTERN =
            Pattern.compile("\\s*\\([^)]*context[^)]*\\)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final int PRICING_PAGE_MAX_IN_MEMORY_BYTES = 2 * 1024 * 1024;
    public static final String PRICING_PAGE_URL = "https://developers.openai.com/api/docs/pricing";

    private final WebClient pricingPageWebClient;

    /** Inicializa o cliente com um WebClient público para a página oficial de preços. */
    public OpenAiPricingPageClient(WebClient.Builder builder) {
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(PRICING_PAGE_MAX_IN_MEMORY_BYTES))
                .build();
        this.pricingPageWebClient =
                builder.baseUrl(PRICING_PAGE_URL).exchangeStrategies(exchangeStrategies).build();
    }

    /** Inicializa o cliente em testes focados no parser da página oficial. */
    OpenAiPricingPageClient() {
        this.pricingPageWebClient = null;
    }

    /** Busca preços exclusivamente na página oficial da OpenAI e falha quando ela não entregar preços tokenizados. */
    public List<OpenAiModelPricing> fetchAllModelPricing() {
        if (pricingPageWebClient == null) {
            throw new IllegalStateException(
                    "Cliente público da página de preços OpenAI não configurado para buscar preços.");
        }
        try {
            String html = pricingPageWebClient.get().uri("").retrieve().bodyToMono(String.class).block();
            List<OpenAiModelPricing> prices = parsePricingPage(html);
            if (prices.isEmpty()) {
                throw new IllegalStateException(
                        "Página oficial de preços OpenAI não retornou preços tokenizados para sincronização.");
            }
            log.info(
                    "Preços OpenAI obtidos pela página oficial; operation=openai-pricing-page-fetch models={} url={}",
                    prices.size(),
                    PRICING_PAGE_URL);
            return prices;
        } catch (RuntimeException ex) {
            log.error(
                    "Falha ao buscar preços na página oficial OpenAI; operation=openai-pricing-page-fetch url={}",
                    PRICING_PAGE_URL,
                    ex);
            throw ex;
        }
    }

    /** Converte o HTML da página oficial de pricing em preços por 1 milhão de tokens. */
    List<OpenAiModelPricing> parsePricingPage(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document document = Jsoup.parse(html);
        Map<String, PriceTriple> standardPrices = new LinkedHashMap<>();
        Map<String, PriceTriple> batchPrices = new HashMap<>();
        for (Element pane : document.select("[data-content-switcher-pane]")) {
            String mode = pane.attr("data-value").trim().toLowerCase(Locale.ROOT);
            if (!"standard".equals(mode) && !"batch".equals(mode)) {
                continue;
            }
            parsePricePanes(pane).forEach((code, price) -> {
                if ("standard".equals(mode)) {
                    standardPrices.putIfAbsent(code, price);
                } else {
                    batchPrices.putIfAbsent(code, price);
                }
            });
        }
        List<OpenAiModelPricing> prices = new ArrayList<>();
        for (Map.Entry<String, PriceTriple> entry : standardPrices.entrySet()) {
            PriceTriple standard = entry.getValue();
            PriceTriple batch = batchPrices.get(entry.getKey());
            if (batch == null) {
                continue;
            }
            prices.add(new OpenAiModelPricing(
                    entry.getKey(),
                    entry.getKey(),
                    standard.input(),
                    standard.cachedInput(),
                    standard.output(),
                    batch.input(),
                    batch.cachedInput(),
                    batch.output()));
        }
        return prices;
    }

    /** Busca preços tokenizados preservando compatibilidade com chamadas antigas focadas em texto. */
    public List<OpenAiModelPricing> fetchTextModelPricing() {
        return fetchAllModelPricing();
    }

    /** Extrai preços de todas as tabelas tokenizadas dentro de uma aba Standard ou Batch. */
    private Map<String, PriceTriple> parsePricePanes(Element pane) {
        Map<String, PriceTriple> prices = new LinkedHashMap<>();
        prices.putAll(parseAstroComponentPrices(pane));
        for (Element table : pane.select("table")) {
            if (isFineTuningTable(table)) {
                continue;
            }
            for (Element row : table.select("tbody tr")) {
                Optional<PriceTriple> price = parsePricingRow(row);
                price.ifPresent(value -> prices.putIfAbsent(value.name(), value));
            }
        }
        return prices;
    }

    /** Extrai preços dos props estruturados dos componentes Astro, incluindo linhas ocultas atrás de "All models". */
    private Map<String, PriceTriple> parseAstroComponentPrices(Element pane) {
        Map<String, PriceTriple> prices = new LinkedHashMap<>();
        for (Element island : pane.select("astro-island[props]")) {
            String component = island.attr("component-export");
            if (!"TextTokenPricingTables".equals(component) && !"PricingTable".equals(component)) {
                continue;
            }
            try {
                JsonNode props = OBJECT_MAPPER.readTree(island.attr("props"));
                if (isFineTuningProps(props)) {
                    continue;
                }
                readAstroRows(props)
                        .forEach(row -> parsePricingRow(row)
                                .ifPresent(price -> prices.putIfAbsent(price.name(), price)));
            } catch (JsonProcessingException ex) {
                log.warn(
                        "Falha ao ler props de preços OpenAI; operation=openai-pricing-page-parse component={}",
                        component,
                        ex);
            }
        }
        return prices;
    }

    /** Ignora tabelas de fine-tuning porque incluem coluna de treinamento que não representa inferência tokenizada. */
    private boolean isFineTuningTable(Element table) {
        return table.select("thead").text().toLowerCase(Locale.ROOT).contains("training");
    }

    /** Ignora props de fine-tuning porque incluem coluna de treinamento que não representa inferência tokenizada. */
    private boolean isFineTuningProps(JsonNode props) {
        return readAstroArray(props.path("headings")).stream()
                .map(this::readAstroScalar)
                .anyMatch(value -> "training".equalsIgnoreCase(value));
    }

    /** Lê as linhas serializadas pelo Astro no formato tipado usado pela documentação da OpenAI. */
    private List<List<String>> readAstroRows(JsonNode props) {
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode rowNode : readAstroArray(props.path("rows"))) {
            List<String> cells = readAstroArray(rowNode).stream().map(this::readAstroScalar).toList();
            rows.add(cells);
        }
        return rows;
    }

    /** Desembrulha arrays serializados como [tipo, valor] pelos componentes Astro. */
    private List<JsonNode> readAstroArray(JsonNode node) {
        JsonNode value = unwrapAstroNode(node);
        if (!value.isArray()) {
            return List.of();
        }
        List<JsonNode> values = new ArrayList<>();
        value.forEach(values::add);
        return values;
    }

    /** Lê um valor escalar serializado pelo Astro, convertendo HTML rico em texto simples. */
    private String readAstroScalar(JsonNode node) {
        JsonNode value = unwrapAstroNode(node);
        if (value.isObject() && value.has("__pricingHtml")) {
            return Jsoup.parse(readAstroScalar(value.path("__pricingHtml"))).text();
        }
        if (value.isNull() || value.isMissingNode()) {
            return "";
        }
        return value.asText("");
    }

    /** Remove o envelope tipado do Astro quando o nó estiver no formato [tipo, valor]. */
    private JsonNode unwrapAstroNode(JsonNode node) {
        if (node != null && node.isArray() && node.size() == 2 && node.get(0).isInt()) {
            return node.get(1);
        }
        return node == null ? OBJECT_MAPPER.nullNode() : node;
    }

    /** Converte uma linha de tabela oficial em trio input/cache/output quando ela representa inferência tokenizada. */
    private Optional<PriceTriple> parsePricingRow(Element row) {
        List<Element> cells = row.select("td");
        if (cells.size() == 7) {
            return parseTextPricingRow(cells);
        }
        if (cells.size() == 5) {
            return parseImagePricingRow(cells);
        }
        if (cells.size() == 4) {
            return parseSimplePricingRow(cells);
        }
        return Optional.empty();
    }

    /** Converte uma linha estruturada dos props oficiais em trio input/cache/output quando ela é tokenizada. */
    private Optional<PriceTriple> parsePricingRow(List<String> cells) {
        if (cells.size() == 4) {
            return buildPriceTriple(cells.get(0), cells.get(1), cells.get(2), cells.get(3));
        }
        if (cells.size() == 5 && "image".equalsIgnoreCase(cells.get(1))) {
            return buildPriceTriple(cells.get(0), cells.get(2), cells.get(3), cells.get(4));
        }
        return Optional.empty();
    }

    /** Extrai preço de tabela textual com colunas de contexto curto e longo, usando o preço base de contexto curto. */
    private Optional<PriceTriple> parseTextPricingRow(List<Element> cells) {
        return buildPriceTriple(cells.get(0).text(), cells.get(1).text(), cells.get(2).text(), cells.get(3).text());
    }

    /** Extrai preço de tabela multimodal, priorizando a modalidade principal Image quando houver linhas por modalidade. */
    private Optional<PriceTriple> parseImagePricingRow(List<Element> cells) {
        String modality = cells.get(1).text().trim();
        if (!"image".equalsIgnoreCase(modality)) {
            return Optional.empty();
        }
        return buildPriceTriple(cells.get(0).text(), cells.get(2).text(), cells.get(3).text(), cells.get(4).text());
    }

    /** Extrai preço de tabelas simples com Model, Input, Cached input e Output. */
    private Optional<PriceTriple> parseSimplePricingRow(List<Element> cells) {
        return buildPriceTriple(cells.get(0).text(), cells.get(1).text(), cells.get(2).text(), cells.get(3).text());
    }

    /** Monta o trio de preço apenas quando modelo, input e output estão publicados na fonte oficial. */
    private Optional<PriceTriple> buildPriceTriple(String rawCode, String rawInput, String rawCachedInput, String rawOutput) {
        String code = normalizeModelCode(rawCode);
        if (!isSupportedTokenModelCode(code)) {
            return Optional.empty();
        }
        BigDecimal input = parseMoney(rawInput);
        BigDecimal output = parseMoney(rawOutput);
        if (input == null || output == null) {
            return Optional.empty();
        }
        return Optional.of(new PriceTriple(code, input, zeroIfNull(parseMoney(rawCachedInput)), output));
    }

    /** Normaliza nomes publicados na tabela removendo sufixos visuais que não fazem parte do código do modelo. */
    private String normalizeModelCode(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toLowerCase(Locale.ROOT);
        int lineBreak = code.indexOf('\n');
        if (lineBreak >= 0) {
            code = code.substring(0, lineBreak);
        }
        code = CONTEXT_SUFFIX_PATTERN.matcher(code).replaceFirst("");
        return code.trim();
    }

    /** Converte uma resposta autenticada de /models em preços a partir dos metadados financeiros do payload legado. */
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

    /** Seleciona preço exato ou preço-base mais específico para variantes datadas retornadas pela fonte oficial. */
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

    /** Verifica se o código solicitado é uma variante datada do código-base publicado na fonte oficial. */
    private boolean isVersionVariantOf(String requestedCode, String pricedCode) {
        if (requestedCode == null || pricedCode == null || requestedCode.equals(pricedCode)) {
            return false;
        }
        return requestedCode.startsWith(pricedCode + "-");
    }

    /** Converte valores monetários da fonte oficial para decimal por 1 milhão de tokens. */
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
