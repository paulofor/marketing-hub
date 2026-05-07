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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HotmartCollectorService {

    private final boolean headless;
    private final String hotmartMarketUrl;
    private final String hotmartSessionCookie;

    public HotmartCollectorService(
            @Value("${collector.playwright.headless:true}") boolean headless,
            @Value("${collector.hotmart.search-url:https://app.hotmart.com/market/search}") String hotmartMarketUrl,
            @Value("${collector.hotmart.session-cookie:}") String hotmartSessionCookie
    ) {
        this.headless = headless;
        this.hotmartMarketUrl = hotmartMarketUrl;
        this.hotmartSessionCookie = hotmartSessionCookie;
    }

    public HotmartCollectionResponse collect(HotmartCollectionRequest request) {
        int boundedMax = request.maxProducts() <= 0 ? 10 : Math.min(request.maxProducts(), 50);

        List<HotmartProductSnapshot> products = new ArrayList<>();
        String status = "COLLECTION_EXECUTED";
        String message = "Coleta executada com Playwright em modo headless=" + headless + ".";
        if (hotmartSessionCookie == null || hotmartSessionCookie.isBlank()) {
            return new HotmartCollectionResponse(
                    "COLLECTION_SKIPPED",
                    "Sessão Hotmart ausente. Configure collector.hotmart.session-cookie.",
                    products
            );
        }

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
            BrowserContext context = browser.newContext();
            context.addCookies(List.of(new Cookie("hotmart_session", hotmartSessionCookie)
                    .setDomain(".hotmart.com")
                    .setPath("/")
                    .setHttpOnly(true)
                    .setSecure(true)));
            Page page = context.newPage();
            page.navigate(hotmartMarketUrl, new Page.NavigateOptions()
                    .setTimeout(60_000)
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));
            page.waitForTimeout(1_500);

            int cardsCount = page.locator("a[href*='/market/products/']").count();
            int maxToRead = Math.min(cardsCount, boundedMax);
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

            context.close();
            browser.close();
        } catch (Exception ex) {
            status = "COLLECTION_ERROR";
            message = "Falha na coleta Playwright: " + ex.getMessage();
        }

        return new HotmartCollectionResponse(status, message, products);
    }
}
