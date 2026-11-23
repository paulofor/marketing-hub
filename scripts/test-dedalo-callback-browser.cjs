const assert = require("node:assert/strict");
const fs = require("node:fs/promises");
const path = require("node:path");
const { chromium, devices } = require("playwright");
const [base, fixtureDir, output] = process.argv.slice(2);
if (!/^http:\/\/127\.0\.0\.1:\d+$/.test(base || "") || !fixtureDir || !output)
  throw new Error(
    "Informe URL local, fixtures sintéticas do backend e diretório de evidências.",
  );
(async () => {
  await fs.mkdir(output, { recursive: true });
  const task = JSON.parse(
    await fs.readFile(path.join(fixtureDir, "rejected-task.json"), "utf8"),
  );
  const play = JSON.parse(
    await fs.readFile(path.join(fixtureDir, "in-play-92004.json"), "utf8"),
  );
  const browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  const results = [];
  try {
    for (const device of ["desktop", "iPhone 15 Pro", "Pixel 7"]) {
      const context = await browser.newContext(
        device === "desktop"
          ? { viewport: { width: 1440, height: 1000 } }
          : devices[device],
      );
      let calls = 0,
        recovered = false;
      await context.route("**/*", async (route) => {
        const req = route.request(),
          url = new URL(req.url());
        if (!url.pathname.startsWith("/api/"))
          return url.origin === base ? route.continue() : route.abort();
        let data = [];
        if (req.method() === "POST") {
          assert.equal(
            url.pathname,
            `/api/agent-tasks/${task.id}/replay-result`,
          );
          calls++;
          await new Promise((r) => setTimeout(r, 200));
          if (calls === 1)
            return route.fulfill({
              status: 422,
              contentType: "application/json",
              body: JSON.stringify({
                code: "AGENT_RESULT_CONTRACT_REJECTED",
                message: "O contrato ainda precisa de correção.",
              }),
            });
          recovered = true;
          return route.fulfill({ status: 204 });
        }
        if (url.pathname === "/api/agent-tasks/active")
          data = recovered ? [] : [task];
        else if (url.pathname === "/api/products/operations/v1/in-play")
          data = play;
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(data),
        });
      });
      const page = await context.newPage();
      await page.goto(base + "/agent-tasks", { waitUntil: "domcontentloaded" });
      await page
        .getByRole("button", { name: "Revalidar retorno original" })
        .waitFor();
      assert.ok(
        (await page.locator("body").innerText()).includes(
          "não comprova implementação",
        ),
      );
      await page
        .getByRole("button", { name: "Revalidar retorno original" })
        .click();
      await page
        .getByRole("alert")
        .filter({ hasText: "O retorno permanece preservado" })
        .waitFor();
      const text = await page.locator("body").innerText();
      assert.ok(
        !text.includes("AGENT_RESULT_CONTRACT_REJECTED") &&
          !text.includes("IllegalArgumentException"),
      );
      await page.screenshot({
        path: path.join(output, device.replaceAll(" ", "-") + "-rejected.png"),
        fullPage: true,
      });
      await page
        .getByRole("button", { name: "Revalidar retorno original" })
        .click();
      await page.getByText("Nenhuma tarefa ativa.", { exact: true }).waitFor();
      assert.equal(calls, 2);
      await page.goto(base + "/products/in-play", {
        waitUntil: "domcontentloaded",
      });
      await page
        .getByRole("heading", { name: "QA atual", exact: true })
        .waitFor();
      assert.ok(
        (await page.locator(".products-play-card").innerText()).includes(
          "Dédalo",
        ),
      );
      const href = await page
        .getByRole("link", { name: /Ver atividade/ })
        .getAttribute("href");
      assert.ok(href.endsWith("#activity-deliverables"));
      await page.screenshot({
        path: path.join(output, device.replaceAll(" ", "-") + "-current.png"),
        fullPage: true,
      });
      results.push({
        device,
        replayPosts: calls,
        noRawError: true,
        currentResponsible: "Dédalo",
        synthetic: true,
      });
      await context.close();
    }
  } finally {
    await browser.close();
  }
  await fs.writeFile(
    path.join(output, "results.json"),
    JSON.stringify(results, null, 2) + "\n",
  );
  console.log(JSON.stringify(results));
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
