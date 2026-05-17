package com.marketinghub.moisclickbank.service;

import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankCollectionRequest;
import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankCollectionResponse;
import com.marketinghub.moisclickbank.dto.ClickbankDtos.ClickbankProductSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import java.time.Instant;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ClickbankCollectorService {
    private static final Logger log = LoggerFactory.getLogger(ClickbankCollectorService.class);
    private static final double PLAYWRIGHT_TIMEOUT_MS = 180_000;
    private static final int LOGIN_SUBMIT_RETRIES = 3;
    private static final int COOKIE_RETRY_ATTEMPTS = 3;
    private static final String CLICKBANK_MARKET_API_URL = "https://api.clickbank.com/v2/marketplace";
    private static final String CLICKBANK_GRAPHQL_QUERY = """
            query ($parameters: MarketplaceSearchParameters!) {
              marketplaceSearch(parameters: $parameters) {
                hits {
                  title
                  url
                  marketplaceStats {
                    category
                    gravity
                    rank
                    sellerVolume
                  }
                }
              }
            }
            """;
    private enum GraphqlSkipReason {
        JWT_ABSENT,
        JWT_EXPIRED_OR_INVALID,
        GRAPHQL_EMPTY_RESULT,
        HTTP_ERROR,
        REQUEST_ERROR
    }

    private final boolean headless;
    private final String chromiumExecutablePath;
    private final String clickbankMarketUrl;
    private final String clickbankSessionCookie;
    private final String clickbankUsername;
    private final String clickbankPassword;
    private final String clickbankTopOffersUrl;
    private final boolean logFullToken;
    private final String backendBaseUrl;
    private final String clickbankJwtSettingKey;
    private final String clickbankGraphqlUrl;
    private final String workspaceId;
    private final String defaultNiche;
    private final String defaultMarketTheme;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClickbankCollectorService(
            @Value("${collector.playwright.headless:true}") boolean headless,
            @Value("${collector.playwright.chromium-executable-path:}") String chromiumExecutablePath,
            @Value("${collector.clickbank.search-url:https://app.clickbank.com/market/search}") String clickbankMarketUrl,
            @Value("${collector.clickbank.session-cookie:}") String clickbankSessionCookie,
            @Value("${collector.clickbank.username:}") String clickbankUsername,
            @Value("${collector.clickbank.password:}") String clickbankPassword,
            @Value("${collector.clickbank.top-offers-url:https://www.clickbank.com/blog/clickbank-top-offers/}") String clickbankTopOffersUrl,
            @Value("${collector.clickbank.username-fallback:}") String clickbankUsernameFallback,
            @Value("${collector.clickbank.password-fallback:}") String clickbankPasswordFallback,
            @Value("${collector.clickbank.log-full-token:false}") boolean logFullToken,
            @Value("${collector.backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
            @Value("${collector.clickbank.jwt-setting-key:clickbank_access_token_jwt}") String clickbankJwtSettingKey,
            @Value("${collector.clickbank.graphql-url:https://accounts.clickbank.com/graphql}") String clickbankGraphqlUrl,
            @Value("${collector.backend.workspace-id:workspace-001}") String workspaceId,
            @Value("${collector.backend.niche:marketing-digital}") String defaultNiche,
            @Value("${collector.backend.market-theme:ofertas-clickbank}") String defaultMarketTheme
    ) {
        this.headless = headless;
        this.chromiumExecutablePath = chromiumExecutablePath;
        this.clickbankMarketUrl = clickbankMarketUrl;
        this.clickbankSessionCookie = clickbankSessionCookie;
        this.clickbankUsername = pickFirstNonBlank(clickbankUsername, clickbankUsernameFallback);
        this.clickbankPassword = pickFirstNonBlank(clickbankPassword, clickbankPasswordFallback);
        this.clickbankTopOffersUrl = clickbankTopOffersUrl;
        this.logFullToken = logFullToken;
        this.backendBaseUrl = backendBaseUrl;
        this.clickbankJwtSettingKey = clickbankJwtSettingKey;
        this.clickbankGraphqlUrl = clickbankGraphqlUrl;
        this.workspaceId = workspaceId;
        this.defaultNiche = defaultNiche;
        this.defaultMarketTheme = defaultMarketTheme;
    }

    public ClickbankCollectionResponse collect(ClickbankCollectionRequest request) {
        return collectThirdCycleGraphql(request);
    }

    public ClickbankCollectionResponse collectFirstCycle(ClickbankCollectionRequest request) {
        int boundedMax = request.maxProducts() <= 0 ? 10 : Math.min(request.maxProducts(), 50);

        List<ClickbankProductSnapshot> products = new ArrayList<>();
        String status = "COLLECTION_EXECUTED";
        String message = "Coleta executada a partir da página pública de Top Offers da ClickBank.";

        log.info(
                "Iniciando Ciclo 1 Clickbank (Top Offers público). maxProductsSolicitado={}, maxProductsAplicado={}, topOffersUrl={}",
                request.maxProducts(),
                boundedMax,
                clickbankTopOffersUrl
        );

        try {
            int collected = collectProductsFromTopOffersPage(boundedMax, products);
            log.info("Ciclo 1 Top Offers finalizado. produtosColetados={}", collected);
            status = "COLLECTION_EXECUTED";
            message = "Ciclo 1 executado via página pública Top Offers da ClickBank.";
        } catch (Exception ex) {
            status = "COLLECTION_ERROR";
            message = "Falha no Ciclo 1 (Top Offers): " + ex.getMessage();
            log.error("Erro no Ciclo 1 Clickbank (Top Offers).", ex);
        }
        persistCollectedProductsOnBackend(request, status, products);
        return new ClickbankCollectionResponse(status, message, products);
    }


    public ClickbankCollectionResponse collectThirdCycleGraphql(ClickbankCollectionRequest request) {
        int boundedMax = request.maxProducts() <= 0 ? 10 : Math.min(request.maxProducts(), 50);
        List<ClickbankProductSnapshot> products = new ArrayList<>();
        String status = "COLLECTION_EXECUTED";
        String message = "Ciclo 3 executado via GraphQL da ClickBank.";

        log.info("Iniciando Ciclo 3 Clickbank (GraphQL). maxProductsSolicitado={}, maxProductsAplicado={}, graphqlUrl={}",
                request.maxProducts(),
                boundedMax,
                clickbankGraphqlUrl);

        try {
            String accessToken = fetchClickbankJwtFromGeneralSettings();
            GraphqlCollectionOutcome outcome = collectProductsFromGraphql(accessToken, boundedMax, products);
            int apiCollected = outcome.collectedProducts();
            if (apiCollected == 0) {
                status = "COLLECTION_SKIPPED";
                message = "Ciclo 3 não coletou produtos via GraphQL. motivo=" + outcome.skipReason();
                log.warn("Ciclo 3 GraphQL sem dados coletados. produtosColetados={} motivo={}", apiCollected, outcome.skipReason());
            } else {
                log.info("Ciclo 3 GraphQL finalizado. produtosColetados={}", apiCollected);
            }
        } catch (Exception ex) {
            status = "COLLECTION_ERROR";
            message = "Falha no Ciclo 3 (GraphQL): " + ex.getMessage();
            log.error("Erro no Ciclo 3 Clickbank (GraphQL).", ex);
        }

        persistCollectedProductsOnBackend(request, status, products);
        return new ClickbankCollectionResponse(status, message, products);
    }

    private GraphqlCollectionOutcome collectProductsFromGraphql(String accessToken, int boundedMax, List<ClickbankProductSnapshot> products) {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("JWT Clickbank ausente para consulta GraphQL.");
            return new GraphqlCollectionOutcome(0, GraphqlSkipReason.JWT_ABSENT);
        }
        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sortField", "rank");
            parameters.put("sortDescending", false);
            parameters.put("productAttributes", List.of("shippable"));
            parameters.put("resultsPerPage", boundedMax);
            parameters.put("offset", 0);
            parameters.put("nicknameMasq", null);
            Map<String, Object> body = Map.of("query", CLICKBANK_GRAPHQL_QUERY, "variables", Map.of("parameters", parameters));
            String payload = objectMapper.writeValueAsString(body);
            boolean tokenPresent = accessToken != null && !accessToken.isBlank();
            log.info(
                    "CLICKBANK_GRAPHQL_REQUEST endpoint={} method=POST hasAuthorizationHeader={} tokenLength={} payloadPreview='{}'",
                    clickbankGraphqlUrl,
                    tokenPresent,
                    tokenPresent ? accessToken.length() : 0,
                    truncateForLog(payload, 1200)
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(clickbankGraphqlUrl))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.info("CLICKBANK_GRAPHQL_PAYLOAD_CRU status={} bodyRaw='{}'", response.statusCode(), truncateForLog(response.body(), 10_000));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                GraphqlSkipReason reason = response.statusCode() == 401 || response.statusCode() == 403
                        ? GraphqlSkipReason.JWT_EXPIRED_OR_INVALID
                        : GraphqlSkipReason.HTTP_ERROR;
                log.warn("Ciclo 3 GraphQL retornou status não 2xx. status={} motivo={}", response.statusCode(), reason);
                return new GraphqlCollectionOutcome(0, reason);
            }
            JsonNode hits = objectMapper.readTree(response.body()).path("data").path("marketplaceSearch").path("hits");
            if (!hits.isArray()) {
                return new GraphqlCollectionOutcome(0, GraphqlSkipReason.GRAPHQL_EMPTY_RESULT);
            }
            LinkedHashSet<String> dedupe = new LinkedHashSet<>();
            for (JsonNode hit : hits) {
                if (products.size() >= boundedMax) {
                    break;
                }
                String title = pickFirstNonBlank(extractProductText(hit, "title", "name"), "Produto sem título");
                String detailsUrl = normalizeClickbankUrl(extractProductText(hit, "url", "productUrl"));
                JsonNode stats = hit.path("marketplaceStats");
                String category = pickFirstNonBlank(extractProductText(stats, "category"), "N/A");
                Double gravity = extractProductNumber(stats, "gravity");
                String dedupeKey = (title + "|" + category + "|" + detailsUrl).toLowerCase();
                if (!dedupe.add(dedupeKey)) {
                    continue;
                }
                products.add(new ClickbankProductSnapshot(title, "N/A", category, detailsUrl, gravity, detailsUrl, Instant.now()));
            }
            if (products.isEmpty()) {
                return new GraphqlCollectionOutcome(0, GraphqlSkipReason.GRAPHQL_EMPTY_RESULT);
            }
            return new GraphqlCollectionOutcome(products.size(), null);
        } catch (Exception ex) {
            log.warn("Falha na coleta GraphQL Clickbank.", ex);
            return new GraphqlCollectionOutcome(0, GraphqlSkipReason.REQUEST_ERROR);
        }
    }

    private record GraphqlCollectionOutcome(int collectedProducts, GraphqlSkipReason skipReason) {}



    public ClickbankCollectionResponse collectSecondCycleFromBackend(ClickbankCollectionRequest request) {
        int boundedMax = request.maxProducts() <= 0 ? 10 : Math.min(request.maxProducts(), 50);
        List<ClickbankProductSnapshot> products = new ArrayList<>();
        String status = "COLLECTION_EXECUTED";
        String message = "Ciclo 2 executado a partir dos produtos persistidos no backend.";

        List<ClickbankProductSnapshot> baseProducts = fetchFirstCycleProductsFromBackend(boundedMax);
        for (ClickbankProductSnapshot base : baseProducts) {
            if (products.size() >= boundedMax) {
                break;
            }
            products.add(collectSalesPageFromProduct(base));
        }
        persistCollectedProductsOnBackend(request, status, products);
        publishSalesPagesToLibrary(products);
        return new ClickbankCollectionResponse(status, message, products);
    }

    private void publishSalesPagesToLibrary(List<ClickbankProductSnapshot> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<Map<String, Object>> urls = new ArrayList<>();
        for (ClickbankProductSnapshot product : products) {
            String salesPageUrl = coalesceNotBlank(product.salesPageUrl(), product.detailsUrl());
            if (salesPageUrl == null || salesPageUrl.isBlank()) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("url", salesPageUrl);
            item.put("source", "CLICKBANK");
            item.put("workspaceId", workspaceId);
            item.put("title", product.title());
            item.put("capturedAt", Instant.now().toString());
            urls.add(item);
        }
        if (urls.isEmpty()) {
            log.info("Ciclo 2: nenhuma URL elegível para ingestão na biblioteca de sales pages.");
            return;
        }
        String endpoint = backendBaseUrl + "/api/mois/sales-library/urls:ingest";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("workspaceId", workspaceId);
            body.put("source", "CLICKBANK");
            body.put("urls", urls);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Ciclo 2: ingestão de sales pages enviada com sucesso. endpoint={}, totalUrls={}", endpoint, urls.size());
            } else {
                log.warn("Ciclo 2: backend rejeitou ingestão de sales pages. endpoint={}, status={}, body={}",
                        endpoint,
                        response.statusCode(),
                        truncate(response.body(), 500));
            }
        } catch (Exception ex) {
            log.warn("Ciclo 2: falha ao enviar URLs para biblioteca de sales pages. endpoint={}", endpoint, ex);
        }
    }

    private List<ClickbankProductSnapshot> fetchFirstCycleProductsFromBackend(int maxProducts) {
        List<ClickbankProductSnapshot> products = new ArrayList<>();
        String endpoint = backendBaseUrl + "/api/v1/mois/clickbase/products?workspaceId=" + workspaceId + "&limit=" + Math.max(1, Math.min(maxProducts, 100));
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Ciclo 2: falha ao obter produtos base do backend. status={}, endpoint={}", response.statusCode(), endpoint);
                return products;
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode items = firstArray(root, "items", "products", "content", "results");
            if (items == null || !items.isArray()) {
                return products;
            }
            for (JsonNode item : items) {
                String title = pickFirstNonBlank(extractProductText(item, "title", "name", "productName"), "Produto sem título");
                String detailsUrl = pickFirstNonBlank(extractProductText(item, "detailsUrl", "productUrl", "url"), clickbankTopOffersUrl);
                String salesPageUrl = pickFirstNonBlank(extractProductText(item, "salesPageUrl", "pageUrl"), detailsUrl);
                products.add(new ClickbankProductSnapshot(
                        title,
                        pickFirstNonBlank(extractProductText(item, "productNickname", "nickname", "rating"), "N/A"),
                        pickFirstNonBlank(extractProductText(item, "productCategory", "category", "commission"), "N/A"),
                        detailsUrl,
                        extractProductNumber(item, "temperature", "clickbankTemperature"),
                        salesPageUrl,
                        Instant.now()));
            }
        } catch (Exception ex) {
            log.warn("Ciclo 2: erro ao obter produtos do backend.", ex);
        }
        return products;
    }

    private ClickbankProductSnapshot collectSalesPageFromProduct(ClickbankProductSnapshot base) {
        String normalizedDetailsUrl = coalesceNotBlank(base.detailsUrl(), clickbankTopOffersUrl);
        String salesPageUrl = coalesceNotBlank(base.salesPageUrl(), normalizedDetailsUrl);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedDetailsUrl))
                    .header("accept", "text/html")
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 400 && response.uri() != null) {
                salesPageUrl = response.uri().toString();
            }
        } catch (Exception ex) {
            log.warn("Ciclo 2: falha ao resolver página de vendas para detailsUrl={}", normalizedDetailsUrl);
        }
        return new ClickbankProductSnapshot(
                base.title(),
                base.rating(),
                base.commission(),
                normalizedDetailsUrl,
                base.temperature(),
                salesPageUrl,
                Instant.now()
        );
    }
    private int collectProductsFromTopOffersPage(int boundedMax, List<ClickbankProductSnapshot> products) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(clickbankTopOffersUrl))
                    .header("accept", "text/html")
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Top Offers retornou status " + response.statusCode());
            }
            String html = response.body();
            java.util.regex.Pattern productPattern = java.util.regex.Pattern.compile(
                    "<h[1-3][^>]*>\\s*(?:\\d+\\)\\s*)?<a[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]*>(.*?)</a>\\s*</h[1-3]>(.*?)(?=<h[1-3][^>]*>\\s*(?:\\d+\\)\\s*)?<a|$)",
                    java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
            );
            java.util.regex.Pattern nicknamePattern = java.util.regex.Pattern.compile(
                    "Nickname:\\s*(.*?)\\s*(?=Category:|$)",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Pattern categoryPattern = java.util.regex.Pattern.compile(
                    "Category:\\s*(.*?)\\s*(?=Check out their landing page here\\.?|$)",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Pattern landingPagePattern = java.util.regex.Pattern.compile(
                    "<a[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]*>\\s*Check out their landing page here\\.?\\s*</a>",
                    java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
            );
            java.util.regex.Matcher matcher = productPattern.matcher(html);
            LinkedHashSet<String> dedupe = new LinkedHashSet<>();
            while (matcher.find() && products.size() < boundedMax) {
                String href = matcher.group(1).trim();
                String title = stripHtml(matcher.group(2));
                String sectionHtml = matcher.group(3);
                String sectionText = stripHtml(sectionHtml);
                if (title.isBlank() || href.isBlank()) {
                    continue;
                }

                String nickname = "N/A";
                java.util.regex.Matcher nicknameMatcher = nicknamePattern.matcher(sectionText);
                if (nicknameMatcher.find()) {
                    nickname = nicknameMatcher.group(1).trim();
                }

                String category = "N/A";
                java.util.regex.Matcher categoryMatcher = categoryPattern.matcher(sectionText);
                if (categoryMatcher.find()) {
                    category = categoryMatcher.group(1).trim();
                }

                String detailsUrl = normalizeClickbankUrl(href);
                String salesPageUrl = detailsUrl;
                java.util.regex.Matcher landingPageMatcher = landingPagePattern.matcher(sectionHtml);
                if (landingPageMatcher.find()) {
                    salesPageUrl = normalizeClickbankUrl(landingPageMatcher.group(1));
                }
                String dedupeKey = title.toLowerCase() + "|" + nickname.toLowerCase() + "|" + detailsUrl.toLowerCase();
                if (!dedupe.add(dedupeKey)) {
                    continue;
                }
                products.add(new ClickbankProductSnapshot(title, nickname, category, detailsUrl, null, salesPageUrl, Instant.now()));
            }
            if (products.isEmpty()) {
                log.warn("Parser Top Offers não encontrou blocos de produtos. url={}", clickbankTopOffersUrl);
            }
            return products.size();
        } catch (Exception ex) {
            throw new RuntimeException("Não foi possível coletar top offers públicos.", ex);
        }
    }

    private String stripHtml(String htmlFragment) {
        if (htmlFragment == null) {
            return "";
        }
        return htmlFragment
                .replaceAll("<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeClickbankUrl(String href) {
        if (href == null || href.isBlank()) {
            return clickbankTopOffersUrl;
        }
        return href.startsWith("http") ? href : "https://www.clickbank.com" + href;
    }

    private void persistCollectedProductsOnBackend(
            ClickbankCollectionRequest request,
            String status,
            List<ClickbankProductSnapshot> products
    ) {
        String jobId = "clickbank-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
        String endpoint = backendBaseUrl + "/api/v1/mois/persistence/collection-jobs/" + jobId;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("job", Map.of(
                    "jobId", jobId,
                    "workspaceId", workspaceId,
                    "niche", defaultNiche,
                    "marketTheme", defaultMarketTheme,
                    "status", status,
                    "timeWindow", "LAST_7_DAYS",
                    "limitPerSource", Math.max(products.size(), request.maxProducts()),
                    "minSuccessScore", 0,
                    "sources", List.of("CLICKBANK"),
                    "createdAt", Instant.now().toString()
            ));
            payload.put("references", toCollectedReferences(jobId, request.source(), products));
            payload.put("lineageByReferenceId", Map.of());
            payload.put("runtime", Map.of("retries", 0, "latencyMs", 0, "finishedAt", Instant.now().toString()));
            payload.put("sourceOps", List.of());

            HttpRequest persistenceRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(persistenceRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Falha ao persistir coleta Clickbank no backend. status={}, endpoint={}, body={}",
                        response.statusCode(), endpoint, truncateForLog(response.body(), 600));
                return;
            }
            log.info("Coleta Clickbank persistida no backend com sucesso. endpoint={}, jobId={}, produtos={}",
                    endpoint, jobId, products.size());
        } catch (Exception ex) {
            log.warn("Falha ao persistir produtos coletados no backend.", ex);
        }
    }

    private List<Map<String, Object>> toCollectedReferences(String jobId, String source, List<ClickbankProductSnapshot> products) {
        List<Map<String, Object>> references = new ArrayList<>();
        int position = 1;
        for (ClickbankProductSnapshot product : products) {
            Map<String, String> rawMetadata = new HashMap<>();
            rawMetadata.put("productName", product.title());
            rawMetadata.put("productUrl", product.detailsUrl());
            rawMetadata.put("salesPageUrl", coalesceNotBlank(product.salesPageUrl(), product.detailsUrl()));
            rawMetadata.put("productNickname", product.rating());
            rawMetadata.put("productCategory", product.commission());
            if (product.temperature() != null) {
                rawMetadata.put("clickbankTemperature", String.valueOf(product.temperature()));
            }
            rawMetadata.put("producerName", null);

            Map<String, Object> reference = new HashMap<>();
            reference.put("referenceId", "clickbank-" + position + "-" + UUID.randomUUID().toString().substring(0, 8));
            reference.put("jobId", jobId);
            reference.put("source", "CLICKBANK");
            reference.put("title", product.title());
            reference.put("url", coalesceNotBlank(product.salesPageUrl(), product.detailsUrl()));
            reference.put("niche", defaultNiche);
            reference.put("status", "COLLECTED");
            reference.put("favorite", false);
            reference.put("importedReferenceId", null);
            reference.put("successScore", 80);
            reference.put("successSignal", "CLICKBANK_TRENDING");
            reference.put("confidenceLevel", "MEDIUM");
            reference.put("rankingPosition", position);
            reference.put("engagementRelative", 0.0);
            reference.put("recurrenceScore", 0.0);
            reference.put("evidenceScore", 0.0);
            reference.put("collectedAt", product.collectedAt() == null ? Instant.now().toString() : product.collectedAt().toString());
            reference.put("rawMetadata", rawMetadata);
            references.add(reference);
            position++;
        }
        return references;
    }

    private String fetchClickbankJwtFromGeneralSettings() {
        try {
            String endpoint = backendBaseUrl + "/api/settings/" + clickbankJwtSettingKey;
            log.info("Buscando JWT Clickbank nas configurações gerais. endpoint={}, settingKey={}", endpoint, clickbankJwtSettingKey);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .GET()
                    .header("accept", "application/json")
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Falha ao buscar JWT em configurações gerais. status={}, endpoint={}", response.statusCode(), endpoint);
                return "";
            }
            JsonNode root = objectMapper.readTree(response.body());
            String token = root.path("value").asText("");
            log.info(
                    "JWT Clickbank carregado das configurações gerais. endpoint={}, tokenPresente={}, tokenTamanho={}",
                    endpoint,
                    token != null && !token.isBlank(),
                    token == null ? 0 : token.length()
            );
            return token;
        } catch (Exception ex) {
            log.warn("Erro ao buscar JWT da Clickbank em configurações gerais.", ex);
            return "";
        }
    }

    private int collectProductsViaMarketApi(String accessToken, int boundedMax, List<ClickbankProductSnapshot> products) {
        try {
            String payload = objectMapper.writeValueAsString(java.util.Map.of(
                    "page", 1,
                    "rows", boundedMax,
                    "userLocale", "PT_BR",
                    "name", "hottest"
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLICKBANK_MARKET_API_URL))
                    .header("accept", "application/json, text/plain, */*")
                    .header("content-type", "application/json")
                    .header("authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            log.info("Resposta API Clickbank recebida. status={}, bodyPreview='{}'", response.statusCode(), truncateForLog(body, 1200));
            log.info("CLICKBANK_FETCH_RESPOSTA_CRUA status={} bodyRaw='{}'",
                    response.statusCode(),
                    truncateForLog(body, 10_000));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return 0;
            }
            JsonNode data = objectMapper.readTree(body);
            JsonNode productsNode = firstArray(data, "products", "items", "content", "results");
            if (productsNode == null || !productsNode.isArray()) {
                log.warn("Resposta API Clickbank sem array de produtos. keysRaiz={}", previewFieldNames(data));
                return 0;
            }
            log.info("Resposta API Clickbank: totalItensArray={}, chavesPrimeiroItem={}",
                    productsNode.size(),
                    productsNode.size() > 0 ? previewFieldNames(productsNode.get(0)) : "[]");
            for (JsonNode item : productsNode) {
                if (products.size() >= boundedMax) break;
                String title = extractProductText(item, "name", "productName", "title");
                String salesPageUrl = extractProductText(item, "salesPageUrl", "pageUrl", "page_url", "link");
                String url = extractProductText(item, "checkoutUrl", "productUrl", "url", "link");
                if (url == null || url.isBlank()) {
                    url = clickbankMarketUrl;
                }
                if (title == null || title.isBlank()) {
                    log.warn("Item Clickbank sem título mapeável. chavesItem={}, itemPreview={}",
                            previewFieldNames(item),
                            truncateForLog(item.toString(), 600));
                }
                Double temperature = extractProductNumber(item, "temperature", "temp", "hotness");
                products.add(new ClickbankProductSnapshot(
                        title == null || title.isBlank() ? "Produto sem título" : title,
                        "N/A",
                        "N/A",
                        url,
                        temperature,
                        salesPageUrl,
                        Instant.now()));
            }
            return products.size();
        } catch (Exception ex) {
            log.warn("Falha na coleta de produtos via API Clickbank.", ex);
            return 0;
        }
    }

    /* legacy Playwright flow kept below for rollback safety */
    private ClickbankCollectionResponse legacyCollect(ClickbankCollectionRequest request) {
        int boundedMax = request.maxProducts() <= 0 ? 10 : Math.min(request.maxProducts(), 50);
        List<ClickbankProductSnapshot> products = new ArrayList<>();
        String status = "COLLECTION_EXECUTED";
        String message = "Coleta executada com Playwright em modo headless=" + headless + ".";
        boolean hasSessionCookie = clickbankSessionCookie != null && !clickbankSessionCookie.isBlank();
        boolean hasCredentials = clickbankUsername != null && !clickbankUsername.isBlank()
                && clickbankPassword != null && !clickbankPassword.isBlank();
        try (Playwright playwright = Playwright.create()) {
            String browserPath = playwright.chromium().executablePath();
            List<String> launchArgs = List.of("--no-sandbox", "--disable-dev-shm-usage");
            log.info("Playwright inicializado. Chromium executablePath='{}', launchArgs={}", browserPath, launchArgs);

            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setArgs(launchArgs);
            if (chromiumExecutablePath != null && !chromiumExecutablePath.isBlank()) {
                launchOptions.setExecutablePath(Path.of(chromiumExecutablePath));
                log.info("Usando Chromium com executablePath explícito: '{}'", chromiumExecutablePath);
            }

            Browser browser = playwright.chromium().launch(launchOptions);
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            authenticateClickbank(context, page, hasSessionCookie, hasCredentials);
            String clickbankAccessToken = tryExtractAccessToken(page);
            logTokenDiagnostics(clickbankAccessToken);

            log.info("Navegando para URL de mercado Clickbank: {}", clickbankMarketUrl);
            page.navigate(clickbankMarketUrl, new Page.NavigateOptions()
                    .setTimeout(120_000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForURL("**/market/**", new Page.WaitForURLOptions().setTimeout(60_000));
            page.locator("#root").first().waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(60_000));
            page.waitForTimeout(1_500);

            int cardsCount = page.locator("a[href*='/market/products/']").count();
            int maxToRead = Math.min(cardsCount, boundedMax);
            log.info("Página carregada. cardsEncontrados={}, cardsProcessados={}", cardsCount, maxToRead);
            if (cardsCount == 0) {
                log.warn("Nenhum card de produto encontrado na página de mercado. url='{}', htmlSnapshot='{}'",
                        page.url(),
                        captureHtmlSnapshot(page));
                int apiCollected = collectProductsViaMarketApi(page, clickbankAccessToken, boundedMax, products);
                log.info("Fallback API Clickbank finalizado. produtosColetadosViaApi={}", apiCollected);
            }
            if (products.isEmpty()) {
                for (int i = 0; i < maxToRead; i++) {
                String title = page.locator("a[href*='/market/products/']").nth(i).innerText();
                String detailsUrl = page.locator("a[href*='/market/products/']").nth(i).getAttribute("href");
                if (detailsUrl != null && detailsUrl.startsWith("/")) {
                    detailsUrl = "https://app.clickbank.com" + detailsUrl;
                }
                products.add(new ClickbankProductSnapshot(
                        title == null || title.isBlank() ? "Produto sem título" : title,
                        "N/A",
                        "N/A",
                        detailsUrl == null ? clickbankMarketUrl : detailsUrl,
                        null,
                        detailsUrl == null ? clickbankMarketUrl : detailsUrl,
                        Instant.now()
                ));
                }
            }

            log.info("Coleta Clickbank finalizada com sucesso. produtosColetados={}", products.size());
            context.close();
            browser.close();
        } catch (Exception ex) {
            status = "COLLECTION_ERROR";
            message = "Falha na coleta Playwright: " + ex.getMessage();
            log.error(
                    "Erro na coleta Clickbank via Playwright. headless={}, hasSessionCookie={}, hasCredentials={}, marketUrl='{}'",
                    headless,
                    hasSessionCookie,
                    hasCredentials,
                    clickbankMarketUrl,
                    ex
            );
        }

        return new ClickbankCollectionResponse(status, message, products);
    }


    private String captureHtmlSnapshot(Page page) {
        String html = page.content();
        if (html == null || html.isBlank()) {
            return "HTML vazio";
        }
        String normalized = html.replaceAll("\\s+", " ").trim();
        int maxLength = 8_000;
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...[TRUNCATED]";
    }


    private int collectProductsViaMarketApi(Page page, String accessToken, int boundedMax, List<ClickbankProductSnapshot> products) {
        try {
            log.info("Fallback API Clickbank: iniciando chamada. hasAccessToken={}, tokenPreview='{}'",
                    accessToken != null && !accessToken.isBlank(),
                    maskToken(accessToken));
            String response = page.evaluate("""
                    async ({ apiUrl, rows, accessToken }) => {
                      const payload = {
                        page: 1,
                        rows,
                        userLocale: 'PT_BR',
                        name: 'hottest'
                      };
                      const res = await fetch(apiUrl, {
                        method: 'POST',
                        credentials: 'include',
                        headers: {
                          'accept': 'application/json, text/plain, */*',
                          ...(accessToken ? { 'authorization': `Bearer ${accessToken}` } : {}),
                          'content-type': 'application/json'
                        },
                        body: JSON.stringify(payload)
                      });
                      const text = await res.text();
                      return JSON.stringify({ status: res.status, body: text });
                    }
                    """, java.util.Map.of("apiUrl", CLICKBANK_MARKET_API_URL, "rows", boundedMax, "accessToken", accessToken == null ? "" : accessToken)).toString();

            JsonNode root = objectMapper.readTree(response);
            int status = root.path("status").asInt();
            String body = root.path("body").asText("{}");
            log.info("Resposta fallback API Clickbank recebida. status={}, bodyPreview='{}'", status, truncateForLog(body, 1200));
            if (status < 200 || status >= 300) {
                return 0;
            }
            JsonNode data = objectMapper.readTree(body);
            JsonNode productsNode = firstArray(data, "products", "items", "content", "results");
            if (productsNode == null || !productsNode.isArray()) {
                return 0;
            }
            for (JsonNode item : productsNode) {
                if (products.size() >= boundedMax) {
                    break;
                }
                String title = firstText(item, "name", "productName", "title");
                String url = firstText(item, "checkoutUrl", "productUrl", "url", "link");
                if (url == null || url.isBlank()) {
                    url = clickbankMarketUrl;
                }
                products.add(new ClickbankProductSnapshot(
                        title == null || title.isBlank() ? "Produto sem título" : title,
                        "N/A",
                        "N/A",
                        url,
                        null,
                        url,
                        Instant.now()
                ));
            }
            return products.size();
        } catch (Exception ex) {
            log.warn("Falha no fallback de coleta via API Clickbank.", ex);
            return 0;
        }
    }

    private String tryExtractAccessToken(Page page) {
        log.info("Token Clickbank: iniciando extração pós-login (localStorage/sessionStorage/cookies).");
        try {
            Object tokenObj = page.evaluate("""
                    () => {
                      const jwtRegex = /eyJ[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+/;
                      const stores = [window.localStorage, window.sessionStorage];
                      for (const storage of stores) {
                        for (let i = 0; i < storage.length; i++) {
                          const key = storage.key(i);
                          const value = storage.getItem(key);
                          if (!value) continue;
                          if (jwtRegex.test(value)) return value.match(jwtRegex)[0];
                        }
                      }
                      const cookieMatch = document.cookie.match(jwtRegex);
                      return cookieMatch ? cookieMatch[0] : '';
                    }
                    """);
            String token = tokenObj == null ? "" : tokenObj.toString();
            if (token.isBlank()) {
                log.warn("Token Clickbank: nenhum token JWT encontrado no contexto da página. url='{}'", page.url());
                return "";
            }
            log.info("Token Clickbank: token capturado com sucesso.");
            return token;
        } catch (Exception ex) {
            log.warn("Token Clickbank: falha ao extrair token pós-login. url='{}'", page.url(), ex);
            return "";
        }
    }

    private void logTokenDiagnostics(String token) {
        boolean hasToken = token != null && !token.isBlank();
        log.info("Token Clickbank: hasToken={}, length={}", hasToken, hasToken ? token.length() : 0);
        if (!hasToken) {
            return;
        }
        if (logFullToken) {
            log.warn("Token Clickbank (FULL - modo diagnóstico): {}", token);
        } else {
            log.info("Token Clickbank (masked): {}", maskToken(token));
        }
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        if (token.length() <= 20) {
            return "***";
        }
        return token.substring(0, 12) + "...<masked>..." + token.substring(token.length() - 8);
    }

    private JsonNode firstArray(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode child = node.path(key);
            if (child.isArray()) {
                return child;
            }
        }
        Iterator<JsonNode> it = node.elements();
        while (it.hasNext()) {
            JsonNode child = it.next();
            if (child.isArray()) {
                return child;
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode child = node.path(key);
            if (child.isTextual() && !child.asText().isBlank()) {
                return child.asText();
            }
        }
        return null;
    }


    private Double extractProductNumber(JsonNode item, String... keys) {
        if (item == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            JsonNode node = findNode(item, key);
            if (node == null || node.isMissingNode() || node.isNull()) {
                continue;
            }
            if (node.isNumber()) {
                return node.asDouble();
            }
            if (node.isTextual()) {
                try {
                    return Double.parseDouble(node.asText().trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private String extractProductText(JsonNode item, String... keys) {
        String direct = firstText(item, keys);
        if (direct != null && !direct.isBlank()) {
            return direct;
        }
        JsonNode productNode = item.path("product");
        if (!productNode.isMissingNode() && !productNode.isNull()) {
            String nested = firstText(productNode, keys);
            if (nested != null && !nested.isBlank()) {
                return nested;
            }
        }
        JsonNode sourceObjectNode = item.path("sourceObject");
        if (!sourceObjectNode.isMissingNode() && !sourceObjectNode.isNull()) {
            String sourceObject = firstText(sourceObjectNode, keys);
            if (sourceObject != null && !sourceObject.isBlank()) {
                return sourceObject;
            }
        }
        return null;
    }

    private String previewFieldNames(JsonNode node) {
        if (node == null || !node.isObject()) {
            return "[]";
        }
        List<String> keys = new ArrayList<>();
        node.fieldNames().forEachRemaining(keys::add);
        if (keys.size() > 12) {
            return keys.subList(0, 12) + "...";
        }
        return keys.toString();
    }


    private JsonNode findNode(JsonNode node, String key) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode direct = node.path(key);
        if (!direct.isMissingNode() && !direct.isNull()) {
            return direct;
        }
        return null;
    }

    private String coalesceNotBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback == null ? "" : fallback;
    }

    private String truncateForLog(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...[TRUNCATED]";
    }

    private String pickFirstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return "";
    }

    private void logPlaywrightRuntimeDiagnostics() {
        String skipDownload = System.getenv("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD");
        String browsersPath = System.getenv("PLAYWRIGHT_BROWSERS_PATH");
        String home = System.getenv("HOME");
        log.info(
                "Diagnóstico Playwright runtime: skipBrowserDownload='{}', browsersPath='{}', home='{}', configuredExecutablePath='{}'",
                skipDownload,
                browsersPath,
                home,
                chromiumExecutablePath
        );
    }

    private void performLogin(Page page) {
        log.info("Iniciando login Clickbank por credenciais (username preenchido={}).",
                clickbankUsername != null && !clickbankUsername.isBlank());
        page.navigate("https://sso.clickbank.com/login", new Page.NavigateOptions()
                .setTimeout(PLAYWRIGHT_TIMEOUT_MS)
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        page.setDefaultTimeout(PLAYWRIGHT_TIMEOUT_MS);
        closeCookieOrConsentOverlays(page);
        waitTransientOverlaysToHide(page);

        String emailSelector = "input#username, input[name='username'], input[autocomplete*='username'], input[type='text'][name='username'], input[type='email'], input[name='email']";
        String passwordSelector = "input[type='password'], input[name='password']";
        String submitSelector = "button#submit-button, button[data-test-id='login-submit'], button[name='submit'][type='submit'], button[type='submit']";

        try {
            long emailCount = page.locator(emailSelector).count();
            long passwordCount = page.locator(passwordSelector).count();
            long submitCount = page.locator(submitSelector).count();
            log.info("Diagnóstico de seletores Clickbank login: emailMatches={}, passwordMatches={}, submitMatches={}, url='{}'",
                    emailCount, passwordCount, submitCount, page.url());

            page.locator(emailSelector).first().fill(clickbankUsername);
            page.locator(passwordSelector).first().fill(clickbankPassword);
            closeCookieOrConsentOverlays(page);
            submitLoginWithRetry(page, submitSelector, passwordSelector);
            ensureLoginProgressed(page, passwordSelector);
            log.info("Login Clickbank submetido. URL atual após espera='{}'.", page.url());
        } catch (Exception ex) {
            String inputSnapshot = page.locator("input")
                    .evaluateAll("els => els.slice(0, 10).map((el, idx) => `${idx}:type=${el.getAttribute('type') || ''};name=${el.getAttribute('name') || ''};id=${el.getAttribute('id') || ''};placeholder=${el.getAttribute('placeholder') || ''}`).join(' | ')")
                    .toString();
            log.error("Falha ao preencher/submeter login Clickbank. url='{}', inputSnapshot='{}'", page.url(), inputSnapshot, ex);
            throw ex;
        }
    }

    private void authenticateClickbank(BrowserContext context, Page page, boolean hasSessionCookie, boolean hasCredentials) {
        if (hasCredentials) {
            log.info("Usando autenticação por login/senha Clickbank.");
            try {
                performLogin(page);
                return;
            } catch (Exception ex) {
                if (hasSessionCookie) {
                    log.warn("Login por credenciais falhou; aplicando fallback para cookie de sessão Clickbank.", ex);
                    applySessionCookie(context);
                    return;
                }
                throw ex;
            }
        }

        if (hasSessionCookie) {
            log.info("Usando autenticação por cookie de sessão Clickbank.");
            applySessionCookie(context);
        }
    }

    private void applySessionCookie(BrowserContext context) {
        context.addCookies(List.of(new Cookie("clickbank_session", clickbankSessionCookie)
                .setDomain(".clickbank.com")
                .setPath("/")
                .setHttpOnly(true)
                .setSecure(true)));
    }

    private void submitLoginWithRetry(Page page, String submitSelector, String passwordSelector) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= LOGIN_SUBMIT_RETRIES; attempt++) {
            try {
                closeCookieOrConsentOverlays(page);
                waitTransientOverlaysToHide(page);
                page.locator(submitSelector).first().scrollIntoViewIfNeeded();
                try {
                    page.locator(submitSelector).first().click();
                } catch (RuntimeException clickFailure) {
                    log.warn("Click padrão no submit falhou na tentativa {}. Aplicando fallback com force=true. motivo={}",
                            attempt, clickFailure.getMessage());
                    closeCookieOrConsentOverlays(page);
                    page.locator(submitSelector).first()
                            .click(new com.microsoft.playwright.Locator.ClickOptions().setForce(true).setTimeout(5_000));
                }
                page.waitForTimeout(2_000);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                log.warn("Tentativa {} de submit no login Clickbank falhou: {}", attempt, ex.getMessage());
                closeCookieOrConsentOverlays(page);
                if (attempt == LOGIN_SUBMIT_RETRIES) {
                    break;
                }
                page.waitForTimeout(500L * attempt);
            }
        }

        log.warn("Fallback de submit por ENTER no campo senha.");
        page.locator(passwordSelector).first().press("Enter");
        page.waitForTimeout(2_000);
        waitTransientOverlaysToHide(page);
        if (lastError != null) {
            log.info("Último erro antes do fallback ENTER: {}", lastError.getMessage());
        }
    }

    private void closeCookieOrConsentOverlays(Page page) {
        String cookieAcceptSelectors = String.join(", ",
                "#clickbank-cookie-policy button",
                "clickbank-cookie-policy button",
                "button[data-testid*='cookie']",
                "button[id*='cookie']",
                "button:has-text('Accept all cookies')",
                "button:has-text('Accept all')",
                "button:has-text('Allow all')",
                "button:has-text('I agree')",
                "button:has-text('Aceitar')",
                "button:has-text('Aceitar tudo')",
                "button:has-text('Concordo')"
        );
        for (int attempt = 1; attempt <= COOKIE_RETRY_ATTEMPTS; attempt++) {
            try {
                if (page.locator(cookieAcceptSelectors).first().isVisible()) {
                    page.locator(cookieAcceptSelectors)
                            .first()
                            .click(new com.microsoft.playwright.Locator.ClickOptions().setTimeout(3_000));
                    log.info("Banner de cookies detectado e aceito automaticamente. tentativa={}", attempt);
                    page.waitForTimeout(300L * attempt);
                }
            } catch (Exception ignored) {
                log.debug("Nenhum botão de aceite de cookie acionável encontrado na tentativa {}.", attempt);
            }
        }

        try {
            page.locator("#clickbank-cookie-policy")
                    .waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(5_000));
        } catch (Exception ignored) {
            log.warn("Overlay de cookie não ocultou no tempo esperado; aplicando fallback por JS. url='{}'", page.url());
            hideOverlayWithJavascript(page, "#clickbank-cookie-policy");
            hideOverlayWithJavascript(page, "clickbank-cookie-policy");
            hideOverlayWithJavascript(page, ".clickbank-cookie-policy-container");
        }
    }


    private void hideOverlayWithJavascript(Page page, String selector) {
        try {
            page.evaluate("""
                    (sel) => {
                      const el = document.querySelector(sel);
                      if (!el) return;
                      el.style.display = 'none';
                      el.style.pointerEvents = 'none';
                      if (el.parentNode) {
                        el.parentNode.removeChild(el);
                      }
                    }
                    """, selector);
            log.info("Overlay '{}' ocultado por fallback JS.", selector);
        } catch (Exception ex) {
            log.debug("Falha ao ocultar overlay '{}' por JS: {}", selector, ex.getMessage());
        }
    }

    private void ensureLoginProgressed(Page page, String passwordSelector) {
        String currentUrl = page.url();
        if (!currentUrl.contains("sso.clickbank.com/login")) {
            return;
        }
        for (int attempt = 1; attempt <= LOGIN_SUBMIT_RETRIES && page.url().contains("sso.clickbank.com/login"); attempt++) {
            log.warn("Login ainda na SSO após submit; forçando nova tentativa de progressão. tentativa={}", attempt);
            closeCookieOrConsentOverlays(page);
            page.locator(passwordSelector).first().press("Enter");
            page.waitForTimeout(1_000L * attempt);
            closeCookieOrConsentOverlays(page);
        }
        if (page.url().contains("sso.clickbank.com/login")) {
            throw new IllegalStateException("Login Clickbank não avançou após tentativas de remover banner/consentimento.");
        }
    }

    private void waitTransientOverlaysToHide(Page page) {
        waitOverlayHidden(page, "#loader");
        waitOverlayHidden(page, "#clickbank-cookie-policy");
        waitOverlayHidden(page, "clickbank-cookie-policy");
    }

    private void waitOverlayHidden(Page page, String selector) {
        try {
            page.locator(selector)
                    .waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(8_000));
        } catch (Exception ignored) {
            log.debug("Overlay '{}' ainda visível após timeout de espera.", selector);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
