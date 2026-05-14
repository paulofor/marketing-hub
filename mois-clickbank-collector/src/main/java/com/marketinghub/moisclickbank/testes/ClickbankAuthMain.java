package com.marketinghub.moisclickbank.testes;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

public class ClickbankAuthMain {

    public static void main(String[] args) {
        String username = System.getenv("CLICKBANK_USERNAME");
        String password = System.getenv("CLICKBANK_PASSWORD");

        if (isBlank(username) || isBlank(password)) {
            System.err.println("Defina CLICKBANK_USERNAME e CLICKBANK_PASSWORD antes de executar.");
            System.exit(1);
        }

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.navigate("https://app.clickbank.com/login", new Page.NavigateOptions()
                    .setTimeout(60_000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            page.locator("input[type='email'], input[name='email']").first().fill(username);
            page.locator("input[type='password'], input[name='password']").first().fill(password);
            page.locator("button[type='submit']").first().click();

            page.waitForTimeout(5_000);
            System.out.println("URL atual após login: " + page.url());

            context.close();
            browser.close();
        } catch (Exception ex) {
            System.err.println("Falha na autenticação Clickbank: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
