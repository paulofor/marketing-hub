import assert from "node:assert/strict";
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const data = JSON.parse(
  await readFile(process.env.VIDEO_PREFLIGHT_FIXTURE_RESULT, "utf8"),
);
const output = process.env.VIDEO_PREFLIGHT_EVIDENCE_DIR;
await mkdir(output, { recursive: true });
const base = "http://127.0.0.1:15173",
  processId = data.processUrl.split("/")[3];
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
    const context = await browser.newContext(device),
      page = await context.newPage(),
      errors = [],
      writes = [],
      external = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/*", async (route) => {
      const request = route.request(),
        url = new URL(request.url());
      if (url.origin !== base) {
        external.push(url.href);
        return route.abort();
      }
      if (request.method() !== "GET") writes.push(url.pathname);
      if (url.pathname === "/api/sales-videos/projects/91004")
        return route.fulfill({
          json: {
            id: 91004,
            productId: 91001,
            experimentId: 91001,
            campaignKey: "fixture-video-finance-v1",
            title: "Vídeo sintético segregado",
            objective: "Primeiro ajuste",
            scriptText: "Aplicar, avaliar e retomar.",
            hookText: "Primeiro ajuste",
            status: "DRAFT",
            targetDurationSeconds: 15,
            videoCategory: "COMMERCIAL_SHORT",
          },
        });
      if (
        url.pathname.startsWith("/api/") &&
        !url.pathname.startsWith(
          "/api/business-process-chains/learning-cycles/v1",
        ) &&
        !url.pathname.startsWith("/api/business-processes/") &&
        !["/api/products", "/api/business-process-chains"].includes(
          url.pathname,
        )
      )
        return route.fulfill({ json: [] });
      return route.continue();
    });
    const processUrl = `${base}/products/91001/value-chain-history/processes/${processId}/activities?learningCycleId=${data.cycle}&chainId=91002`;
    await page.goto(processUrl);
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    await expect(
      panel.getByText("Produção do anúncio bloqueada", { exact: true }),
    ).toBeVisible();
    await expect(panel.getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "0",
    );
    await expect(
      panel.locator(".product-process-situation__running-icon"),
    ).toHaveCount(0);
    await panel.getByText("Ver contexto completo", { exact: true }).click();
    const copied = panel.getByLabel("Contexto completo do processo", {
      exact: true,
    });
    await expect(copied).toContainText("RESOLVE_VIDEO_PREFLIGHT");
    await expect(copied).toContainText(data.action.evidenceReference);
    await panel.getByText("Ver contexto completo", { exact: true }).click();
    const action = panel.getByRole("link", {
      name: "Ver impedimento do vídeo",
      exact: true,
    });
    await expect(action).toHaveAttribute(
      "href",
      "/audio-video-studio/projects/91004",
    );
    await action.scrollIntoViewIfNeeded();
    await page.screenshot({ path: `${output}/${name}-blocked.png` });
    await action.click();
    await expect(page.getByLabel("Roteiro completo")).toHaveValue(
      "Aplicar, avaliar e retomar.",
    );
    assert.equal(
      new URL(page.url()).pathname,
      "/audio-video-studio/projects/91004",
    );
    await page.goBack();
    await expect(
      panel.getByText("Produção do anúncio bloqueada", { exact: true }),
    ).toBeVisible();
    assert.deepEqual(errors, []);
    assert.deepEqual(writes, []);
    assert.deepEqual(external, []);
    results.push({
      device: name,
      blocker: true,
      persistedEvidence: true,
      exactProject: true,
      return: true,
      externalCalls: 0,
    });
    await context.close();
  }
} finally {
  await browser.close();
}
await writeFile(`${output}/results.json`, JSON.stringify(results, null, 2));
console.log(JSON.stringify(results));
