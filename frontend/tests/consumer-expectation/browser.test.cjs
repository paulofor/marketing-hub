const { test } = require("node:test");
const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");
const { chromium, devices } = require("@playwright/test");
const { mkdir } = require("node:fs/promises");

test(
  "Expectativas: pesquisa, alternativas, medicao e segregacao funcionam no fluxo local",
  { timeout: 120000 },
  async () => {
    assert.ok(
      process.env.EXPECTATION_BACKEND_URL,
      "Exige backend local real iniciado pelo teste integrado",
    );
    const server = spawn(
      process.execPath,
      [
        "node_modules/vite/bin/vite.js",
        "--config",
        "tests/consumer-expectation/vite.config.ts",
        "--host",
        "127.0.0.1",
        "--port",
        "5199",
        "--strictPort",
      ],
      { stdio: "pipe", cwd: "frontend" },
    );
    let logs = "";
    server.stderr.on("data", (c) => (logs += c));
    server.stdout.on("data", (c) => (logs += c));
    let browser;
    try {
      let ready = false;
      for (let i = 0; i < 100; i++) {
        try {
          const r = await fetch(
            "http://127.0.0.1:5199/tests/consumer-expectation/",
          );
          if (r.ok) {
            ready = true;
            break;
          }
        } catch {}
        await new Promise((r) => setTimeout(r, 200));
      }
      assert.ok(ready, logs);
      browser = await chromium.launch({
        executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
        args: ["--no-sandbox"],
      });
      await mkdir(".codex/expectation-evidence", { recursive: true });
      for (const [name, options] of [
        ["desktop", { viewport: { width: 1440, height: 1000 } }],
        ["iphone", devices["iPhone 15 Pro"]],
        ["pixel", devices["Pixel 7"]],
      ]) {
        const context = await browser.newContext(options);
        const page = await context.newPage();
        const errors = [];
        page.on("pageerror", (e) => errors.push(e.message));
        await page.route("**/api/**", async (route) => {
          const path = new URL(route.request().url()).pathname;
          assert.ok(
            path.startsWith("/api/consumer-expectations/v1/"),
            "Nenhum endpoint de produção",
          );
          const response = await route.fetch({
            url: process.env.EXPECTATION_BACKEND_URL + path,
          });
          await route.fulfill({ response });
        });
        await page.goto("http://127.0.0.1:5199/tests/consumer-expectation/", {
          waitUntil: "domcontentloaded",
        });
        await page.getByText("Base científica, dimensões e limites").click();
        await page
          .getByRole("link", { name: "Schultz, Dayan e Montague (1997)" })
          .waitFor();
        await page.getByText("Base científica, dimensões e limites").click();
        const fields = {
          audience: "Público sintético " + name,
          context: "Primeira visita mobile",
          expectedExperience: "Montar proposta manualmente",
          desiredOutcome: "Proposta pronta para editar",
          actualDelivery: "Rascunho com campos editáveis",
        };
        for (const [key, value] of Object.entries(fields))
          await page.locator("#expectation-" + key).fill(value);
        for (const surface of name === "desktop"
          ? ["AD", "OFFER", "PRODUCT", "DIGITAL_EXPERIENCE"]
          : ["AD"]) {
          await page.locator("#expectation-surface").selectOption(surface);
          await page
            .getByRole("button", { name: "Preparar três alternativas" })
            .click();
          await page
            .getByRole("heading", { name: "Hipóteses para revisão" })
            .waitFor();
          assert.equal(
            await page
              .getByRole("heading", {
                name: /Controle familiar|Descoberta guiada|Quebra de expectativa explicada/,
              })
              .count(),
            3,
          );
          assert.match(
            await page.locator("body").innerText(),
            /falta prova funcional/,
          );
        }
        await page.locator("#expectation-product").fill("502");
        await page.locator("#expectation-cycle").fill("501");
        await page
          .getByRole("button", { name: "Consultar aprendizado" })
          .click();
        await page
          .getByText("Não há medição conciliada aplicável.", { exact: false })
          .waitFor();
        await page.locator("#expectation-product").fill("702");
        await page
          .getByRole("button", { name: "Consultar aprendizado" })
          .click();
        await page
          .getByRole("alert")
          .filter({ hasText: "ciclo não encontrado" })
          .waitFor();
        assert.ok(
          await page.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth + 1,
          ),
          "Sem rolagem horizontal",
        );
        assert.deepEqual(errors, []);
        await page.screenshot({
          path: `.codex/expectation-evidence/${name}.png`,
          fullPage: true,
        });
        await context.close();
      }
    } catch (error) {
      error.message += "\nVite: " + logs;
      throw error;
    } finally {
      await browser?.close();
      server.kill("SIGTERM");
    }
  },
);
