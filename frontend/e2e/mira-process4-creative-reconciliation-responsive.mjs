import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const baseUrl = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
assert.equal(
  new URL(baseUrl).hostname,
  "127.0.0.1",
  "A homologação responsiva deve usar somente o frontend local",
);

const approvedAsset = {
  id: 311,
  commercialPlanId: 8,
  assetUrl: "https://cdn.test/mira-control.png",
  label: "Controle estático Mira",
  contentSha256:
    "bd5bd13370ebf69ff022abcbe6b69bf5735dc271cb0bde64cebf68eccad53637",
  origin: "Processo criativo aprovado",
  rightsStatement: "Uso comercial autorizado",
};
const officialInstagram = {
  id: 1,
  name: "Produtividade360",
  handle: "@produtividade360_",
  code: "instagram-official-1",
};
const officialPage = {
  id: 1,
  accountId: 1,
  pageId: "meta-page-official-1",
  name: "Produtividade360",
};

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
});
try {
  for (const [profileName, contextOptions] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iPhone-15-Pro", devices["iPhone 15 Pro"]],
    ["Pixel-7", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(contextOptions);
    const page = await context.newPage();
    const pageErrors = [];
    const writes = [];
    let publishingIdentity = null;
    let creative = null;
    page.on("pageerror", (error) => pageErrors.push(error.message));
    await page.route("**/*", async (route) => {
      const request = route.request();
      const url = new URL(request.url());
      if (url.hostname === "cdn.test") {
        await route.fulfill({
          status: 200,
          contentType: "image/png",
          body: Buffer.from(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
            "base64",
          ),
        });
        return;
      }
      if (!url.pathname.startsWith("/api/")) {
        await route.continue();
        return;
      }
      if (request.method() !== "GET") {
        writes.push({ method: request.method(), path: url.pathname });
      }
      if (url.pathname === "/api/experiments/93") {
        await route.fulfill({
          json: {
            id: "93",
            nicheId: 34,
            hypothesisId: "11111111-1111-1111-1111-111111111111",
            productId: 10,
            productName: "Mira",
            name: "Mira — piloto comercial",
            hypothesis: "Validar primeira venda com rotina individualizada",
            creationSource: "SYSTEM_FLOW",
            experimentType: "LOW_TICKET_PRODUCT",
            platform: "FACEBOOK",
            stage: "AD",
            status: "PLANNED",
            creativeApproved: false,
            creativesToGenerate: 0,
            startDate: null,
            endDate: null,
            kpiTarget: null,
            metricPresetId: null,
            followUpActionUrl: "https://mira.digicomdigital.com.br",
            commercialCheckoutUrl: "https://checkout.test/mira",
            instagramAccount: publishingIdentity ? officialInstagram : null,
            facebookPage: publishingIdentity ? officialPage : null,
            adCopy: JSON.stringify({
              adCopy: {
                primaryTextVariants: [
                  {
                    headline: "Organize sua rotina",
                    primaryText:
                      "Use os produtos que você já tem em uma rotina simples e individualizada.",
                    description: "Acesso único por R$ 49",
                  },
                ],
              },
            }),
          },
        });
        return;
      }
      if (
        url.pathname === "/api/experiments/93/publishing-identity" &&
        request.method() === "PATCH"
      ) {
        publishingIdentity = request.postDataJSON();
        assert.deepEqual(publishingIdentity, {
          facebookPageId: 1,
          instagramAccountId: 1,
        });
        await route.fulfill({ json: { id: "93" } });
        return;
      }
      if (
        url.pathname ===
        "/api/experiments/93/commercial-plan-visual-assets/eligible"
      ) {
        await route.fulfill({ json: [approvedAsset] });
        return;
      }
      if (
        url.pathname ===
          "/api/experiments/93/commercial-plan-visual-assets/311/creative" &&
        request.method() === "POST"
      ) {
        const body = request.postDataJSON();
        assert.deepEqual(body, {
          headline: "Organize sua rotina",
          primaryText:
            "Use os produtos que você já tem em uma rotina simples e individualizada.",
          description: "Acesso único por R$ 49",
        });
        creative = {
          id: 612,
          creativeId: 612,
          experimentId: 93,
          sourceCreativeId: null,
          versionNumber: 1,
          finalCandidate: false,
          format: "IMAGE",
          headline: body.headline,
          primaryText: body.primaryText,
          description: body.description,
          imageUrl: approvedAsset.assetUrl,
          destinationUrl: "https://mira.digicomdigital.com.br",
          cta: "LEARN_MORE",
          status: "DRAFT",
          agentReviewStatus: "PENDING",
        };
        await route.fulfill({ json: creative });
        return;
      }
      if (url.pathname === "/api/products/experiments/93/ads-in-use") {
        await route.fulfill({ json: { ads: creative ? [creative] : [] } });
        return;
      }
      if (url.pathname === "/api/accounts/instagram") {
        await route.fulfill({ json: [officialInstagram] });
        return;
      }
      if (url.pathname === "/api/accounts/facebook") {
        await route.fulfill({ json: [{ id: 1, name: "Conta oficial" }] });
        return;
      }
      if (url.pathname === "/api/accounts/facebook/1/pages") {
        await route.fulfill({ json: [officialPage] });
        return;
      }
      if (url.pathname === "/api/niches/34") {
        await route.fulfill({ json: { id: 34, name: "Skincare" } });
        return;
      }
      await route.fulfill({ json: [] });
    });

    await page.goto(`${baseUrl}/experiments/93?tab=creatives`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("button", { name: "Salvar identidades" }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Usar como controle estático" }),
    ).toBeDisabled();
    await expect(
      page.getByText("Salve primeiro a identidade oficial do Instagram."),
    ).toBeVisible();

    await page.getByLabel("Conta do Instagram *").selectOption("1");
    await page
      .getByLabel("Página do Facebook usada pelo experimento")
      .selectOption("1");
    await page.getByRole("button", { name: "Salvar identidades" }).click();
    await expect(page.getByText("Identidades atualizadas")).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Usar como controle estático" }),
    ).toBeEnabled();

    await page
      .getByRole("button", { name: "Usar como controle estático" })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByText(/SHA-256 bd5bd133/)).toBeVisible();
    await expect(dialog.getByLabel("Título do controle estático")).toHaveValue(
      "Organize sua rotina",
    );
    await dialog
      .getByRole("button", { name: "Cadastrar e enviar para revisão" })
      .click();
    await expect(
      page.getByText("Controle vinculado ao anúncio #612"),
    ).toBeVisible();

    assert.deepEqual(
      writes,
      [
        { method: "PATCH", path: "/api/experiments/93/publishing-identity" },
        {
          method: "POST",
          path: "/api/experiments/93/commercial-plan-visual-assets/311/creative",
        },
      ],
      `${profileName}: somente os dois comandos auditáveis podem alterar estado`,
    );
    assert.deepEqual(pageErrors, [], `${profileName}: erros JavaScript`);
    const widths = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
    }));
    assert.ok(
      widths.content <= widths.viewport + 1,
      `${profileName}: overflow horizontal ${widths.content}px > ${widths.viewport}px`,
    );
    await page.screenshot({
      path: `/tmp/mira-process4-creative-reconciliation-${profileName}.png`,
      fullPage: true,
    });
    await context.close();
    console.log(`PASS reconciliação criativa Mira responsiva: ${profileName}`);
  }
} finally {
  await browser.close();
}
