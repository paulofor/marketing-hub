/** Homologa autoria, biblioteca e montagem responsiva sem produção ou geração paga. */
import assert from "node:assert/strict";
import { mkdir, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const base = process.env.FRONTEND_TEST_URL || "http://127.0.0.1:5175";
const output =
  process.env.VIDEO_AUTHORING_EVIDENCE_DIR || "/tmp/product-video-authoring";
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
const results = [];
try {
  for (const [name, device] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(device);
    const page = await context.newPage();
    const errors = [],
      external = [],
      writes = [];
    const profile = {
      id: 91002,
      productId: 91001,
      title: "Personalizado #7",
      videoKind: "HERO",
      avatarStrategy: "PLATFORM_TEST_AVATAR",
      requiresConsent: false,
      status: "DRAFT",
    };
    const assets = [];
    page.on("pageerror", (e) => errors.push(e.message));
    page.on("console", (m) => {
      if (m.type() === "error") console.log(name, m.text());
    });
    await page.route("**/*", async (route) => {
      const request = route.request(),
        url = new URL(request.url());
      if (url.origin !== base) {
        external.push(url.href);
        return route.abort();
      }
      if (url.pathname === "/fixture-image.png")
        return route.fulfill({
          contentType: "image/svg+xml",
          body: '<svg xmlns="http://www.w3.org/2000/svg" width="1536" height="1536"><rect width="1536" height="1536" fill="#662f48"/></svg>',
        });
      if (!url.pathname.startsWith("/api/")) return route.continue();
      if (request.method() !== "GET")
        writes.push({ path: url.pathname, body: request.postData() });
      let data = [];
      if (url.pathname === "/api/products/91001")
        data = {
          id: 91001,
          name: "Kit demonstrativo",
          primaryCta: "Conhecer meu kit",
        };
      else if (url.pathname === "/api/products/91001/sales-videos/profiles")
        data = [profile];
      else if (url.pathname === "/api/products/91001/sales-videos/jobs")
        data = [
          {
            id: 91030,
            profileId: profile.id,
            status: "VIDEO_READY",
            jobType: "RENDER",
            providerName: "EDITORIAL_MOTION",
            assetId: 91031,
            metadataJson: JSON.stringify({ videoProjectId: 91032 }),
          },
          {
            id: 91036,
            profileId: profile.id,
            status: "VIDEO_READY",
            jobType: "POST_PRODUCTION",
            providerName: "MUSA_POST_PRODUCTION",
            retryOfJobId: 91030,
            commercialReadinessStatus: "BLOCKED",
            commercialReadinessBlockers: [
              "A revisão humana final ainda não foi aprovada.",
            ],
          },
        ];
      else if (url.pathname === "/api/sales-videos/jobs/91036/events")
        data = [
          {
            id: 91037,
            createdAt: "2026-10-11T00:00:00Z",
            message: "Vídeo processado com sucesso",
            detailsJson: JSON.stringify({
              source: `https://fixture.local/${"artifact".repeat(100)}`,
              hash: "a".repeat(64),
            }),
          },
        ];
      else if (url.pathname === "/api/media/91031")
        data = {
          id: 91031,
          type: "VIDEO",
          status: "READY",
          url: `${base}/fixture-video.mp4`,
        };
      else if (
        url.pathname === "/api/sales-videos/projects/91032/delivery-examples"
      ) {
        assert.equal(request.headers()["x-tenant-id"], "default");
        data = [91033, 91034].map((id) => ({
          visualAssetId: id,
          commercialPlanId: 91020,
          label: `Amostra ${id} — ${"Descrição completa de arte aprovada para divulgação. ".repeat(7)}`,
          url: `${base}/fixture-image.png`,
          origin: "Fixture local segregada",
          rightsStatement: "Amostra ilustrativa de teste",
          sha256: String(id % 2).repeat(64),
        }));
      } else if (
        url.pathname === "/api/sales-videos/jobs/91030/request-post-production"
      ) {
        const body = request.postDataJSON();
        assert.equal(body.deliveryExamples.length, 2);
        assert.equal(body.proofShots.length, 1);
        assert.equal(body.proofShots[0].captionCueIndex, 1);
        assert.equal(body.narrationRevisionConfirmed, false);
        assert(body.deliveryExamples.every((s) => !s.url && !s.sha256));
        data = { id: 91035, profileId: profile.id, status: "VIDEO_REQUESTED" };
      } else if (
        url.pathname === "/api/sales-videos/profiles/91002/approve-script"
      ) {
        const body = request.postDataJSON();
        assert.equal(body.ctaText, "Peça uma amostra gratuita");
        assert.equal(body.captionText, "Negócio demonstrativo");
        assert(!JSON.stringify(body).includes("MUSA"));
        profile.latestScript = {
          ...body,
          id: 91003,
          version: 1,
          source: "MANUAL",
          status: "APPROVED",
        };
        data = profile.latestScript;
      } else if (url.pathname === "/api/planning/commercial-plans")
        data = [
          {
            id: 91020,
            name: "Plano segregado",
            status: "DRAFT",
            planType: "FIRST_SALE",
            referenceMonth: "2026-10",
            milestones: [],
            simulations: [],
            experiments: [],
          },
        ];
      else if (
        url.pathname === "/api/planning/commercial-plans/91020/visual-assets"
      ) {
        if (request.method() === "POST") {
          const body = request.postDataJSON();
          assert.equal(body.assetUrl, `${base}/fixture-image.png`);
          assert.equal(body.origin, "Composição de fonte aprovada");
          assert.equal(
            body.rightsStatement,
            "Demonstração autorizada, sem resultados de cliente",
          );
          data = { ...body, id: 91021, status: "DRAFT" };
          assets.push(data);
        } else data = assets;
      } else if (url.pathname === "/api/assets") {
        assert(request.postData().includes("DETERMINISTIC_COMPOSITE_V1"));
        data = { url: `${base}/fixture-image.png` };
      } else if (url.pathname.includes("/agent-activity"))
        data = { entries: [] };
      else if (url.pathname.includes("/operational-flow"))
        data = {
          stages: [],
          specialistDecisions: [],
          status: "BLOQUEADO",
          nextAction: "Fixture sem publicação",
        };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(data),
      });
    });
    await page.goto(`${base}/products/91001/sales-videos`);
    await expect(page.getByLabel("Roteiro completo *")).toHaveValue("");
    await expect(page.getByLabel("Chamada para ação *")).toHaveValue(
      "Conhecer meu kit",
    );
    await page
      .getByLabel("Roteiro completo *")
      .fill("Seu trabalho merece uma divulgação coerente.");
    await page
      .getByLabel("Chamada para ação *")
      .fill("Peça uma amostra gratuita");
    await page.getByLabel("Legenda da peça").fill("Negócio demonstrativo");
    await page.getByRole("button", { name: "Salvar roteiro aprovado" }).click();
    await expect
      .poll(
        () => writes.filter((w) => w.path.endsWith("approve-script")).length,
      )
      .toBe(1);
    await expect(page.getByLabel("Chamada para ação *")).toHaveValue(
      "Peça uma amostra gratuita",
    );
    await page.screenshot({
      path: `${output}/${name}-script.png`,
      fullPage: false,
    });
    await page
      .locator("#post-production-caption")
      .fill("Veja uma amostra. | Conheça o kit.");
    await page
      .locator("#post-production-voice")
      .fill("Veja uma amostra. Conheça o kit.");
    await page
      .getByText("Montagem com amostras da entrega", { exact: true })
      .click();
    await page
      .getByRole("button", { name: "Escolher amostras para a montagem" })
      .click();
    await page.locator("#delivery-example-0").selectOption("91033");
    await page.locator("#delivery-example-1").selectOption("91034");
    await page
      .locator("#delivery-headline-0")
      .fill("Sua divulgação, pronta para usar.");
    await expect
      .poll(() =>
        page
          .locator("fieldset img")
          .first()
          .evaluate((img) => img.naturalWidth),
      )
      .toBe(1536);
    await expect(
      page.getByText("Detalhe técnico:", { exact: false }),
    ).toBeVisible();
    const layout = await page.evaluate(() => ({
      viewport: innerWidth,
      width: document.documentElement.scrollWidth,
    }));
    assert(
      layout.width <= layout.viewport + 1,
      `Montagem transborda em ${name}: ${JSON.stringify(layout)}`,
    );
    await page.getByText("Sequência da demonstração", { exact: true }).click();
    await page.getByRole("button", { name: "Adicionar captura", exact: true }).click();
    await page.locator("#proof-reference-0").fill("internal://agent-tasks/91050/visual-evidence/91051#crop=0,0,900,700");
    await page.locator("#proof-cue-0").fill("2");
    await page.locator("#proof-seconds-0").fill("2");
    await page.locator("#delivery-example-0").scrollIntoViewIfNeeded();
    await page.screenshot({ path: `${output}/${name}-montage.png` });
    await page
      .getByRole("button", { name: "Gerar pós-produção + HLS", exact: true })
      .click();
    await expect
      .poll(
        () =>
          writes.filter((w) => w.path.endsWith("request-post-production"))
            .length,
      )
      .toBe(1);
    await page.goto(`${base}/planning/91020`);
    await page.getByLabel("Importar imagem composta").setInputFiles({
      name: "demo.png",
      mimeType: "image/png",
      buffer: Buffer.from(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/qukAAAAASUVORK5CYII=",
        "base64",
      ),
    });
    await writeFile(
      `${output}/${name}-library-before.txt`,
      await page.locator("body").innerText(),
    );
    await writeFile(
      `${output}/${name}-library-errors.json`,
      JSON.stringify(errors),
    );
    await page
      .getByLabel("Descrição *", { exact: true })
      .fill("Exemplo de divulgação");
    await page
      .getByLabel("Origem *", { exact: true })
      .fill("Composição de fonte aprovada");
    await page
      .getByLabel("Direitos de uso *", { exact: true })
      .fill("Demonstração autorizada, sem resultados de cliente");
    await page
      .getByRole("button", { name: "Enviar imagem", exact: true })
      .click();
    await expect(page.getByLabel("URL da mídia *")).toHaveValue(
      `${base}/fixture-image.png`,
    );
    await page
      .getByRole("button", { name: "Anexar ao kit", exact: true })
      .click();
    await expect.poll(() => assets.length).toBe(1);
    await expect(
      page.getByText("Exemplo de divulgação", { exact: true }),
    ).toBeVisible();
    assert.equal(assets[0].status, "DRAFT");
    assert.equal(writes.length, 4);
    assert.equal(errors.length, 0, errors.join("\n"));
    assert.equal(external.length, 0, external.join("\n"));
    await page.screenshot({
      path: `${output}/${name}-library.png`,
      fullPage: false,
    });
    results.push({
      device: name,
      scriptSavedToProfile: profile.id,
      assetStatus: assets[0].status,
      writes: writes.map((w) => w.path),
      externalRequests: external,
      errors,
    });
    await context.close();
  }
  await writeFile(`${output}/results.json`, JSON.stringify(results, null, 2));
  console.log(JSON.stringify(results));
} finally {
  await browser.close();
}
