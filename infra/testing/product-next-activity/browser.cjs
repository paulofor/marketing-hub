// Exercita cards e telas reais com contratos locais; bloqueia qualquer escrita ou serviço externo.
const { chromium, devices, expect } = require("@playwright/test");
const { readFile, mkdir, writeFile } = require("node:fs/promises");
const { createServer } = require("node:http");
const path = require("node:path");
const assert = require("node:assert/strict");

(async () => {
  const root = path.resolve("infra/testing/vega-cycle-card/fixtures");
  const output = path.resolve(
    process.env.NEXT_ACTIVITY_OUTPUT ||
      "artifacts/product-next-activity/browser",
  );
  const uiDir = path.resolve("frontend/dist");
  const readJson = async (file) => JSON.parse(await readFile(file, "utf8"));
  const vega = await readJson(path.join(root, "position.json"));
  const context = await readJson(path.join(root, "context.json"));
  const activities = await readJson(path.join(root, "activities.json"));
  const rigelActivities = await readJson(
    "infra/testing/product-next-activity/rigel-activities.json",
  );
  context.productVersion = "musa-pde-entry-v9-primeiro-ajuste-aplicavel";
  context.nextWork = {
    ...context.nextWork,
    activityId: "psiqueAdherent",
    activityNumber: 7,
    activityName: "Psique · cenário aderente",
    state: "IN_PROGRESS",
    reason: "Atividade em execução pelo responsável.",
    url: "/products/4/value-chain-history/processes/70/activities?learningCycleId=2&chainId=14#activity-psiqueAdherent",
  };
  Object.assign(activities, {
    operationalState: "IN_PROGRESS",
    currentActivityId: "psiqueAdherent",
    currentActivityName: context.nextWork.activityName,
    currentActivityState: "IN_PROGRESS",
    currentActivityStateReason: context.nextWork.reason,
  });
  const target = activities.activities.find(
    (item) => item.activityId === "psiqueAdherent",
  );
  assert(target, "A fixture deve conter a atividade de destino.");
  Object.assign(target, {
    operationalState: "IN_PROGRESS",
    stateReason: context.nextWork.reason,
  });
  const rigel = {
    ...vega,
    productId: 9,
    processMeasurements: [],
    subprocessPosition: null,
  };
  const products = [
    {
      id: 4,
      name: "Vega · QA local",
      internalName: "Vega",
      productTypeInternalName: "Opala",
      slug: "metodo-musa-7-dias",
      commercialStatus: "ATIVO",
      automaticExecutionEnabled: true,
      automaticExecutionStatus: "PLAY",
      currentPriceBrl: 67,
      primaryHypothesis:
        "Melhorar o primeiro resultado útil e medir a continuidade até compra.",
    },
    {
      id: 9,
      name: "Rigel · QA local",
      internalName: "Rigel",
      productTypeInternalName: "Opala",
      slug: "kit-whatsapp-pronto",
      commercialStatus: "ATIVO",
      automaticExecutionEnabled: true,
      automaticExecutionStatus: "PLAY",
      currentPriceBrl: 349,
    },
  ];
  await mkdir(output, { recursive: true });
  const server = createServer(async (req, res) => {
    try {
      const name = new URL(req.url, "http://localhost").pathname;
      const file = name.startsWith("/assets/")
        ? path.join(uiDir, name)
        : path.join(uiDir, "index.html");
      assert(file.startsWith(uiDir + path.sep));
      const body = await readFile(file);
      res.setHeader(
        "Content-Type",
        file.endsWith(".js")
          ? "application/javascript"
          : file.endsWith(".css")
            ? "text/css"
            : "text/html",
      );
      res.end(body);
    } catch {
      res.writeHead(404);
      res.end();
    }
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const origin = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
    headless: true,
    args: ["--no-sandbox"],
  });
  const results = [];
  try {
    for (const [device, profile] of [
      ["desktop", { viewport: { width: 1440, height: 1000 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      const session = await browser.newContext(profile);
      const page = await session.newPage();
      const errors = [],
        unexpected = [],
        requests = [];
      let mode = "success";
      const card = (product) =>
        page.getByRole("region", {
          name: `Posição de ${product} · QA local na cadeia de valor`,
        });
      page.on("pageerror", (error) => errors.push(error.message));
      await page.route("**/*", async (route) => {
        const request = route.request();
        const url = new URL(request.url());
        if (url.pathname.startsWith("/ws")) return route.abort();
        if (url.pathname.startsWith("/api/")) {
          requests.push({
            method: request.method(),
            path: url.pathname,
            query: url.search,
          });
          if (request.method() !== "GET") {
            unexpected.push(`${request.method()} ${url.pathname}`);
            return route.abort();
          }
          let data;
          if (url.pathname === "/api/products") data = products;
          else if (url.pathname === "/api/products/value-chain-positions")
            data = [vega, rigel];
          else if (url.pathname === "/api/products/value-chain-positions/4")
            data = vega;
          else if (url.pathname === "/api/products/value-chain-positions/9")
            data = rigel;
          else if (url.pathname.endsWith("/process-context")) {
            if (url.pathname.includes("/products/4/")) {
              assert.equal(url.searchParams.get("cycleId"), "2");
              assert.equal(url.searchParams.get("chainId"), "14");
              data = structuredClone(context);
              if (mode === "blocked")
                Object.assign(data.nextWork, {
                  state: "BLOCKED",
                  reason:
                    "Pendência simulada: revisar a evidência antes de continuar.",
                });
              if (mode === "cycle-step")
                Object.assign(data, {
                  stageLabel: "Decisão comercial",
                  nextWork: null,
                });
            } else {
              assert(url.pathname.includes("/products/9/"));
              assert.equal(url.searchParams.get("cycleId"), null);
              data = null;
            }
          } else if (
            url.pathname ===
            "/api/business-processes/70/products/4/activity-executions"
          ) {
            assert.equal(url.searchParams.get("learningCycleId"), "2");
            assert.equal(url.searchParams.get("chainId"), "14");
            data = activities;
          } else if (
            url.pathname ===
            "/api/business-processes/75/products/9/activity-executions"
          ) {
            assert.equal(url.searchParams.get("learningCycleId"), null);
            assert.equal(url.searchParams.get("chainId"), "14");
            if (mode === "error")
              return route.fulfill({
                status: 503,
                contentType: "application/json",
                body: '{"message":"Falha simulada local"}',
              });
            data = structuredClone(rigelActivities);
            if (mode === "mismatch") data.productId = 4;
            if (mode === "complete")
              Object.assign(data, {
                currentActivityId: null,
                objectiveAchieved: true,
              });
            if (mode === "updated") {
              Object.assign(data, {
                currentActivityId: "delivery",
                currentActivityName:
                  "Entregar cada venda e acompanhar satisfação",
              });
              Object.assign(data.activities[0], {
                activityId: "delivery",
                activityName: data.currentActivityName,
                sequenceNumber: 2,
              });
            }
            if (mode === "slow")
              await new Promise((resolve) => setTimeout(resolve, 600));
          } else if (url.pathname.endsWith("/execution-progress")) data = [];
          else if (
            [
              "/api/creatives/video-review",
              "/api/ops-monitor/v1/modules/availability",
              "/api/facebook/configuration-status",
            ].includes(url.pathname)
          )
            data = [];
          else {
            unexpected.push(url.pathname);
            return route.abort();
          }
          return route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(data),
          });
        }
        if (url.origin !== origin) {
          unexpected.push(url.href);
          return route.abort();
        }
        return route.continue();
      });

      for (const [surface, pathname] of [
        ["home", "/"],
        ["catalog", "/products"],
      ]) {
        await page.goto(origin + pathname, { waitUntil: "domcontentloaded" });
        await expect(
          card("Vega").getByText("3.7 — Psique · cenário aderente", {
            exact: true,
          }),
        ).toBeVisible();
        await expect(
          card("Vega").getByRole("link", { name: "Acompanhar atividade" }),
        ).toHaveAttribute("href", context.nextWork.url);
        await expect(
          card("Rigel").getByRole("link", { name: "Abrir próxima atividade" }),
        ).toHaveAttribute(
          "href",
          "/products/9/value-chain-history/processes/75/activities?chainId=14#activity-optimization",
        );
        await expect(
          card("Rigel").getByText("Responsável: Hermes"),
        ).toBeVisible();
        await expect(card("Rigel").getByText(/Experimento #92/)).toHaveCount(0);
        assert.equal(
          await page.evaluate(
            () => document.documentElement.scrollWidth > innerWidth + 1,
          ),
          false,
        );
        await card("Vega").scrollIntoViewIfNeeded();
        await page.screenshot({
          path: path.join(output, `${device}-${surface}.png`),
        });
        for (const product of ["Vega", "Rigel"]) {
          const link = card(product).getByRole("link", {
            name:
              product === "Vega"
                ? "Acompanhar atividade"
                : "Abrir próxima atividade",
          });
          const box = await link.boundingBox();
          assert(
            box.height >= 44,
            "O link deve oferecer alvo de toque de pelo menos 44px.",
          );
          const href = await link.getAttribute("href");
          await card(product).screenshot({
            path: path.join(output, `${device}-${surface}-${product}.png`),
          });
          if (device === "desktop") {
            await link.focus();
            await page.keyboard.press("Enter");
          } else await link.click();
          await expect(page).toHaveURL(origin + href);
          const activity = page.locator(
            product === "Vega"
              ? "#activity-psiqueAdherent"
              : "#activity-optimization",
          );
          await expect(activity).toBeVisible();
          await expect(activity).toBeInViewport();
          if (product === "Vega") {
            await expect(
              page.getByRole("heading", {
                name: "2º ciclo de vendas · Experimento #92",
              }),
            ).toBeAttached();
            await expect(
              page.getByText(context.previousLearning[2].learning, {
                exact: true,
              }),
            ).toBeAttached();
          } else
            await expect(
              page.getByText("2º ciclo de vendas · Experimento #92"),
            ).toHaveCount(0);
          await page.screenshot({
            path: path.join(
              output,
              `${device}-${surface}-${product}-destination.png`,
            ),
          });
          await page.goBack({ waitUntil: "domcontentloaded" });
          await expect(
            card(product).getByRole("link", {
              name:
                product === "Vega"
                  ? "Acompanhar atividade"
                  : "Abrir próxima atividade",
            }),
          ).toBeVisible();
          results.push({
            device,
            surface,
            product,
            navigation: "PASS",
            writes: 0,
          });
        }
        await card("Vega")
          .locator("summary")
          .filter({ hasText: "Aprendizado dos ciclos anteriores" })
          .click();
        await expect(
          card("Vega").getByText(context.previousLearning[2].learning, {
            exact: true,
          }),
        ).toBeVisible();
      }

      mode = "error";
      await page.goto(origin, { waitUntil: "domcontentloaded" });
      await expect(card("Rigel").getByRole("alert")).toBeVisible({
        timeout: 20000,
      });
      await expect(
        card("Rigel").getByRole("link", { name: "Abrir próxima atividade" }),
      ).toHaveCount(0);
      mode = "slow";
      await card("Rigel")
        .getByRole("button", { name: "Tentar novamente" })
        .click();
      await expect(
        card("Rigel").getByRole("button", { name: "Consultando..." }),
      ).toBeDisabled();
      await expect(
        card("Rigel").getByRole("link", { name: "Abrir próxima atividade" }),
      ).toBeVisible();
      for (const variant of [
        "mismatch",
        "complete",
        "blocked",
        "cycle-step",
        "updated",
      ]) {
        mode = variant;
        await page.goto(origin, { waitUntil: "domcontentloaded" });
        if (variant === "mismatch")
          await expect(card("Rigel").getByRole("alert")).toBeVisible();
        if (variant === "complete")
          await expect(
            card("Rigel").getByRole("link", {
              name: "Ver continuidade na cadeia",
            }),
          ).toBeVisible();
        if (variant === "blocked") {
          await expect(
            card("Vega").getByRole("link", {
              name: "Ver atividade e pendência",
            }),
          ).toBeVisible();
          await expect(
            card("Vega").getByText(
              "Pendência simulada: revisar a evidência antes de continuar.",
            ),
          ).toBeVisible();
        }
        if (variant === "cycle-step")
          await expect(
            card("Vega").getByRole("link", { name: "Abrir etapa do ciclo" }),
          ).toHaveAttribute("href", context.cycleUrl);
        if (variant === "updated")
          await expect(
            card("Rigel").getByRole("link", {
              name: "Abrir próxima atividade",
            }),
          ).toHaveAttribute(
            "href",
            "/products/9/value-chain-history/processes/75/activities?chainId=14#activity-delivery",
          );
      }
      assert.deepEqual(errors, [], "Nenhum erro de renderização.");
      assert.deepEqual(
        unexpected,
        [],
        "Nenhuma integração externa ou escrita.",
      );
      await writeFile(
        path.join(output, `${device}-requests.json`),
        JSON.stringify(requests, null, 2),
      );
      results.push({
        device,
        statesAndRecovery: "PASS",
        errors: 0,
        unexpectedRequests: 0,
      });
      await session.close();
    }
    await writeFile(
      path.join(output, "results.json"),
      JSON.stringify(results, null, 2),
    );
    console.log(
      JSON.stringify(
        { status: "PASS", controls: results.length, results },
        null,
        2,
      ),
    );
  } finally {
    await browser.close();
    await new Promise((resolve) => server.close(resolve));
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
