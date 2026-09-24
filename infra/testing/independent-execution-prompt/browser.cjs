// Homologa a cópia no build real com contratos isolados e nenhuma escrita externa.
const { chromium, devices, expect } = require("@playwright/test");
const { readFile, writeFile, mkdir } = require("node:fs/promises");
const { createServer } = require("node:http");
const path = require("node:path");
const assert = require("node:assert/strict");
const fixture = require("./fixture.json");

async function paste(page) {
  await page.evaluate(() => {
    const field = document.createElement("textarea");
    field.id = "paste-test";
    field.style.cssText = "position:fixed;top:0;left:0;z-index:99999";
    document.body.appendChild(field);
    field.focus({ preventScroll: true });
  });
  await page.keyboard.press("Control+V");
  const field = page.locator("#paste-test");
  await expect(field).not.toHaveValue("");
  const text = await field.inputValue();
  await field.evaluate((element) => element.remove());
  return text;
}

(async () => {
  const output = path.resolve(
    process.env.PROMPT_TEST_OUTPUT || "artifacts/independent-execution-prompt",
  );
  const uiDir = path.resolve("frontend/dist");
  await mkdir(output, { recursive: true });
  const server = createServer(async (req, res) => {
    try {
      const pathname = new URL(req.url, "http://localhost").pathname;
      const file =
        pathname.startsWith("/assets/") || pathname === "/favicon.ico"
          ? path.join(uiDir, pathname)
          : path.join(uiDir, "index.html");
      assert(file.startsWith(uiDir + path.sep));
      res.setHeader(
        "Content-Type",
        file.endsWith(".js")
          ? "application/javascript"
          : file.endsWith(".css")
            ? "text/css"
            : file.endsWith(".ico")
              ? "image/x-icon"
              : "text/html",
      );
      res.end(await readFile(file));
    } catch {
      res.statusCode = 404;
      res.end();
    }
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const port = server.address().port;
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
    headless: true,
    args: [
      "--no-sandbox",
      "--no-proxy-server",
      "--host-resolver-rules=MAP independent-prompt.test 127.0.0.1",
    ],
  });
  const results = [];
  try {
    for (const [profileName, profile] of [
      ["desktop", { viewport: { width: 1366, height: 900 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      for (const secure of [false, true]) {
        const name = `${profileName}-${secure ? "secure" : "http"}`;
        const origin = `http://${secure ? "127.0.0.1" : "independent-prompt.test"}:${port}`;
        const context = await browser.newContext(profile);
        if (secure)
          await context.grantPermissions(
            ["clipboard-read", "clipboard-write"],
            { origin },
          );
        const page = await context.newPage();
        const errors = [];
        let mutations = 0;
        let apiFailure = false;
        page.on("pageerror", (error) => errors.push(error.message));
        await page.route("**/*", async (route) => {
          const request = route.request();
          const url = new URL(request.url());
          if (request.method() !== "GET") {
            mutations++;
            return route.abort();
          }
          if (url.pathname.startsWith("/api/")) {
            if (
              url.pathname.startsWith(
                "/api/independent-business-process-executions/",
              )
            ) {
              if (apiFailure)
                return route.fulfill({
                  status: 503,
                  json: { message: "Falha simulada" },
                });
              const data = structuredClone(fixture);
              if (url.pathname.endsWith("/91002")) {
                data.execution.id = 91002;
                data.execution.sourceReference = "research:93002";
                data.execution.status = "COMPLETED";
                data.activities = [];
              }
              return route.fulfill({ json: data });
            }
            return route.fulfill({ json: [] });
          }
          if (url.origin !== origin) return route.abort();
          return route.continue();
        });
        await page.goto(`${origin}/business-process-executions/91001`);
        const button = page.getByRole("button", {
          name: "Prompt para AIHUB",
          exact: true,
        });
        await expect(button).toBeEnabled();
        await button.scrollIntoViewIfNeeded();
        await page.screenshot({
          path: path.join(output, `${name}-button.png`),
        });
        const box = await button.boundingBox();
        assert(box.width >= 44 && box.height >= 44);
        assert(box.x >= 0 && box.x + box.width <= page.viewportSize().width);
        await button.click();
        await expect(
          page.getByText("Prompt copiado! Cole na conversa do AIHUB.", {
            exact: true,
          }),
        ).toBeVisible();
        const copied = await paste(page);
        assert(copied.includes("Execução: #91001"));
        assert(copied.includes("Tarefa #94001"));
        assert(copied.includes("Custo estimado da execução: Não informado"));
        assert(copied.includes("Execução independente, sem produto definido"));
        assert(copied.includes("cinco pontos em toda oferta"));
        assert(
          copied.includes(
            "criar ou atualizar manualmente os Pull Requests necessários",
          ),
        );
        assert(!/PRIVATE_|INTERNAL_REQUEST_KEY/.test(copied));
        assert.equal(
          (copied.match(/CONTEXTO DA EXECUÇÃO INDEPENDENTE/g) || []).length,
          1,
        );
        await page.getByText("Ver prompt para AIHUB", { exact: true }).click();
        const preview = page.getByLabel("Prompt completo para AIHUB", {
          exact: true,
        });
        await expect(preview).toHaveText(copied, { useInnerText: false });
        assert.equal(await preview.textContent(), copied);
        await preview.scrollIntoViewIfNeeded();
        await page.screenshot({
          path: path.join(output, `${name}-preview.png`),
        });
        await writeFile(path.join(output, `${name}-copied.txt`), copied);
        await page.evaluate(() => {
          Object.defineProperty(navigator, "clipboard", {
            configurable: true,
            value: { writeText: () => Promise.reject(new Error("negado")) },
          });
          document.execCommand = () => false;
        });
        await button.click();
        const manual = page.getByRole("textbox", {
          name: "Prompt para AIHUB para copiar manualmente",
          exact: true,
        });
        await expect(manual).toHaveValue(copied);
        await manual.focus();
        assert.equal(
          await manual.evaluate((element) => element.selectionEnd),
          copied.length,
        );
        await page.evaluate(() => {
          history.pushState({}, "", "/business-process-executions/91002");
          dispatchEvent(new PopStateEvent("popstate"));
        });
        await expect(
          page.getByRole("heading", {
            name: "Detalhe da execução #91002",
            exact: true,
          }),
        ).toBeVisible();
        await expect(manual).toHaveCount(0);
        await page.getByText("Ver prompt para AIHUB", { exact: true }).click();
        await expect(preview).toContainText("Execução: #91002");
        const next = await preview.textContent();
        assert(
          !/Execução: #91001|Tarefa #94001|product-discovery-cycle:93001/.test(
            next,
          ),
        );
        apiFailure = true;
        await page.reload();
        await expect(
          page.getByText("Não foi possível detalhar a execução.", {
            exact: true,
          }),
        ).toBeVisible({ timeout: 20000 });
        await expect(button).toHaveCount(0);
        assert.equal(mutations, 0);
        assert.deepEqual(errors, []);
        results.push({
          profile: name,
          checks: [
            "layout",
            "real-copy",
            "preview",
            "manual-fallback",
            "navigation",
            "api-failure",
          ],
          mutations,
          pageErrors: errors,
        });
        await context.close();
      }
    }
    await writeFile(
      path.join(output, "results.json"),
      JSON.stringify(results, null, 2),
    );
    console.log(JSON.stringify(results));
  } finally {
    await browser.close();
    await new Promise((resolve) => server.close(resolve));
  }
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
