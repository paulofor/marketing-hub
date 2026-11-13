const { chromium, expect } = require("@playwright/test");
const base = "http://127.0.0.1:57188";
const route = "/experiences/musa-consultor-pwa-v1/agent-validation";
const api = "/api/pde/vega/consultor/v1";
(async () => {
  const browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
  });
  try {
    const ctx = await browser.newContext();
    const page = await ctx.newPage();
    const issue = async () => {
      const r = await ctx.request.post(
        base + api + "/internal/cycles/91024/access",
        {
          headers: { "X-PDE-Internal-Token": "vega-local-internal-only" },
          data: {},
        },
      );
      expect(r.ok()).toBeTruthy();
      return r.json();
    };
    const a = await issue();
    const b = await issue();
    await page.goto(base + route + "#access=" + a.accessKey);
    await page.getByRole("checkbox").check();
    await page
      .getByRole("button", { name: "Começar minha orientação" })
      .click();
    await page.getByLabel("Para qual ocasião").fill("almoço");
    await page.getByRole("button", { name: "Próximo", exact: true }).click();
    await page
      .getByLabel("Quais peças")
      .fill("Camisa, lenço e bolsa. Posso usar o lenço no pescoço ou na alça.");
    await page.getByRole("button", { name: "Próximo", exact: true }).click();
    await page
      .getByLabel("O que você prefere")
      .selectOption("Cor e criatividade");
    await page
      .getByRole("button", { name: "Receber orientação", exact: true })
      .click();
    await expect(
      page.getByRole("article", { name: "Resultado da orientação" }),
    ).toBeVisible();
    await page.evaluate((key) => {
      location.hash = "access=" + key;
    }, b.accessKey);
    await expect(page.getByRole("checkbox")).toBeVisible();
    await expect(
      page.getByRole("article", { name: "Resultado da orientação" }),
    ).toHaveCount(0);
    const state = await page.evaluate(
      async (api) => (await fetch(api + "/workspace")).json(),
      api,
    );
    expect(state.sessionId).toBe(b.sessionId);
    expect(Object.keys(state.choices)).toHaveLength(0);
    console.log(
      JSON.stringify({
        status: "PASS",
        sameTabOwnerSwitch: true,
        previousResultRemoved: true,
        otherIdentity: true,
      }),
    );
    await ctx.close();
  } finally {
    await browser.close();
  }
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
