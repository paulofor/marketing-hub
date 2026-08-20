const { chromium, devices } = require("@playwright/test");

const frontendUrl =
  process.env.FRONTEND_URL ?? "http://127.0.0.1:15184/business-processes";

const profiles = [
  { name: "desktop", options: { viewport: { width: 1440, height: 1000 } } },
  { name: "iphone-15-pro", options: devices["iPhone 15 Pro"] },
  { name: "pixel-7", options: devices["Pixel 7"] },
];

(async () => {
  const browser = await chromium.launch({
    ...(process.env.CHROMIUM_BIN
      ? { executablePath: process.env.CHROMIUM_BIN }
      : {}),
    headless: true,
  });

  try {
    for (const profile of profiles) {
      const context = await browser.newContext(profile.options);
      const page = await context.newPage();
      const pageErrors = [];
      page.on("pageerror", (error) => pageErrors.push(error.message));
      await page.goto(frontendUrl, { waitUntil: "networkidle" });
      const processButton = page.getByRole("button", {
        name: /Homologação local de entregáveis premium/,
      });
      try {
        await processButton.waitFor({ state: "visible", timeout: 30_000 });
      } catch (error) {
        throw new Error(
          `Catálogo não carregou em ${profile.name}. Página: ${await page.locator("body").innerText()}. Erros: ${pageErrors.join(" | ")}`,
          { cause: error },
        );
      }
      await processButton.click();
      await page.getByText("Recurso obrigatório:").waitFor();
      await page
        .getByText(/Estúdio de Imagens de Têmis.*themis-image-studio/)
        .waitFor();
      await page.getByRole("button", { name: "Criar versão editável" }).click();
      const resourceSelect = page.getByLabel(
        "Recurso especializado de Produzir entregáveis premium",
      );
      if ((await resourceSelect.inputValue()) !== "themis-image-studio") {
        throw new Error(`Recurso não preservado no editor ${profile.name}`);
      }
      const hasOverflow = await page.evaluate(
        () =>
          document.documentElement.scrollWidth >
          document.documentElement.clientWidth,
      );
      if (hasOverflow) {
        const overflowEvidence = await page.evaluate(() => ({
          clientWidth: document.documentElement.clientWidth,
          scrollWidth: document.documentElement.scrollWidth,
          elements: [...document.querySelectorAll("body *")]
            .map((element) => {
              const rectangle = element.getBoundingClientRect();
              return {
                selector: `${element.tagName.toLowerCase()}.${element.className}`,
                left: rectangle.left,
                right: rectangle.right,
                width: rectangle.width,
              };
            })
            .filter(
              (element) =>
                element.right > document.documentElement.clientWidth + 1 ||
                element.left < -1,
            )
            .slice(0, 10),
        }));
        throw new Error(
          `Overflow horizontal encontrado em ${profile.name}: ${JSON.stringify(overflowEvidence)}`,
        );
      }
      await page.screenshot({
        path: `/tmp/business-process-resource-${profile.name}.png`,
        fullPage: true,
      });
      await context.close();
    }
  } finally {
    await browser.close();
  }

  process.stdout.write(
    "Homologação visual concluída em desktop, iPhone 15 Pro e Pixel 7.\n",
  );
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
