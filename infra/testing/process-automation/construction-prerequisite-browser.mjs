import assert from "node:assert/strict";
import { mkdir, readFile } from "node:fs/promises";
import { createRequire } from "node:module";

const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const { control, history } = JSON.parse(
  await readFile(process.env.CONSTRUCTION_PREREQUISITE_FIXTURE, "utf8"),
);
assert.equal(control.canResume, false);
assert.equal(control.canStart, false);
assert.match(control.reason, /Processo 3 pendente/);
assert.match(
  control.navigationUrl,
  /processes\/117\/activities\?learningCycleId=/,
);
const output = process.env.PROCESS_TEST_ARTIFACTS;
assert(output, "Informe o destino das evidências sintéticas.");
await mkdir(output, { recursive: true });
const base = process.env.PROCESS_FRONTEND_URL || "http://127.0.0.1:4173";
assert(["localhost", "127.0.0.1"].includes(new URL(base).hostname));
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  headless: true,
  args: ["--no-sandbox"],
});
try {
  for (const [name, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const page = await browser.newPage(options);
    const errors = [],
      mutations = [];
    page.on("console", (message) => {
      if (message.type() === "error") console.error(message.text());
    });
    page.on("pageerror", (error) => {
      errors.push(error.message);
      console.error(error.stack);
    });
    await page.routeWebSocket("**/*", (socket) => socket.close());
    await page.route("**/*", (route) => {
      const request = route.request(),
        url = new URL(request.url());
      if (url.pathname.startsWith("/api/")) {
        if (!["GET", "OPTIONS"].includes(request.method()))
          mutations.push(request.url());
        if (url.pathname.endsWith("/activity-executions"))
          return route.fulfill({ json: history });
        if (url.pathname.includes("/automation/v1"))
          return route.fulfill({ json: control });
        if (url.pathname.endsWith("/process-context"))
          return route.fulfill({ status: 204, body: "" });
        if (url.pathname.includes("video-review/summary"))
          return route.fulfill({ json: { humanDecisionRequired: 0 } });
        return route.fulfill({ json: [] });
      }
      if (url.origin !== base) return route.abort();
      return route.continue();
    });
    await page.goto(
      `${base}/products/${control.productId}/value-chain-history/processes/${control.processDefinitionId}/activities?chainId=${control.chainId}&learningCycleId=${control.learningCycleId}&sourceReference=${encodeURIComponent(control.sourceReference)}`,
    );
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    await expect(panel).toContainText(control.reason, { timeout: 20000 });
    await expect(
      panel.getByRole("button", { name: /Retomar processo/ }),
    ).toHaveCount(0);
    await expect(
      panel.getByRole("link", { name: "Abrir pendência", exact: true }),
    ).toHaveAttribute("href", control.navigationUrl);
    await expect(
      panel.getByRole("button", { name: /Iniciar processo/ }),
    ).toHaveCount(0);
    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth + 1,
      ),
      false,
    );
    assert.deepEqual(errors, []);
    assert.deepEqual(mutations, []);
    await panel.screenshot({
      path: `${output}/${name}-construction-prerequisite.png`,
    });
    await page.close();
    console.log(
      `PASS ${name}: causa real, link do Processo 3 e comunicação impedida sem mutações`,
    );
  }
} finally {
  await browser.close();
}
