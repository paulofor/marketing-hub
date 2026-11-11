const { chromium, devices } = require("playwright");
const fs = require("node:fs");
const assert = require("node:assert/strict");
const base = process.env.PLANNING_ALIGNMENT_UI_URL || "http://127.0.0.1:15175";
const api = process.env.PLANNING_ALIGNMENT_API_URL || "http://127.0.0.1:18094";
const output =
  process.env.PLANNING_ALIGNMENT_EVIDENCE_DIR ||
  "artifacts/musa-continuous-offer";
(async () => {
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
    headless: true,
    args: ["--no-sandbox"],
  });
  try {
    for (const [name, device] of [
      ["desktop", { viewport: { width: 1440, height: 1000 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      const context = await browser.newContext(device);
      const page = await context.newPage();
      const errors = [];
      page.on("pageerror", (e) => errors.push(e.message));
      await context.request.post(api + "/__fixtures/reset");
      await page.goto(base + "/e2e/fixtures/planning-alignment/");
      const button = page.getByRole("button", {
        name: "Alinhar à definição registrada",
      });
      await button.waitFor();
      assert.match(
        await page.locator("body").innerText(),
        /Base gratuita e premium opcional/,
      );
      await page.screenshot({
        path: output + "/local-" + name + "-before.png",
        fullPage: true,
      });
      const response = page.waitForResponse(
        (r) =>
          r.url().endsWith("/planning-alignment") &&
          r.request().method() === "POST",
      );
      await button.click();
      assert.equal((await response).status(), 200);
      await page.getByRole("status").filter({ hasText: "Revisão 2" }).waitFor();
      await page.screenshot({
        path: output + "/local-" + name + "-after.png",
        fullPage: true,
      });
      assert.equal(await button.count(), 0);
      assert.deepEqual(errors, []);
      console.log(
        "PASS",
        name,
        "prévia → contrato Java real → revisão 2; sem chamada externa",
      );
      await context.close();
    }
  } finally {
    await browser.close();
  }
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
