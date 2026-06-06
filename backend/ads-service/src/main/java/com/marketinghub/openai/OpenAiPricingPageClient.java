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

/** Responsabilidade: consultar a página oficial de preços da OpenAI e extrair preços de modelos de texto. */
@Component
public class OpenAiPricingPageClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiPricingPageClient.class);
    private static final Pattern MONEY_PATTERN = Pattern.compile("[0-9]+(?:\\.[0-9]+)?");

    private final OpenAiProperties properties;

    /** Inicializa o cliente com as propriedades de URL/timeout da integração OpenAI. */
    public OpenAiPricingPageClient(OpenAiProperties properties) {
        this.properties = properties;
    }

    /** Busca a página oficial de preços e retorna os modelos com preços standard e batch consolidados. */
    public List<OpenAiModelPricing> fetchTextModelPricing() {
        try {
            String html = Jsoup.connect(properties.getPricingUrl())
                    .userAgent("MarketingHub/1.0 (+https://oportunidadebrasil.shop)")
                    .timeout(toRequestTimeoutMillis())
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .execute()
                    .body();
            return parseTextModelPricing(html);
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

    /** Seleciona preço exato ou preço-base mais específico para variantes datadas retornadas por /models. */
    public Optional<OpenAiModelPricing> findBestTextModelPricing(List<OpenAiModelPricing> prices, String modelCode) {
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

    /** Converte o timeout configurado para milissegundos aceitos pelo cliente HTTP de preços. */
    private int toRequestTimeoutMillis() {
        long millis = properties.getRequestTimeout().toMillis();
        return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(1, (int) millis);
    }

    /** Extrai os preços oficiais textuais, aceitando tabela legada standard/batch ou tabela atual short/long context. */
    public List<OpenAiModelPricing> parseTextModelPricing(String html) {
        if (html == null || html.isBlank()) {
            return List.of();
        }
        List<Map<String, PriceTriple>> tables = parseTextPricingTables(html);
        if (tables.isEmpty()) {
            log.warn("Nenhuma tabela de preço de texto OpenAI encontrada; operation=openai-pricing-parse source={}", properties.getPricingUrl());
            return List.of();
        }
        Map<String, PriceTriple> standard = tables.get(0);
        Map<String, PriceTriple> batch = tables.size() > 1 ? tables.get(1) : Map.of();
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

    /** Localiza tabelas com colunas de modelo, input, cached input e output e converte suas linhas em preços. */
    private List<Map<String, PriceTriple>> parseTextPricingTables(String html) {
        Document document = Jsoup.parse(html);
        List<Map<String, PriceTriple>> parsedTables = new ArrayList<>();
        for (Element table : document.select("table")) {
            String headerText = table.select("thead").text().toLowerCase(Locale.ROOT);
            if (!headerText.contains("model") || !headerText.contains("input") || !headerText.contains("output")) {
                continue;
            }
            Map<String, PriceTriple> rows = parseRows(table.select("tbody tr"));
            if (!rows.isEmpty()) {
                parsedTables.add(rows);
            }
        }
        return parsedTables;
    }

    /** Converte linhas HTML de preço em mapa por código canônico do modelo. */
    private Map<String, PriceTriple> parseRows(Elements rows) {
        Map<String, PriceTriple> tablePrices = new LinkedHashMap<>();
        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() < 4) {
                continue;
            }
            String displayName = cells.get(0).text().trim();
            String code = normalizeModelCode(displayName);
            if (!isTextModelCode(code)) {
                continue;
            }
            PriceTriple price = parsePriceTriple(cells, 1, code);
            if (price != null) {
                tablePrices.put(code, price);
            }
        }
        return tablePrices;
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

    /** Remove observações de contexto e normaliza o código do modelo para comparação e persistência. */
    private String normalizeModelCode(String value) {
        return value.replaceAll("\\s*\\(.*$", "").trim().toLowerCase(Locale.ROOT);
    }

    /** Indica se o código extraído representa modelo de texto/reasoning suportado no catálogo financeiro. */
    private boolean isTextModelCode(String code) {
        return code.startsWith("gpt-") || code.startsWith("o1") || code.startsWith("o3") || code.startsWith("o4");
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
