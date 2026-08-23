const { chromium, devices } = require("playwright");

const baseUrl =
  process.env.PRODUCT_TYPE_E2E_BASE_URL || "http://127.0.0.1:4173";

const productTypes = [
  {
    id: 1,
    code: "PDE",
    name: "PDE - Produto Digital Experiencial",
    description: "Jornada de valor observável.",
    aliases: ["PDE", "Experiência guiada"],
    status: "ACTIVE",
    productCount: 4,
  },
  {
    id: 2,
    code: "IMMERSIVE_PRODUCT",
    name: "Produto imersivo",
    description: "Proposta ainda em avaliação.",
    aliases: ["Experiência imersiva"],
    status: "PROPOSED",
    productCount: 0,
  },
];

async function mockApi(page) {
  await page.route(
    (url) => url.pathname.startsWith("/api/"),
    async (route) => {
      const request = route.request();
      const url = new URL(request.url());
      if (url.pathname === "/api/product-types") {
        const response =
          request.method() === "POST"
            ? {
                ...JSON.parse(request.postData() || "{}"),
                id: 3,
                code: "EXPERIENCIA_POR_MISSOES",
                productCount: 0,
              }
            : productTypes;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(response),
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: "[]",
      });
    },
  );
}

async function assertVisible(page, selector, message) {
  try {
    await page
      .locator(selector)
      .first()
      .waitFor({ state: "visible", timeout: 5000 });
  } catch {
    const visibleText = (await page.locator("body").innerText()).slice(0, 1000);
    throw new Error(`${message}. Conteúdo visível: ${visibleText}`);
  }
}

async function runScenario(browser, scenario) {
  const context = await browser.newContext(scenario.options);
  const page = await context.newPage();
  await mockApi(page);
  await page.goto(`${baseUrl}/product-types`, { waitUntil: "networkidle" });

  await assertVisible(
    page,
    "h1:has-text('Tipos de Produto')",
    `${scenario.name}: título ausente`,
  );
  await assertVisible(
    page,
    "text=Experiência guiada",
    `${scenario.name}: apelido ausente`,
  );
  await assertVisible(
    page,
    "text=Produto imersivo",
    `${scenario.name}: proposta ausente`,
  );

  await page.getByLabel("Nome canônico *").fill("Experiência por missões");
  await page.getByLabel("Apelidos internos").fill("Jornada em missões");
  await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes("/api/product-types") &&
        response.request().method() === "POST",
    ),
    page.getByRole("button", { name: "Cadastrar tipo" }).click(),
  ]);

  const overflow = await page.evaluate(
    () =>
      document.documentElement.scrollWidth >
      document.documentElement.clientWidth + 1,
  );
  if (overflow)
    throw new Error(`${scenario.name}: layout possui overflow horizontal`);

  await page.goto(`${baseUrl}/products/new`, { waitUntil: "networkidle" });
  await page.getByLabel("Tipo de produto *").selectOption("1");
  const saveDisabled = await page
    .getByRole("button", { name: "Salvar" })
    .isDisabled();
  if (saveDisabled)
    throw new Error(
      `${scenario.name}: tipo ativo não liberou o cadastro do produto`,
    );
  await context.close();
}

(async () => {
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  });
  try {
    const scenarios = [
      { name: "desktop", options: { viewport: { width: 1440, height: 1000 } } },
      { name: "iPhone 15 Pro", options: { ...devices["iPhone 15 Pro"] } },
      { name: "Pixel 7", options: { ...devices["Pixel 7"] } },
    ];
    for (const scenario of scenarios) await runScenario(browser, scenario);
    console.log("product-type-catalog-e2e: 3 cenários aprovados");
  } finally {
    await browser.close();
  }
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
