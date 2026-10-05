const { chromium, devices, expect } = require("@playwright/test");
const http = require("node:http");
const fs = require("node:fs/promises");
const path = require("node:path");

const backend = process.env.VIDEO_REVIEW_BACKEND;
if (!backend || !/^http:\/\/127\.0\.0\.1:\d+$/.test(backend))
  throw new Error("Homologação requer backend local efêmero.");
const product = process.env.VIDEO_REVIEW_PRODUCT;
const unrelated = process.env.VIDEO_REVIEW_UNRELATED_PRODUCT;
const experiment = process.env.VIDEO_REVIEW_EXPERIMENT;
const asset = process.env.VIDEO_REVIEW_ASSET;
const dist = path.resolve(__dirname, "../dist");
const output = path.resolve(__dirname, "../../.codex/video-review-validation");
const mime = {
  ".js": "text/javascript",
  ".css": "text/css",
  ".html": "text/html",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".json": "application/json",
};
const server = http.createServer(async (req, res) => {
  try {
    const pathname = new URL(req.url, "http://localhost").pathname;
    const file = pathname.startsWith("/assets/")
      ? path.join(dist, pathname)
      : path.join(dist, "index.html");
    res.setHeader(
      "Content-Type",
      mime[path.extname(file)] || "application/octet-stream",
    );
    res.end(await fs.readFile(file));
  } catch (error) {
    res.statusCode = 404;
    res.end("Fixture ausente");
  }
});
(async () => {
  await fs.mkdir(output, { recursive: true });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const origin = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  try {
    for (const [name, options] of [
      ["desktop", { viewport: { width: 1440, height: 1000 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      const context = await browser.newContext(options);
      const page = await context.newPage();
      const pageErrors = [];
      page.on("pageerror", (error) => pageErrors.push(error.message));
      await context.route("**/*", async (route) => {
        const url = new URL(route.request().url());
        if (url.pathname.startsWith("/api/creatives/")) {
          return route.fulfill({
            response: await route.fetch({
              url: backend + url.pathname + url.search,
            }),
          });
        }
        if (url.pathname === "/api/facebook/configuration-status")
          return route.fulfill({
            json: {
              worker: { ready: true, hasAccount: true },
              tokenRenewal: {
                enabledAccounts: 1,
                eligibleAccounts: 1,
                accounts: [],
              },
            },
          });
        if (url.pathname.startsWith("/api/"))
          return route.fulfill({
            status: 503,
            json: { message: "Dependência fora do escopo simulada" },
          });
        if (url.origin === origin) return route.continue();
        return route.abort();
      });
      await page.goto(
        origin +
          `/products/${unrelated}/value-chain-history/processes/123/activities?sourceReference=experiment%3A${experiment}`,
      );
      await page.waitForResponse((r) =>
        r.url().includes("/video-review/summary"),
      );
      await expect(
        page.getByRole("link", { name: "Ver aprovações necessárias" }),
      ).toHaveCount(0);
      await page.screenshot({ path: path.join(output, `${name}-product.png`) });
      await page.goto(
        origin +
          `/creative-video-review?productId=${product}&experimentId=${experiment}`,
      );
      await expect(
        page.getByText("1 vídeo precisa da sua aprovação"),
      ).toBeVisible();
      await expect(
        page.getByRole("link", { name: "Ver aprovações necessárias" }),
      ).toHaveAttribute(
        "href",
        `/creative-video-review?productId=${product}&experimentId=${experiment}`,
      );
      await expect(
        page.getByText("Nova peça local", { exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole("button", { name: "Aprovar para portfólio" }),
      ).toBeEnabled();
      await expect(
        page.getByText("Candidata opcional local", { exact: true }),
      ).toHaveCount(0);
      await page
        .getByRole("button", { name: "Candidatas opcionais", exact: true })
        .click();
      await expect(
        page.getByText("Candidata opcional local", { exact: true }),
      ).toBeVisible();
      await expect(
        page.getByText("Candidata opcional — sem aprovação obrigatória", {
          exact: true,
        }),
      ).toBeVisible();
      await page.screenshot({
        path: path.join(output, `${name}-optional.png`),
        fullPage: true,
      });
      await page.getByRole("button", { name: "Ajustes e pareceres" }).click();
      await expect(
        page.getByText("Anúncio em ajuste", { exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole("button", { name: "Aprovar para portfólio" }),
      ).toBeDisabled();
      await page.goto(origin + "/creative-video-review");
      await page
        .getByRole("button", { name: "Histórico", exact: true })
        .click();
      await expect(
        page.getByText("Tentativa histórica", { exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole("button", { name: "Aprovar para portfólio" }),
      ).toHaveCount(0);
      await expect(
        page.getByRole("button", { name: "Reavaliar com Têmis" }),
      ).toHaveCount(0);
      await page.screenshot({
        path: path.join(output, `${name}-history.png`),
        fullPage: true,
      });
      const overflow = await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 2,
      );
      expect(overflow).toBe(false);
      expect(pageErrors).toEqual([]);
      if (name === "pixel") {
        await page
          .getByRole("button", { name: "Aprovações necessárias", exact: true })
          .click();
        await page
          .getByRole("button", { name: "Aprovar para portfólio" })
          .click();
        await expect(
          page.getByText("Nenhuma aprovação necessária neste contexto."),
        ).toBeVisible();
        await expect(
          page.getByRole("link", { name: "Ver aprovações necessárias" }),
        ).toHaveCount(0);
        await page
          .getByRole("button", { name: "Candidatas opcionais", exact: true })
          .click();
        await expect(
          page.getByText("Candidata opcional local", { exact: true }),
        ).toBeVisible();
        await page
          .getByRole("button", { name: "Aprovados", exact: true })
          .click();
        await expect(
          page.getByText(`Vídeo produzido #${asset}`, { exact: true }),
        ).toBeVisible();
      }
      console.log(
        JSON.stringify({
          device: name,
          result: "PASS",
          backend: "local H2",
          outsideScope: "simulated",
          externalCalls: 0,
        }),
      );
      await context.close();
    }
  } finally {
    await browser.close();
    await new Promise((resolve) => server.close(resolve));
  }
})().catch((error) => {
  console.error(error);
  server.close();
  process.exitCode = 1;
});
