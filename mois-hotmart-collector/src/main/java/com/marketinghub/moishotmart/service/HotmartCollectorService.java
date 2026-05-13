package com.marketinghub.moishotmart.service;

import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionRequest;
import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionResponse;
import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartProductSnapshot;
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
public class HotmartCollectorService {
    private static final Logger log = LoggerFactory.getLogger(HotmartCollectorService.class);
    private static final double PLAYWRIGHT_TIMEOUT_MS = 180_000;
    private static final int LOGIN_SUBMIT_RETRIES = 3;
    private static final int COOKIE_RETRY_ATTEMPTS = 3;
    private static final String HOTMART_MARKET_API_URL = "https://api-affiliation-market.hotmart.com/v2/market/search";

    private final boolean headless;
    private final String chromiumExecutablePath;
    private final String hotmartMarketUrl;
    private final String hotmartSessionCookie;
    private final String hotmartUsername;
    private final String hotmartPassword;
    private final boolean logFullToken;
    private final String backendBaseUrl;
    private final String hotmartJwtSettingKey;
    private final String workspaceId;
    private final String defaultNiche;
    private final String defaultMarketTheme;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HotmartCollectorService(
            @Value("${collector.playwright.headless:true}") boolean headless,
            @Value("${collector.playwright.chromium-executable-path:}") String chromiumExecutablePath,
            @Value("${collector.hotmart.search-url:https://app.hotmart.com/market/search}") String hotmartMarketUrl,
            @Value("${collector.hotmart.session-cookie:}") String hotmartSessionCookie,
            @Value("${collector.hotmart.username:}") String hotmartUsername,
            @Value("${collector.hotmart.password:}") String hotmartPassword,
            @Value("${collector.hotmart.username-fallback:}") String hotmartUsernameFallback,
            @Value("${collector.hotmart.password-fallback:}") String hotmartPasswordFallback,
            @Value("${collector.hotmart.log-full-token:false}") boolean logFullToken,
            @Value("${collector.backend.base-url:http://191.252.181.168:8000}") String backendBaseUrl,
            @Value("${collector.hotmart.jwt-setting-key:hotmart_access_token_jwt}") String hotmartJwtSettingKey,
            @Value("${collector.backend.workspace-id:workspace-001}") String workspaceId,
            @Value("${collector.backend.niche:marketing-digital}") String defaultNiche,
            @Value("${collector.backend.market-theme:ofertas-hotmart}") String defaultMarketTheme
    ) {
        this.headless = headless;
        this.chromiumExecutablePath = chromiumExecutablePath;
        this.hotmartMarketUrl = hotmartMarketUrl;
        this.hotmartSessionCookie = hotmartSessionCookie;
        this.hotmartUsername = pickFirstNonBlank(hotmartUsername, hotmartUsernameFallback);
        this.hotmartPassword = pickFirstNonBlank(hotmartPassword, hotmartPasswordFallback);
        this.logFullToken = logFullToken;
        this.backendBaseUrl = backendBaseUrl;
        this.hotmartJwtSettingKey = hotmartJwtSettingKey;
        this.workspaceId = workspaceId;
        this.defaultNiche = defaultNiche;
        this.defaultMarketTheme = defaultMarketTheme;
    }

    public HotmartCollectionResponse collect(HotmartCollectionRequest request) {
        int boundedMax = request.maxProducts() <= 0 ? 10 : Math.min(request.maxProducts(), 50);

        List<HotmartProductSnapshot> products = new ArrayList<>();
        String status = "COLLECTION_EXECUTED";
        String message = "Coleta executada com token JWT da configuração geral.";
        boolean hasSessionCookie = hotmartSessionCookie != null && !hotmartSessionCookie.isBlank();
        boolean hasCredentials = hotmartUsername != null && !hotmartUsername.isBlank()
                && hotmartPassword != null && !hotmartPassword.isBlank();

        String hotmartAccessToken = fetchHotmartJwtFromGeneralSettings();
        if (hotmartAccessToken.isBlank()) {
            log.warn("Coleta Hotmart ignorada: token JWT não encontrado em configurações gerais.");
            return new HotmartCollectionResponse(
                    "COLLECTION_SKIPPED",
                    "Token JWT da Hotmart ausente. Configure a chave '" + hotmartJwtSettingKey + "' em /api/settings/{name}.",
                    products
            );
        }

        log.info(
                "Iniciando coleta Hotmart com Playwright. headless={}, maxProductsSolicitado={}, maxProductsAplicado={}, hasSessionCookie={}, hasCredentials={}",
                headless,
                request.maxProducts(),
                boundedMax,
                hasSessionCookie,
                hasCredentials
        );
        logTokenDiagnostics(hotmartAccessToken);

        try (Playwright ignored = Playwright.create()) {
            int apiCollected = collectProductsViaMarketApi(hotmartAccessToken, boundedMax, products);
            log.info("Coleta API Hotmart finalizada. produtosColetadosViaApi={}", apiCollected);
            status = "COLLECTION_EXECUTED";
            message = "Coleta executada via API da Hotmart usando JWT salvo em configurações gerais.";
        } catch (Exception ex) {
            status = "COLLECTION_ERROR";
            message = "Falha na coleta API: " + ex.getMessage();
            log.error("Erro na coleta Hotmart via API JWT.", ex);
        }
        persistCollectedProductsOnBackend(request, status, products);
        return new HotmartCollectionResponse(status, message, products);
    }

    private void persistCollectedProductsOnBackend(
            HotmartCollectionRequest request,
            String status,
            List<HotmartProductSnapshot> products
    ) {
        String jobId = "hotmart-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
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
                    "sources", List.of("HOTMART"),
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
                log.warn("Falha ao persistir coleta Hotmart no backend. status={}, endpoint={}, body={}",
                        response.statusCode(), endpoint, truncateForLog(response.body(), 600));
                return;
            }
            log.info("Coleta Hotmart persistida no backend com sucesso. endpoint={}, jobId={}, produtos={}",
                    endpoint, jobId, products.size());
        } catch (Exception ex) {
            log.warn("Falha ao persistir produtos coletados no backend.", ex);
        }
    }

    private List<Map<String, Object>> toCollectedReferences(String jobId, String source, List<HotmartProductSnapshot> products) {
        List<Map<String, Object>> references = new ArrayList<>();
        int position = 1;
        for (HotmartProductSnapshot product : products) {
            Map<String, String> rawMetadata = new HashMap<>();
            rawMetadata.put("productName", product.title());
            rawMetadata.put("productUrl", product.detailsUrl());
            rawMetadata.put("salesPageUrl", coalesceNotBlank(product.salesPageUrl(), product.detailsUrl()));
            if (product.temperature() != null) {
                rawMetadata.put("hotmartTemperature", String.valueOf(product.temperature()));
            }
            rawMetadata.put("producerName", null);

            Map<String, Object> reference = new HashMap<>();
            reference.put("referenceId", "hotmart-" + position + "-" + UUID.randomUUID().toString().substring(0, 8));
            reference.put("jobId", jobId);
            reference.put("source", "HOTMART");
            reference.put("title", product.title());
            reference.put("url", coalesceNotBlank(product.salesPageUrl(), product.detailsUrl()));
            reference.put("niche", defaultNiche);
            reference.put("status", "COLLECTED");
            reference.put("favorite", false);
            reference.put("importedReferenceId", null);
            reference.put("successScore", 80);
            reference.put("successSignal", "HOTMART_TRENDING");
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

    private String fetchHotmartJwtFromGeneralSettings() {
        try {
            String endpoint = backendBaseUrl + "/api/settings/" + hotmartJwtSettingKey;
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
            return root.path("value").asText("");
        } catch (Exception ex) {
            log.warn("Erro ao buscar JWT da Hotmart em configurações gerais.", ex);
            return "";
        }
    }

    private int collectProductsViaMarketApi(String accessToken, int boundedMax, List<HotmartProductSnapshot> products) {
        try {
            String payload = objectMapper.writeValueAsString(java.util.Map.of(
                    "page", 1,
                    "rows", boundedMax,
                    "userLocale", "PT_BR",
                    "name", "hottest"
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HOTMART_MARKET_API_URL))
                    .header("accept", "application/json, text/plain, */*")
                    .header("content-type", "application/json")
                    .header("authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            log.info("Resposta API Hotmart recebida. status={}, bodyPreview='{}'", response.statusCode(), truncateForLog(body, 1200));
            log.info("HOTMART_FETCH_RESPOSTA_CRUA status={} bodyRaw='{}'",
                    response.statusCode(),
                    truncateForLog(body, 10_000));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return 0;
            }
            JsonNode data = objectMapper.readTree(body);
            JsonNode productsNode = firstArray(data, "products", "items", "content", "results");
            if (productsNode == null || !productsNode.isArray()) {
                log.warn("Resposta API Hotmart sem array de produtos. keysRaiz={}", previewFieldNames(data));
                return 0;
            }
            log.info("Resposta API Hotmart: totalItensArray={}, chavesPrimeiroItem={}",
                    productsNode.size(),
                    productsNode.size() > 0 ? previewFieldNames(productsNode.get(0)) : "[]");
            for (JsonNode item : productsNode) {
                if (products.size() >= boundedMax) break;
                String title = extractProductText(item, "name", "productName", "title");
                String salesPageUrl = extractProductText(item, "salesPageUrl", "pageUrl", "page_url", "link");
                String url = extractProductText(item, "checkoutUrl", "productUrl", "url", "link");
                if (url == null || url.isBlank()) {
                    url = hotmartMarketUrl;
                }
                if (title == null || title.isBlank()) {
                    log.warn("Item Hotmart sem título mapeável. chavesItem={}, itemPreview={}",
                            previewFieldNames(item),
                            truncateForLog(item.toString(), 600));
                }
                Double temperature = extractProductNumber(item, "temperature", "temp", "hotness");
                products.add(new HotmartProductSnapshot(
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
            log.warn("Falha na coleta de produtos via API Hotmart.", ex);
            return 0;
        }
    }

    /* legacy Playwright flow kept below for rollback safety */
    private HotmartCollectionResponse legacyCollect(HotmartCollectionRequest request) {
        int boundedMax = request.maxProducts() <= 0 ? 10 : Math.min(request.maxProducts(), 50);
        List<HotmartProductSnapshot> products = new ArrayList<>();
        String status = "COLLECTION_EXECUTED";
        String message = "Coleta executada com Playwright em modo headless=" + headless + ".";
        boolean hasSessionCookie = hotmartSessionCookie != null && !hotmartSessionCookie.isBlank();
        boolean hasCredentials = hotmartUsername != null && !hotmartUsername.isBlank()
                && hotmartPassword != null && !hotmartPassword.isBlank();
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

            authenticateHotmart(context, page, hasSessionCookie, hasCredentials);
            String hotmartAccessToken = tryExtractAccessToken(page);
            logTokenDiagnostics(hotmartAccessToken);

            log.info("Navegando para URL de mercado Hotmart: {}", hotmartMarketUrl);
            page.navigate(hotmartMarketUrl, new Page.NavigateOptions()
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
                int apiCollected = collectProductsViaMarketApi(page, hotmartAccessToken, boundedMax, products);
                log.info("Fallback API Hotmart finalizado. produtosColetadosViaApi={}", apiCollected);
            }
            if (products.isEmpty()) {
                for (int i = 0; i < maxToRead; i++) {
                String title = page.locator("a[href*='/market/products/']").nth(i).innerText();
                String detailsUrl = page.locator("a[href*='/market/products/']").nth(i).getAttribute("href");
                if (detailsUrl != null && detailsUrl.startsWith("/")) {
                    detailsUrl = "https://app.hotmart.com" + detailsUrl;
                }
                products.add(new HotmartProductSnapshot(
                        title == null || title.isBlank() ? "Produto sem título" : title,
                        "N/A",
                        "N/A",
                        detailsUrl == null ? hotmartMarketUrl : detailsUrl,
                        Instant.now()
                ));
                }
            }

            log.info("Coleta Hotmart finalizada com sucesso. produtosColetados={}", products.size());
            context.close();
            browser.close();
        } catch (Exception ex) {
            status = "COLLECTION_ERROR";
            message = "Falha na coleta Playwright: " + ex.getMessage();
            log.error(
                    "Erro na coleta Hotmart via Playwright. headless={}, hasSessionCookie={}, hasCredentials={}, marketUrl='{}'",
                    headless,
                    hasSessionCookie,
                    hasCredentials,
                    hotmartMarketUrl,
                    ex
            );
        }

        return new HotmartCollectionResponse(status, message, products);
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


    private int collectProductsViaMarketApi(Page page, String accessToken, int boundedMax, List<HotmartProductSnapshot> products) {
        try {
            log.info("Fallback API Hotmart: iniciando chamada. hasAccessToken={}, tokenPreview='{}'",
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
                    """, java.util.Map.of("apiUrl", HOTMART_MARKET_API_URL, "rows", boundedMax, "accessToken", accessToken == null ? "" : accessToken)).toString();

            JsonNode root = objectMapper.readTree(response);
            int status = root.path("status").asInt();
            String body = root.path("body").asText("{}");
            log.info("Resposta fallback API Hotmart recebida. status={}, bodyPreview='{}'", status, truncateForLog(body, 1200));
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
                    url = hotmartMarketUrl;
                }
                products.add(new HotmartProductSnapshot(
                        title == null || title.isBlank() ? "Produto sem título" : title,
                        "N/A",
                        "N/A",
                        url,
                        Instant.now()
                ));
            }
            return products.size();
        } catch (Exception ex) {
            log.warn("Falha no fallback de coleta via API Hotmart.", ex);
            return 0;
        }
    }

    private String tryExtractAccessToken(Page page) {
        log.info("Token Hotmart: iniciando extração pós-login (localStorage/sessionStorage/cookies).");
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
                log.warn("Token Hotmart: nenhum token JWT encontrado no contexto da página. url='{}'", page.url());
                return "";
            }
            log.info("Token Hotmart: token capturado com sucesso.");
            return token;
        } catch (Exception ex) {
            log.warn("Token Hotmart: falha ao extrair token pós-login. url='{}'", page.url(), ex);
            return "";
        }
    }

    private void logTokenDiagnostics(String token) {
        boolean hasToken = token != null && !token.isBlank();
        log.info("Token Hotmart: hasToken={}, length={}", hasToken, hasToken ? token.length() : 0);
        if (!hasToken) {
            return;
        }
        if (logFullToken) {
            log.warn("Token Hotmart (FULL - modo diagnóstico): {}", token);
        } else {
            log.info("Token Hotmart (masked): {}", maskToken(token));
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
        log.info("Iniciando login Hotmart por credenciais (username preenchido={}).",
                hotmartUsername != null && !hotmartUsername.isBlank());
        page.navigate("https://sso.hotmart.com/login", new Page.NavigateOptions()
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
            log.info("Diagnóstico de seletores Hotmart login: emailMatches={}, passwordMatches={}, submitMatches={}, url='{}'",
                    emailCount, passwordCount, submitCount, page.url());

            page.locator(emailSelector).first().fill(hotmartUsername);
            page.locator(passwordSelector).first().fill(hotmartPassword);
            closeCookieOrConsentOverlays(page);
            submitLoginWithRetry(page, submitSelector, passwordSelector);
            ensureLoginProgressed(page, passwordSelector);
            log.info("Login Hotmart submetido. URL atual após espera='{}'.", page.url());
        } catch (Exception ex) {
            String inputSnapshot = page.locator("input")
                    .evaluateAll("els => els.slice(0, 10).map((el, idx) => `${idx}:type=${el.getAttribute('type') || ''};name=${el.getAttribute('name') || ''};id=${el.getAttribute('id') || ''};placeholder=${el.getAttribute('placeholder') || ''}`).join(' | ')")
                    .toString();
            log.error("Falha ao preencher/submeter login Hotmart. url='{}', inputSnapshot='{}'", page.url(), inputSnapshot, ex);
            throw ex;
        }
    }

    private void authenticateHotmart(BrowserContext context, Page page, boolean hasSessionCookie, boolean hasCredentials) {
        if (hasCredentials) {
            log.info("Usando autenticação por login/senha Hotmart.");
            try {
                performLogin(page);
                return;
            } catch (Exception ex) {
                if (hasSessionCookie) {
                    log.warn("Login por credenciais falhou; aplicando fallback para cookie de sessão Hotmart.", ex);
                    applySessionCookie(context);
                    return;
                }
                throw ex;
            }
        }

        if (hasSessionCookie) {
            log.info("Usando autenticação por cookie de sessão Hotmart.");
            applySessionCookie(context);
        }
    }

    private void applySessionCookie(BrowserContext context) {
        context.addCookies(List.of(new Cookie("hotmart_session", hotmartSessionCookie)
                .setDomain(".hotmart.com")
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
                log.warn("Tentativa {} de submit no login Hotmart falhou: {}", attempt, ex.getMessage());
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
                "#hotmart-cookie-policy button",
                "hotmart-cookie-policy button",
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
            page.locator("#hotmart-cookie-policy")
                    .waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(5_000));
        } catch (Exception ignored) {
            log.warn("Overlay de cookie não ocultou no tempo esperado; aplicando fallback por JS. url='{}'", page.url());
            hideOverlayWithJavascript(page, "#hotmart-cookie-policy");
            hideOverlayWithJavascript(page, "hotmart-cookie-policy");
            hideOverlayWithJavascript(page, ".hotmart-cookie-policy-container");
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
        if (!currentUrl.contains("sso.hotmart.com/login")) {
            return;
        }
        for (int attempt = 1; attempt <= LOGIN_SUBMIT_RETRIES && page.url().contains("sso.hotmart.com/login"); attempt++) {
            log.warn("Login ainda na SSO após submit; forçando nova tentativa de progressão. tentativa={}", attempt);
            closeCookieOrConsentOverlays(page);
            page.locator(passwordSelector).first().press("Enter");
            page.waitForTimeout(1_000L * attempt);
            closeCookieOrConsentOverlays(page);
        }
        if (page.url().contains("sso.hotmart.com/login")) {
            throw new IllegalStateException("Login Hotmart não avançou após tentativas de remover banner/consentimento.");
        }
    }

    private void waitTransientOverlaysToHide(Page page) {
        waitOverlayHidden(page, "#loader");
        waitOverlayHidden(page, "#hotmart-cookie-policy");
        waitOverlayHidden(page, "hotmart-cookie-policy");
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
}
