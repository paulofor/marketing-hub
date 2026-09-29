import playwright from "../frontend/node_modules/@playwright/test/index.js";

const { chromium, devices } = playwright;
const profiles = [
  { name: "desktop", options: { viewport: { width: 1440, height: 1100 } } },
  { name: "iphone-15-pro", options: devices["iPhone 15 Pro"] },
  { name: "pixel-7", options: devices["Pixel 7"] },
];
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
  headless: true,
});

try {
  for (const profile of profiles) {
    const context = await browser.newContext({ ...profile.options, locale: "pt-BR" });
    const page = await context.newPage();
    await page.goto(
      `https://mira.digicomdigital.com.br/?mh_test=1&profile=${profile.name}`,
      { waitUntil: "domcontentloaded", timeout: 60_000 },
    );
    await page.getByRole("heading", { name: /Cuide de você com mais clareza/i }).waitFor();
    await page.waitForTimeout(10_000);
    const roleMatch = page.getByRole("link", {
      name: /Organizar minha rotina por R\$ 49/i,
    });
    const textMatch = page.getByText("Organizar minha rotina por R$ 49", {
      exact: true,
    });
    const result = {
      profile: profile.name,
      roleCount: await roleMatch.count(),
      textCount: await textMatch.count(),
      links: await page.locator("a").evaluateAll((links) =>
        links.map((link) => ({
          text: link.textContent?.trim(),
          visible: Boolean(link.offsetWidth || link.offsetHeight || link.getClientRects().length),
          rect: link.getBoundingClientRect().toJSON(),
          ariaLabel: link.getAttribute("aria-label"),
          href: link.href,
        })),
      ),
    };
    process.stdout.write(`${JSON.stringify(result)}\n`);
    await context.close();
  }
} finally {
  await browser.close();
}
