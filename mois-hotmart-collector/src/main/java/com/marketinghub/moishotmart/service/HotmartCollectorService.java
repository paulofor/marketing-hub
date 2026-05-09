package com.marketinghub.moishotmart.service;

import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionRequest;
import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartCollectionResponse;
import com.marketinghub.moishotmart.dto.HotmartDtos.HotmartProductSnapshot;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;
import java.time.Instant;
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

    private final boolean headless;
    private final String chromiumExecutablePath;
    private final String hotmartMarketUrl;
    private final String hotmartSessionCookie;
    private final String hotmartUsername;
    private final String hotmartPassword;

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

            if (hasSessionCookie) {
                log.info("Usando autenticação por cookie de sessão Hotmart.");
                context.addCookies(List.of(new Cookie("hotmart_session", hotmartSessionCookie)
                        .setDomain(".hotmart.com")
                        .setPath("/")
                        .setHttpOnly(true)
                        .setSecure(true)));
            } else {
                log.info("Usando autenticação por login/senha Hotmart.");
                performLogin(page);
            }

            log.info("Navegando para URL de mercado Hotmart: {}", hotmartMarketUrl);
            page.navigate(hotmartMarketUrl, new Page.NavigateOptions()
                    .setTimeout(60_000)
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));
            page.waitForTimeout(1_500);

            int cardsCount = page.locator("a[href*='/market/products/']").count();
            int maxToRead = Math.min(cardsCount, boundedMax);
            log.info("Página carregada. cardsEncontrados={}, cardsProcessados={}", cardsCount, maxToRead);
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
        page.navigate("https://app.hotmart.com/login", new Page.NavigateOptions()
                .setTimeout(60_000)
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        page.locator("input[type='email'], input[name='email']").first().fill(hotmartUsername);
        page.locator("input[type='password'], input[name='password']").first().fill(hotmartPassword);
        page.locator("button[type='submit']").first().click();
        page.waitForTimeout(3_000);
        log.info("Login Hotmart submetido. URL atual após espera='{}'.", page.url());
    }
}
