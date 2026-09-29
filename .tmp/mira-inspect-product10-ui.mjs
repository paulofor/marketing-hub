import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium } = playwright;
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1400 },
    locale: "pt-BR",
  });
  await page.goto("http://191.252.181.168:5173/products/10/edit", {
    waitUntil: "networkidle",
    timeout: 60_000,
  });
  const fields = [
    "name",
    "internalName",
    "colorPalette",
    "pdeExperienceJson",
    "validationDefinitionVersion",
    "currentPriceBrl",
    "instagramAccountId",
    "riskReversal",
    "commercialNotes",
    "desireAssociationMapJson",
  ];
  const product = {};
  for (const field of fields) {
    product[field] = await page.locator(`#product-${field}`).inputValue();
  }

  await page.goto(
    "http://191.252.181.168:5173/products/10/pde-versions#pde-contract-editor",
    { waitUntil: "networkidle", timeout: 60_000 },
  );
  const slotContract = JSON.parse(await page.locator("#pde-editor-json").inputValue());
  process.stdout.write(
    `${JSON.stringify(
      {
        product: {
          ...product,
          pdeExperienceJson: JSON.parse(product.pdeExperienceJson),
        },
        slot: {
          experienceVersion: slotContract.experienceVersion,
          theme: slotContract.theme,
          heroVideos: slotContract.heroVideos,
        },
      },
      null,
      2,
    )}\n`,
  );
} finally {
  await browser.close();
}
