import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const baseUrl = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
const profiles = [
  ["desktop", { viewport: { width: 1440, height: 1000 } }],
  ["iPhone 15 Pro", devices["iPhone 15 Pro"]],
  ["Pixel 7", devices["Pixel 7"]],
];

const cards = Array.from({ length: 13 }, (_, index) => ({
  cardId: `RI1-${(index + 1).toString(16).padStart(12, "0").toUpperCase()}`,
  collection: index % 2 === 0 ? "video" : "prazer-audio-visual",
  title: `Cartão audiovisual global ${index + 1}`,
  finding: "O primeiro quadro precisa materializar uma situação reconhecível.",
  mechanism: "Antecipação visual com recompensa perceptível.",
  commercialApplication: "Adaptar o gancho ao briefing do projeto atual.",
  evidenceStrength: "Evidência externa para formular hipótese.",
  publishedOn: "2026-08-31",
  validUntil: "2026-10-15",
  experimentHypothesis: "Melhorar retenção sem aumentar retrabalho.",
  risks: "Não generalizar o achado para outro público sem teste.",
  limits: "Não substitui evento humano, pagamento ou satisfação.",
  sourcePath: `pesquisas/video/2026-08-31-global-${index + 1}.md`,
  sourceSha256: (index + 1).toString(16).padStart(64, "0"),
  evidenceKind: "EXTERNAL_RESEARCH",
}));

const agentPolicies = [
  ["communication-director", "Íris", "COMMUNICATION_ADVISORY"],
  ["videomaker", "Apolo", "PRODUCTION_ADVISORY"],
  ["customer-agent", "Psique", "REVIEW_CRITERIA_ONLY"],
  ["meta-ad-approver", "Têmis", "REVIEW_CRITERIA_ONLY"],
].map(([agentKey, agentName, authority]) => ({
  agentKey,
  agentName,
  purpose: `Aplicar pesquisa na responsabilidade de ${agentName}.`,
  authority,
  collections: ["video", "prazer-audio-visual"],
  maxCardsPerContext: 4,
}));

const catalog = {
  contractVersion: "HARNESS_RESEARCH_INTELLIGENCE_V1",
  evaluatedOn: "2026-09-03",
  totalCompiledCards: cards.length,
  activeCards: cards.length,
  agentPolicies,
  cards,
  limitations: [
    "Cartões são evidência externa; não comprovam demanda, venda ou satisfação.",
  ],
};

const futureProject = {
  id: 847,
  productId: 10,
  experimentId: 203,
  videoCategory: "COMMERCIAL_SHORT",
  contextType: "PDE",
  productionMode: "CINEMATIC_SCENE_BLUEPRINT",
  targetChannel: "YOUTUBE_SHORTS",
  format: "VERTICAL_9_16",
  title: "Projeto futuro fora do Vega",
  objective: "Converter atenção em diagnóstico financeiro",
  targetDurationSeconds: 30,
  status: "READY_FOR_SCRIPT",
  researchIntelligence: {
    contractVersion: catalog.contractVersion,
    contextFingerprint: "a".repeat(64),
    totalAvailableCards: cards.length,
    routes: agentPolicies.map((policy) => ({
      agentKey: policy.agentKey,
      agentName: policy.agentName,
      purpose: policy.purpose,
      authority: policy.authority,
      selectionReason: "Seleção contextual do briefing atual.",
      cards: cards.slice(0, 2),
    })),
    limitations: catalog.limitations,
  },
};

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
});
try {
  for (const [name, options] of profiles) {
    const context = await browser.newContext(options);
    const page = await context.newPage();
    const pageErrors = [];
    const catalogRequests = [];
    page.on("pageerror", (error) => pageErrors.push(error.message));
    await page.route("**/api/**", async (route) => {
      const pathname = new URL(route.request().url()).pathname;
      if (pathname === "/api/research-intelligence/v1/catalog") {
        catalogRequests.push(pathname);
        await route.fulfill({ json: catalog });
      } else if (pathname === "/api/sales-videos/projects/847") {
        await route.fulfill({ json: futureProject });
      } else if (pathname === "/api/sales-videos/studio/catalog") {
        await route.fulfill({ json: { characters: [], captionPresets: [] } });
      } else {
        await route.fulfill({ json: [] });
      }
    });

    await page.goto(`${baseUrl}/audio-video-studio/research-library`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByRole("heading", {
        name: /biblioteca de inteligência do harness/i,
      }),
    ).toBeVisible();
    await expect(page.getByText(/fonte global única/i)).toBeVisible();
    await expect(page.getByText(/projetos atuais e futuros/i)).toBeVisible();
    await expect(page.getByText("Apolo", { exact: true }).first()).toBeVisible();
    await expect(page.getByText(/cartão audiovisual global 12/i)).toBeVisible();
    await expect(page.getByText(/cartão audiovisual global 13/i)).toHaveCount(0);
    await page.getByRole("button", { name: /exibir mais cartões/i }).click();
    await expect(page.getByText(/cartão audiovisual global 13/i)).toBeVisible();
    await assertNoHorizontalOverflow(page, `${name}: catálogo global`);
    await page.screenshot({
      path: `/tmp/research-intelligence-catalog-${name.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });

    const requestsBeforeProject = catalogRequests.length;
    await page.goto(`${baseUrl}/audio-video-studio/projects/847`, {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByText(/seleção contextual deste projeto/i)).toBeVisible();
    await expect(page.getByLabel(/titulo do projeto/i)).toHaveValue(
      "Projeto futuro fora do Vega",
    );
    await expect(
      page.getByRole("link", {
        name: /catálogo global usado por todos os projetos/i,
      }),
    ).toHaveAttribute("href", "/audio-video-studio/research-library");
    assert.equal(
      catalogRequests.length,
      requestsBeforeProject,
      `${name}: detalhe não deve carregar o catálogo integral`,
    );
    await assertNoHorizontalOverflow(page, `${name}: projeto futuro`);
    assert.deepEqual(pageErrors, [], `${name}: erros não tratados`);
    await page.screenshot({
      path: `/tmp/research-intelligence-project-${name.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Biblioteca global aprovada em desktop, iPhone 15 Pro e Pixel 7, inclusive projeto fora do Vega.",
);

async function assertNoHorizontalOverflow(page, context) {
  const sizes = await page.evaluate(() => ({
    viewport: document.documentElement.clientWidth,
    page: document.documentElement.scrollWidth,
    content: document.querySelector(".audio-video-studio-page")?.scrollWidth,
  }));
  assert.ok(
    (sizes.content ?? 0) <= sizes.viewport + 1,
    `${context}: overflow horizontal ${JSON.stringify(sizes)}`,
  );
}
