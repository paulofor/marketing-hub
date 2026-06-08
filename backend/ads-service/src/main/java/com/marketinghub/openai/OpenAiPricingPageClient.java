package com.marketinghub.openai;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
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
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Responsabilidade: consultar a página oficial de preços da OpenAI e extrair preços tokenizados de modelos suportados. */
@Component
public class OpenAiPricingPageClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiPricingPageClient.class);
    private static final Pattern MONEY_PATTERN = Pattern.compile("[0-9]+(?:\\.[0-9]+)?");

    private final OpenAiProperties properties;

    /** Inicializa o cliente com as propriedades de URL/timeout da integração OpenAI. */
    public OpenAiPricingPageClient(OpenAiProperties properties) {
        this.properties = properties;
    }

    /** Busca a página oficial de preços e retorna modelos tokenizados com preços standard e batch consolidados. */
    public List<OpenAiModelPricing> fetchAllModelPricing() {
        try {
            String html = Jsoup.connect(properties.getPricingUrl())
                    .userAgent("MarketingHub/1.0 (+https://oportunidadebrasil.shop)")
                    .timeout(toRequestTimeoutMillis())
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .execute()
                    .body();
            return parseAllModelPricing(html);
        } catch (IOException ex) {
            log.error(
                    "Falha de IO ao buscar página oficial de preços OpenAI; operation=openai-pricing-fetch source={}",
                    properties.getPricingUrl(),
                    ex);
            throw new IllegalStateException("Não foi possível consultar a página oficial de preços da OpenAI.", ex);
        } catch (RuntimeException ex) {
            log.error(
                    "Falha inesperada ao buscar página oficial de preços OpenAI; operation=openai-pricing-fetch source={}",
                    properties.getPricingUrl(),
                    ex);
            throw ex;
        }
    }

    /** Busca preços tokenizados preservando compatibilidade com chamadas antigas focadas em texto. */
    public List<OpenAiModelPricing> fetchTextModelPricing() {
        return fetchAllModelPricing();
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

    /** Converte o timeout configurado para milissegundos aceitos pelo cliente HTTP de preços. */
    private int toRequestTimeoutMillis() {
        long millis = properties.getRequestTimeout().toMillis();
        return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(1, (int) millis);
    }

    /** Extrai os preços oficiais tokenizados, aceitando múltiplas seções standard/batch da página atual. */
    public List<OpenAiModelPricing> parseAllModelPricing(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        List<PricingTable> tables = parsePricingTables(html);
        if (tables.isEmpty()) {
            log.warn(
                    "Nenhuma tabela tokenizada de preço OpenAI encontrada; operation=openai-pricing-parse source={}",
                    properties.getPricingUrl());
            return List.of();
        }
        Map<String, PriceTriple> standard = new LinkedHashMap<>();
        Map<String, PriceTriple> batch = new LinkedHashMap<>();
        for (PricingTable table : tables) {
            if (table.mode() == PricingMode.SKIP) {
                continue;
            }
            for (Map.Entry<String, PriceTriple> entry : table.rows().entrySet()) {
                if (table.mode() == PricingMode.BATCH) {
                    batch.putIfAbsent(entry.getKey(), entry.getValue());
                } else if (table.mode() == PricingMode.STANDARD || !standard.containsKey(entry.getKey())) {
                    standard.putIfAbsent(entry.getKey(), entry.getValue());
                } else {
                    batch.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }
        List<OpenAiModelPricing> result = new ArrayList<>();
        for (Map.Entry<String, PriceTriple> entry : standard.entrySet()) {
            PriceTriple standardPrice = entry.getValue();
            PriceTriple batchPrice = batch.getOrDefault(entry.getKey(), standardPrice.half());
            result.add(new OpenAiModelPricing(
                    entry.getKey(),
                    standardPrice.name(),
                    standardPrice.input(),
                    standardPrice.cachedInput(),
                    standardPrice.output(),
                    batchPrice.input(),
                    batchPrice.cachedInput(),
                    batchPrice.output()));
        }
        return result;
    }

    /** Extrai preços mantendo compatibilidade com chamadas antigas focadas em texto. */
    public List<OpenAiModelPricing> parseTextModelPricing(String html) {
        return parseAllModelPricing(html);
    }

    /** Localiza tabelas com colunas de modelo, input, cached input e output e converte suas linhas em preços. */
    private List<PricingTable> parsePricingTables(String html) {
        Document document = Jsoup.parse(html);
        List<PricingTable> parsedTables = new ArrayList<>();
        for (Element table : document.select("table")) {
            String headerText = table.select("thead").text().toLowerCase(Locale.ROOT);
            if (!headerText.contains("model") || !headerText.contains("input") || !headerText.contains("output")) {
                continue;
            }
            Map<String, PriceTriple> rows = parseRows(table.select("tbody tr"));
            if (!rows.isEmpty()) {
                parsedTables.add(new PricingTable(resolveTableMode(table), rows));
            }
        }
        return parsedTables;
    }

    /** Converte linhas HTML de preço em mapa por código canônico do modelo e escolhe a modalidade operacional. */
    private Map<String, PriceTriple> parseRows(Elements rows) {
        Map<String, PriceTriple> tablePrices = new LinkedHashMap<>();
        Map<String, Integer> scores = new LinkedHashMap<>();
        String currentCode = null;
        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() < 4) {
                continue;
            }
            RowPricing rowPricing = parseRowPricing(cells, currentCode);
            if (rowPricing == null || !isSupportedTokenModelCode(rowPricing.code())) {
                continue;
            }
            currentCode = rowPricing.code();
            PriceTriple price = parsePriceTriple(cells, rowPricing.firstPriceCell(), rowPricing.code());
            if (price == null) {
                continue;
            }
            int score = modalityScore(rowPricing.code(), rowPricing.modality());
            if (score > scores.getOrDefault(rowPricing.code(), -1)) {
                tablePrices.put(rowPricing.code(), price);
                scores.put(rowPricing.code(), score);
            }
        }
        return tablePrices;
    }

    /** Identifica código, modalidade opcional e posição inicial dos preços dentro de uma linha de tabela. */
    private RowPricing parseRowPricing(Elements cells, String currentCode) {
        String first = cells.get(0).text().trim();
        String normalizedFirst = normalizeModelCode(first);
        if (isSupportedTokenModelCode(normalizedFirst)) {
            if (cells.size() >= 5 && isKnownModality(cells.get(1).text())) {
                return new RowPricing(normalizedFirst, cells.get(1).text().trim(), 2);
            }
            return new RowPricing(normalizedFirst, null, 1);
        }
        if (currentCode != null && isKnownModality(first)) {
            return new RowPricing(currentCode, first, 1);
        }
        return null;
    }

    /** Extrai um trio input/cache/output a partir da posição inicial informada na linha da tabela. */
    private PriceTriple parsePriceTriple(Elements cells, int firstPriceCell, String code) {
        if (cells.size() <= firstPriceCell + 2) {
            return null;
        }
        BigDecimal input = parseMoney(cells.get(firstPriceCell).text());
        BigDecimal cachedInput = parseMoney(cells.get(firstPriceCell + 1).text());
        BigDecimal output = parseMoney(cells.get(firstPriceCell + 2).text());
        if (input == null || output == null) {
            return null;
        }
        return new PriceTriple(code, input, zeroIfNull(cachedInput), output);
    }

    /** Classifica a tabela para separar preços standard, batch e modos que não cabem no contrato financeiro atual. */
    private PricingMode resolveTableMode(Element table) {
        String context = previousContext(table).toLowerCase(Locale.ROOT);
        int batch = context.lastIndexOf("batch");
        int standard = context.lastIndexOf("standard");
        int flex = context.lastIndexOf("flex");
        int priority = context.lastIndexOf("priority");
        if ((flex > batch && flex > standard) || (priority > batch && priority > standard)) {
            return PricingMode.SKIP;
        }
        if (batch > standard) {
            return PricingMode.BATCH;
        }
        if (standard >= 0) {
            return PricingMode.STANDARD;
        }
        return PricingMode.UNKNOWN;
    }

    /** Coleta contexto textual próximo antes da tabela para inferir se ela representa standard ou batch. */
    private String previousContext(Element table) {
        StringBuilder context = new StringBuilder();
        Element previous = table.previousElementSibling();
        for (int i = 0; previous != null && i < 8; i++) {
            String text = previous.text();
            if (!text.isBlank()) {
                context.insert(0, text + " ");
            }
            previous = previous.previousElementSibling();
        }
        return context.toString();
    }

    /** Remove observações de contexto e normaliza o código do modelo para comparação e persistência. */
    private String normalizeModelCode(String value) {
        return value.replaceAll("\\s*\\(.*$", "").trim().toLowerCase(Locale.ROOT);
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

    /** Verifica se a célula representa uma modalidade de preço tokenizado na tabela oficial. */
    private boolean isKnownModality(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("text") || normalized.equals("image") || normalized.equals("audio");
    }

    /** Prioriza a modalidade que melhor representa o uso operacional do modelo no contrato atual de preços. */
    private int modalityScore(String code, String modality) {
        if (modality == null || modality.isBlank()) {
            return 2;
        }
        String normalized = modality.trim().toLowerCase(Locale.ROOT);
        if (code.startsWith("gpt-image")) {
            return normalized.equals("image") ? 4 : 1;
        }
        if (code.startsWith("gpt-realtime") || code.startsWith("gpt-audio")) {
            return normalized.equals("audio") ? 4 : 2;
        }
        return normalized.equals("text") ? 4 : 1;
    }

    /** Verifica se o código de /models é uma variante datada do código-base publicado na página de preços. */
    private boolean isVersionVariantOf(String requestedCode, String pricedCode) {
        if (requestedCode == null || pricedCode == null || requestedCode.equals(pricedCode)) {
            return false;
        }
        return requestedCode.startsWith(pricedCode + "-");
    }

    /** Converte valores monetários da tabela oficial para decimal por 1 milhão de tokens. */
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

    /** Responsabilidade: indicar o papel operacional de uma tabela de preços parseada. */
    private enum PricingMode {
        STANDARD,
        BATCH,
        UNKNOWN,
        SKIP
    }

    /** Responsabilidade: transportar uma tabela de preços parseada com seu modo financeiro. */
    private record PricingTable(PricingMode mode, Map<String, PriceTriple> rows) {}

    /** Responsabilidade: transportar a interpretação de uma linha de preço da tabela oficial. */
    private record RowPricing(String code, String modality, int firstPriceCell) {}

    /** Responsabilidade: manter um trio de preços input/cache/output por modo de processamento. */
    private record PriceTriple(String name, BigDecimal input, BigDecimal cachedInput, BigDecimal output) {
        /** Calcula fallback batch como metade do preço standard quando a página não expõe tabela batch separada. */
        private PriceTriple half() {
            return new PriceTriple(
                    name,
                    input.divide(BigDecimal.valueOf(2)),
                    cachedInput.divide(BigDecimal.valueOf(2)),
                    output.divide(BigDecimal.valueOf(2)));
        }
    }
}
