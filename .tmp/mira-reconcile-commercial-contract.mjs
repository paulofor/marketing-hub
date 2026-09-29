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
  page.setDefaultTimeout(120_000);
  page.on("console", (message) => {
    if (message.type() === "error") console.error(`browser: ${message.text()}`);
  });

  await page.goto(
    "http://191.252.181.168:5173/products/10/pde-versions#pde-contract-editor",
    { waitUntil: "networkidle", timeout: 60_000 },
  );

  const editor = page.locator("#pde-editor-json");
  await editor.waitFor();
  const contract = JSON.parse(await editor.inputValue());
  contract.name = "Mira · sua rotina organizada";
  contract.promise =
    "Organize os produtos que você já tem em uma rotina individualizada, clara e consultável.";
  contract.theme = {
    ...contract.theme,
    primary: "#6b3e7d",
    accent: "#7a4e8c",
    background: "#f7f2fa",
    imageUrl: "/media/mira-commercial-demo-v3-poster.jpg",
  };
  contract.heroVideos = [
    {
      experienceVersion: "mira-commercial-v1",
      placement: "hero",
      playbackUrl:
        "https://mira.digicomdigital.com.br/media/mira-commercial-demo-v3-hls/index.m3u8",
      hlsPlaybackUrl:
        "https://mira.digicomdigital.com.br/media/mira-commercial-demo-v3-hls/index.m3u8",
      posterUrl:
        "https://mira.digicomdigital.com.br/media/mira-commercial-demo-v3-poster.jpg",
      autoplay: false,
      muted: false,
      controls: true,
      loop: false,
      playsInline: true,
      source: "MARKETING_HUB_APPROVED_EXPERIMENT_VIDEO",
      assetId: 2981,
      experimentVideoAssetId: 49,
      salesVideoProfileId: null,
      salesVideoJobId: null,
      reviewStatus: "APPROVED",
      status: "READY",
    },
  ];

  await editor.fill(JSON.stringify(contract, null, 2));
  await page
    .locator("#pde-editor-published-by")
    .fill("Codex · reconciliação Processo 4");

  const [publishResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.request().method() === "POST" &&
        response.url().endsWith(
          "/api/products/10/pde-production-slots/v1/publish",
        ),
    ),
    page.getByRole("button", { name: "Publicar no slot" }).click(),
  ]);
  const publishBody = await publishResponse.text();
  if (!publishResponse.ok()) {
    throw new Error(
      `Publicação falhou: HTTP ${publishResponse.status()} ${publishBody}`,
    );
  }

  const [validationResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.request().method() === "POST" &&
        response.url().endsWith(
          "/api/products/10/pde-production-slots/v1/validate",
        ),
    ),
    page.getByRole("button", { name: "Testar URL" }).first().click(),
  ]);
  const validationBody = await validationResponse.text();
  if (!validationResponse.ok()) {
    throw new Error(
      `Validação falhou: HTTP ${validationResponse.status()} ${validationBody}`,
    );
  }

  const published = JSON.parse(publishBody);
  const validated = JSON.parse(validationBody);
  const publishedContract = JSON.parse(published.publishedExperienceJson);
  console.log(
    JSON.stringify(
      {
        slotCode: published.slotCode,
        experienceVersion: published.experienceVersion,
        publishedAt: published.publishedAt,
        publishedBy: published.publishedBy,
        name: publishedContract.name,
        promise: publishedContract.promise,
        theme: publishedContract.theme,
        heroVideo: publishedContract.heroVideos?.[0],
        validationStatus: validated.validationStatus,
        validationSummary: validated.validationSummary,
        validationCheckedAt: validated.validationCheckedAt,
      },
      null,
      2,
    ),
  );
} finally {
  await browser.close();
}
