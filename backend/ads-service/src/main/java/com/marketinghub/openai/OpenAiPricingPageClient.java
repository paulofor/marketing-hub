package com.marketinghub.openai;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: consultar a página oficial de preços da OpenAI e extrair preços de modelos de texto. */
@Component
public class OpenAiPricingPageClient {
    private static final Logger log = LoggerFactory.getLogger(OpenAiPricingPageClient.class);
    private static final Pattern MONEY_PATTERN = Pattern.compile("[0-9]+(?:\\.[0-9]+)?");

    private final WebClient.Builder webClientBuilder;
    private final OpenAiProperties properties;

    /** Inicializa o cliente com WebClient e propriedades de URL/timeout da integração OpenAI. */
    public OpenAiPricingPageClient(WebClient.Builder webClientBuilder, OpenAiProperties properties) {
        this.webClientBuilder = webClientBuilder;
        this.properties = properties;
    }

    /** Busca a página oficial de preços e retorna os modelos com preços standard e batch consolidados. */
    public List<OpenAiModelPricing> fetchTextModelPricing() {
        String html = webClientBuilder.build().get().uri(properties.getPricingUrl()).retrieve().bodyToMono(String.class).block();
        return parseTextModelPricing(html);
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
