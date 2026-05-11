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
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
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
    private static final String HOTMART_MARKET_API_URL = "https://api-affiliation-market.hotmart.com/v2/market/search";

    private final boolean headless;
    private final String chromiumExecutablePath;
    private final String hotmartMarketUrl;
    private final String hotmartSessionCookie;
    private final String hotmartUsername;
    private final String hotmartPassword;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HotmartCollectorService(
            @Value("${collector.playwright.headless:true}") boolean headless,
            @Value("${collector.playwright.chromium-executable-path:}") String chromiumExecutablePath,
            @Value("${collector.hotmart.search-url:https://app.hotmart.com/market/search}") String hotmartMarketUrl,
            @Value("${collector.hotmart.session-cookie:}") String hotmartSessionCookie,
            @Value("${collector.hotmart.username:}") String hotmartUsername,
            @Value("${collector.hotmart.password:}") String hotmartPassword,
            @Value("${collector.hotmart.username-fallback:}") String hotmartUsernameFallback,
            @Value("${collector.hotmart.password-fallback:}") String hotmartPasswordFallback
    ) {
        this.headless = headless;
        this.chromiumExecutablePath = chromiumExecutablePath;
        this.hotmartMarketUrl = hotmartMarketUrl;
        this.hotmartSessionCookie = hotmartSessionCookie;
        this.hotmartUsername = pickFirstNonBlank(hotmartUsername, hotmartUsernameFallback);
        this.hotmartPassword = pickFirstNonBlank(hotmartPassword, hotmartPasswordFallback);
    }

    public HotmartCollectionResponse collect(HotmartCollectionRequest request) {
        int boundedMax = request.maxProducts() <= 0 ? 10 : Math.min(request.maxProducts(), 50);

        List<HotmartProductSnapshot> products = new ArrayList<>();
        String status = "COLLECTION_EXECUTED";
        String message = "Coleta executada com Playwright em modo headless=" + headless + ".";
        boolean hasSessionCookie = hotmartSessionCookie != null && !hotmartSessionCookie.isBlank();
        boolean hasCredentials = hotmartUsername != null && !hotmartUsername.isBlank()
                && hotmartPassword != null && !hotmartPassword.isBlank();

        if (!hasSessionCookie && !hasCredentials) {
            log.warn("Coleta Hotmart ignorada: autenticação ausente (sem cookie de sessão e sem credenciais).");
            return new HotmartCollectionResponse(
                    "COLLECTION_SKIPPED",
                    "Autenticação Hotmart ausente. Configure collector.hotmart.session-cookie "
                            + "ou collector.hotmart.username/password.",
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
        logPlaywrightRuntimeDiagnostics();

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
                int apiCollected = collectProductsViaMarketApi(page, boundedMax, products);
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


    private int collectProductsViaMarketApi(Page page, int boundedMax, List<HotmartProductSnapshot> products) {
        try {
            String response = page.evaluate("""
                    async ({ apiUrl, rows }) => {
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
                          'content-type': 'application/json'
                        },
                        body: JSON.stringify(payload)
                      });
                      const text = await res.text();
                      return JSON.stringify({ status: res.status, body: text });
                    }
                    """, java.util.Map.of("apiUrl", HOTMART_MARKET_API_URL, "rows", boundedMax)).toString();

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
                waitTransientOverlaysToHide(page);
                page.locator(submitSelector).first().scrollIntoViewIfNeeded();
                page.locator(submitSelector).first().click();
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
                "button:has-text('Accept all cookies')",
                "button:has-text('Accept all')",
                "button:has-text('I agree')",
                "button:has-text('Aceitar')",
                "button:has-text('Aceitar tudo')",
                "button:has-text('Concordo')"
        );
        try {
            if (page.locator(cookieAcceptSelectors).first().isVisible()) {
                page.locator(cookieAcceptSelectors).first().click(new com.microsoft.playwright.Locator.ClickOptions().setTimeout(3_000));
                log.info("Banner de cookies detectado e aceito automaticamente.");
            }
        } catch (Exception ignored) {
            log.debug("Nenhum botão de aceite de cookie acionável encontrado.");
        }

        try {
            page.locator("#hotmart-cookie-policy")
                    .waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(5_000));
        } catch (Exception ignored) {
            log.debug("Overlay de cookie não ocultou no tempo esperado; aplicando fallback por JS.");
            hideOverlayWithJavascript(page, "#hotmart-cookie-policy");
        }
    }


    private void hideOverlayWithJavascript(Page page, String selector) {
        try {
            page.evaluate("(sel) => { const el = document.querySelector(sel); if (el) { el.style.display = 'none'; el.style.pointerEvents = 'none'; } }", selector);
            log.info("Overlay '{}' ocultado por fallback JS.", selector);
        } catch (Exception ex) {
            log.debug("Falha ao ocultar overlay '{}' por JS: {}", selector, ex.getMessage());
        }
    }

    private void waitTransientOverlaysToHide(Page page) {
        waitOverlayHidden(page, "#loader");
        waitOverlayHidden(page, "#hotmart-cookie-policy");
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
