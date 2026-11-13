const { chromium, devices, expect } = require("@playwright/test");
const fs = require("fs");
const path = require("path");
const url =
  process.env.MUSA_TEST_URL ||
  "http://127.0.0.1:57188/experiences/musa-consultor-pwa-v1/agent-validation";
if (!/^http:\/\/127\.0\.0\.1:57188\//.test(url))
  throw new Error("Este harness aceita somente a topologia local isolada.");
const output =
  process.env.MUSA_TEST_OUTPUT || "artifacts/musa-private-20261113/browser";
fs.mkdirSync(output, { recursive: true });
const api = "/api/pde/vega/consultor/v1";
const pieces =
  "Camisa, calça, lenço e bolsa. Posso usar o lenço no pescoço ou na alça da bolsa.";
let browser;
(async () => {
  browser = await chromium.launch({ executablePath: "/usr/bin/chromium" });
  const reports = [];
  const profiles = [
    ["DESKTOP_1440", { viewport: { width: 1440, height: 1000 } }],
    ["IPHONE_15_PRO", devices["iPhone 15 Pro"]],
    ["PIXEL_7", devices["Pixel 7"]],
  ];
  async function create(options) {
    const ctx = await browser.newContext(options);
    const page = await ctx.newPage();
    await page.route("**/internal/cycles/*/access", (route) =>
      route.continue({
        headers: {
          ...route.request().headers(),
          "X-PDE-Internal-Token": "vega-local-internal-only",
        },
      }),
    );
    await page.goto(url);
    await page.getByText("Operação da homologação").click();
    await page.getByLabel("Ciclo", { exact: true }).fill("91022");
    await page.getByRole("button", { name: "Emitir acesso privado" }).click();
    const link = await page
      .getByRole("link", { name: "Abrir acesso emitido" })
      .getAttribute("href");
    await page.getByRole("link", { name: "Abrir acesso emitido" }).click();
    await page.getByRole("checkbox").check();
    await page
      .getByRole("button", { name: "Começar minha orientação" })
      .click();
    await expect(page.getByText("Passo 1 de 3")).toBeVisible();
    return { ctx, page, link };
  }
  async function fill(page, occasion = "almoço", selection = pieces) {
    await page.getByLabel("Para qual ocasião").fill(occasion);
    await page.getByRole("button", { name: "Próximo", exact: true }).click();
    await page.getByLabel("Quais peças").fill(selection);
    await page.getByRole("button", { name: "Próximo", exact: true }).click();
    await page
      .getByLabel("O que você prefere")
      .selectOption("Cor e criatividade");
    await page
      .getByRole("button", { name: "Receber orientação", exact: true })
      .click();
  }
  async function workspace(page) {
    return page.evaluate(async (api) => {
      const r = await fetch(api + "/workspace");
      return r.json();
    }, api);
  }
  for (const [device, options] of profiles)
    for (const scenario of ["ADHERENT", "RECOVERY", "SAFETY"]) {
      if (
        process.env.MUSA_CASES &&
        !process.env.MUSA_CASES.split(",").includes(`${device}-${scenario}`)
      )
        continue;
      const started = Date.now();
      const { ctx, page, link } = await create(options);
      let extra;
      try {
        if (scenario === "ADHERENT") {
          await fill(page);
          await expect(
            page.getByRole("article", { name: "Resultado da orientação" }),
          ).toContainText("alça");
          await page
            .getByRole("button", { name: "Salvar minha escolha", exact: true })
            .click();
          await expect(
            page.getByRole("button", { name: "Escolha salva", exact: true }),
          ).toBeVisible();
          await page.reload();
          await page
            .getByRole("button", { name: "Adaptar para outra ocasião" })
            .click();
          await page
            .getByLabel("Para qual ocasião")
            .fill("videochamada de trabalho");
          await page
            .getByRole("button", { name: "Próximo", exact: true })
            .click();
          await expect(page.getByLabel("Quais peças")).toHaveValue(pieces);
          await page
            .getByRole("button", { name: "Próximo", exact: true })
            .click();
          await page
            .getByRole("button", { name: "Receber orientação", exact: true })
            .click();
          await expect(
            page.getByRole("article", { name: "Resultado da orientação" }),
          ).toContainText("lenço no pescoço");
          await page
            .getByRole("button", { name: "Salvar minha escolha", exact: true })
            .click();
          await expect(
            page.getByRole("button", { name: "Adaptar para outra ocasião" }),
          ).toHaveCount(2);
          await page
            .getByRole("button", { name: "Usar esta orientação" })
            .click();
          await expect(page.getByRole("alert")).toContainText("ação de uso");
          const state = await workspace(page);
          expect(Object.keys(state.choices)).toHaveLength(2);
          expect(
            Object.values(state.choices).some(
              (x) => x.answers.occasion === "almoço",
            ),
          ).toBeTruthy();
          await page.getByRole("button", { name: "Sair", exact: true }).click();
          await page.goto(link);
          await expect(
            page.getByRole("button", { name: "Adaptar para outra ocasião" }),
          ).toHaveCount(2);
        } else if (scenario === "RECOVERY") {
          await fill(page, "passeio", "camisa e calça");
          await expect(page.getByText("Vamos ajustar a entrada")).toBeVisible();
          await page
            .getByRole("button", { name: "Revisar minhas peças" })
            .click();
          await expect(page.getByLabel("Quais peças")).toHaveValue(
            "camisa e calça",
          );
          await page.getByLabel("Quais peças").fill(pieces);
          await page
            .getByRole("button", { name: "Voltar", exact: true })
            .click();
          await page.getByLabel("Para qual ocasião").fill("almoço");
          await page
            .getByRole("button", { name: "Próximo", exact: true })
            .click();
          await page
            .getByRole("button", { name: "Próximo", exact: true })
            .click();
          let lost = false;
          await page.route("**/commands", async (route) => {
            const body = route.request().postDataJSON();
            if (body.action === "generate" && !lost) {
              lost = true;
              await route.fetch();
              await route.abort("connectionreset");
            } else await route.continue();
          });
          await page
            .getByRole("button", { name: "Receber orientação", exact: true })
            .click();
          await expect(
            page.getByRole("button", {
              name: "Salvar minha escolha",
              exact: true,
            }),
          ).toBeVisible();
          await page
            .getByRole("button", { name: "Salvar minha escolha", exact: true })
            .click();
          await page.reload();
          await expect(
            page.getByRole("button", { name: "Adaptar para outra ocasião" }),
          ).toHaveCount(1);
          const state = await workspace(page);
          expect(state.draft.existingSelection).toBe(pieces);
        } else {
          await fill(page, "passeio", "camisa e calça");
          await expect(page.getByText("Vamos ajustar a entrada")).toBeVisible();
          expect(Object.keys((await workspace(page)).choices)).toHaveLength(0);
          extra = await create(options);
          await fill(extra.page);
          await extra.page
            .getByRole("button", { name: "Salvar minha escolha", exact: true })
            .click();
          await expect(
            extra.page.getByRole("button", {
              name: "Escolha salva",
              exact: true,
            }),
          ).toBeVisible();
          const other = await workspace(extra.page);
          const key = await extra.page.evaluate(async (api) => {
            const w = await (await fetch(api + "/workspace")).json();
            return w.revision;
          }, api);
          const foreign = await page.evaluate(
            async ({ api, other, key }) => {
              const r = await fetch(
                api + "/operations/" + Object.keys(other.choices)[0],
              );
              return r.status;
            },
            { api, other, key },
          );
          expect(foreign).not.toBe(200);
          await page.unroute("**/internal/cycles/*/access");
          const leak = await page.evaluate(async (api) => {
            const r = await fetch(api + "/internal/cycles/91022/access", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: "{}",
            });
            return r.status;
          }, api);
          expect(leak).toBe(401);
        }
        await page.screenshot({
          path: path.join(output, `${device}-${scenario}.png`),
          fullPage: true,
        });
        expect(
          await page.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth,
          ),
        ).toBeTruthy();
        await page.evaluate(() => (document.documentElement.style.zoom = "2"));
        expect(
          await page.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth,
          ),
        ).toBeTruthy();
        await page.evaluate(() => (document.documentElement.style.zoom = "1"));
        const w = await workspace(page);
        reports.push({
          device,
          scenario,
          status: "PASS",
          elapsedMs: Date.now() - started,
          sessionId: w.sessionId,
          choices: Object.keys(w.choices).length,
          prototypeVersion: "musa-consultor-pwa-v1",
          providerCalls: 0,
          trafficClass: "AGENT_VALIDATION",
          commercialEvidenceClaimed: false,
        });
        fs.writeFileSync(
          path.join(output, `${device}-${scenario}.json`),
          JSON.stringify(reports.at(-1), null, 2),
        );
      } finally {
        if (extra) await extra.ctx.close();
        await ctx.close();
      }
    }
  await browser.close();
  fs.writeFileSync(
    path.join(output, "report.json"),
    JSON.stringify(
      {
        technicalMatrixRuns: 1,
        inputComparisonApplicable: false,
        results: reports,
      },
      null,
      2,
    ),
  );
  console.log(JSON.stringify({ passed: reports.length, providerCalls: 0 }));
})()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(async () => {
    await browser?.close();
  });
