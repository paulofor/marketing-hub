const { chromium, devices } = require("@playwright/test");
const fs = require("node:fs");
const crypto = require("node:crypto");
const assert = require("node:assert/strict");

// Registra somente a entrada privada e sua identidade publicada; não provisiona sessões ou resultados.
(async () => {
  const [base, slug, destination] = process.argv.slice(2);
  const url = new URL(base);
  assert(
    url.protocol === "https:" &&
      !url.username &&
      !url.password &&
      !url.search &&
      !url.hash,
  );
  assert(/^[a-z0-9-]+$/.test(slug) && destination);
  fs.mkdirSync(destination, { recursive: true });
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  const surfaces = [];
  let runtime;
  try {
    for (const [profile, device] of [
      ["desktop", { viewport: { width: 1440, height: 900 } }],
      ["iphone", devices["iPhone 15 Pro"]],
      ["pixel", devices["Pixel 7"]],
    ]) {
      const context = await browser.newContext(device),
        page = await context.newPage(),
        writes = [];
      page.on("request", (request) => {
        if (request.method() !== "GET") writes.push(request.method());
      });
      const response = await page.goto(url.href, { waitUntil: "networkidle" });
      assert.equal(response.status(), 200);
      const current = {};
      for (const route of [
        "/version-diagnostics.json",
        `/api/pde/agent-validation/v1/products/${slug}/contract`,
      ]) {
        const result = await page.request.get(new URL(route, url).href);
        assert.equal(result.status(), 200);
        current[route] = { status: result.status(), body: await result.json() };
      }
      if (runtime) assert.deepEqual(current, runtime);
      runtime = current;
      const diagnostic = current["/version-diagnostics.json"].body;
      const contract =
        current[`/api/pde/agent-validation/v1/products/${slug}/contract`].body;
      assert.equal(diagnostic.productSlug, slug);
      assert.equal(diagnostic.experienceVersion, contract.prototypeVersion);
      assert.equal(contract.published, false);
      assert.equal(contract.paymentEnabled, false);
      assert.equal(contract.providerCallsAuthorized, 0);
      assert(
        (await page.locator("body").innerText()).includes(
          diagnostic.experienceVersion,
        ),
      );
      const image = await page.screenshot({
        path: `${destination}/${profile}.png`,
        fullPage: true,
      });
      assert.equal(
        writes.length,
        0,
        "A captura privada não pode modificar estado",
      );
      assert(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      );
      surfaces.push({
        profile,
        status: response.status(),
        body: await page.locator("body").innerText(),
        writeCount: writes.length,
        screenshotSha256: crypto
          .createHash("sha256")
          .update(image)
          .digest("hex"),
      });
      await context.close();
    }
  } finally {
    await browser.close();
  }
  fs.writeFileSync(
    `${destination}/runtime.json`,
    JSON.stringify(runtime, null, 2) + "\n",
  );
  fs.writeFileSync(
    `${destination}/surfaces.json`,
    JSON.stringify(surfaces, null, 2) + "\n",
  );
  process.stdout.write(
    "Três perfis capturados; identidade coerente e nenhuma mutação.\n",
  );
})().catch((error) => {
  process.stderr.write(error.message + "\n");
  process.exit(1);
});
