import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");

const baseUrl = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
const debug = process.env.DEBUG_E2E === "1";
const profiles = [
  ["desktop", { viewport: { width: 1440, height: 1000 } }],
  ["iPhone 15 Pro", devices["iPhone 15 Pro"]],
  ["Pixel 7", devices["Pixel 7"]],
];

const reference = {
  id: 3,
  title: "Madonna",
  sourceUrl: "https://cdn.example/madonna-reference.mp4",
  sourcePlatform: "Upload",
  niche: "Criativos IA",
  funnelStage: "AWARENESS",
  primaryLearningGoal: "Fazer nesse estilo",
  status: "ANALYZED",
};

const productionBlueprint = {
  archetype: "Performance narrativa original",
  targetDurationSeconds: 60,
  format: "VERTICAL_9_16",
  hook: "Abrir com uma ação visual original",
  story:
    "Uma artista fictícia atravessa quatro atos originais até uma recompensa visual coerente.",
  scenePlan: [
    "Cena 1 em clube autoral, ação de entrada, plano geral e função de gancho.",
    "Cena 2 no palco original, gesto visível, plano médio e progressão comercial.",
    "Cena 3 com dança autorizada, travelling curto e recompensa visual.",
    "Cena 4 em close original, encerramento e convite comercial claro.",
  ],
  characterBible: "Artista fictícia com aparência, voz e figurino originais.",
  environmentBible: "Clube autoral com mapa de luz e posições persistidas.",
  objectBible: "Objetos sem marcas ou personagens reconhecíveis.",
  visualStyleGuide:
    "Paleta autoral, contraste mobile e textura cinematográfica.",
  imageGenerationPlan: "Gerar e aprovar frames mestres antes dos clipes.",
  continuityRules: "Preservar rosto, figurino, luz, lentes e objetos.",
  voiceoverPlan: "Voz original e autorizada com direção natural.",
  soundtrackPlan: "Trilha original ou licenciada com prova persistida.",
  captionPlan: "Legendas temporizadas dentro da área segura vertical.",
  providerPlan: "Homologar performance autorizada com Runway Act-Two.",
  editingNotes: "Alternar escalas e finalizar texto na pós-produção.",
  qualityGate: "Bloquear sem direitos, continuidade, áudio e revisão humana.",
  estimatedGeneratedClips: 8,
  requiresLipSync: true,
  requiresLicensedMusic: true,
  apolloCapability: "EXTEND_APOLLO",
  capabilityGaps: ["Homologar performance e lip-sync"],
};

const analysis = {
  executionId: 91,
  referenceId: 3,
  attemptNumber: 1,
  status: "COMPLETED",
  input: { title: "Madonna" },
  artifacts: {
    durationSeconds: 63.7,
    width: 576,
    height: 1024,
    sceneChangeCount: 10,
    integratedLoudnessLufs: -14.1,
  },
  output: {
    commercialDiagnosis:
      "A referência usa performance, progressão e recompensa visual para sustentar atenção.",
    hook: "Abrir com uma ação visual original e reconhecível.",
    narrativePattern:
      "Gancho, progressão, recompensa, prova visual e encerramento.",
    visualDirection:
      "Luz original, personagem fictícia e alternância de planos.",
    continuityStrategy:
      "Bíblia fixa para personagem, cenário, figurino e paleta.",
    audioStrategy: "Voz e trilha originais ou licenciadas com mixagem mobile.",
    captionStrategy: "Legendas curtas, sincronizadas e em área segura.",
    sequence: [],
    reusableLearnings: [
      "Alternar escalas",
      "Criar recompensa",
      "Legendar com ritmo",
    ],
    salesApplications: {
      campaign: "Criativo vertical para retenção e CTA.",
      product: "Abertura premium de uma aula.",
      organic: "Série autoral de performance.",
    },
    rightsRisks: [
      "Não copiar artista, voz, música, letra ou gravação da referência.",
    ],
    productionBlueprint,
    operationalDecision: "NEEDS_PROVIDER_HOMOLOGATION",
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
    page.on("pageerror", (error) => pageErrors.push(error.message));
    page.on("console", (message) => {
      if (debug) console.log(`${name} console: ${message.text()}`);
    });
    await page.route("**/api/**", async (route) => {
      const url = route.request().url();
      const pathname = new URL(url).pathname;
      if (debug) console.log(`${name} request: ${url}`);
      if (!pathname.startsWith("/api/")) {
        await route.continue();
        return;
      }
      let json = [];
      if (
        url.includes("/reference-videos/3") &&
        !url.includes("/reference-analysis/")
      ) {
        json = reference;
      } else if (url.includes("/reference-analysis/v1/references/3/latest")) {
        json = analysis;
      } else if (url.includes("/sales-videos/studio/catalog")) {
        json = { characters: [], captionPresets: [] };
      } else if (/\/api\/assets\/1953(?:\?|$)/.test(url)) {
        json = {
          id: 1953,
          publicUrl: "https://assets.example/master.png",
        };
      }
      await route.fulfill({ json });
    });

    await page.goto(`${baseUrl}/audio-video-studio/videos-analysis/3/results`, {
      waitUntil: "domcontentloaded",
    });
    if (debug) console.log(`${name} body: ${await page.locator("body").innerText()}`);
    await expect(
      page.getByText("Performance narrativa original", { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByText(/não copiar artista, voz, música, letra/i),
    ).toBeVisible();
    await assertNoHorizontalOverflow(page, `${name}: resultado`);

    await page
      .getByRole("link", { name: /produzir com esta receita/i })
      .click();
    await page
      .getByRole("button", { name: /aplicar receita ao projeto/i })
      .click();
    await expect(page.getByText(/Receita #3 aplicada/i)).toBeVisible();
    assert.equal(await page.getByLabel(/id do produto/i).inputValue(), "");
    assert.equal(
      await page.getByLabel(/^cta$/i).inputValue(),
      "Fazer o diagnostico MUSA",
    );

    await page.getByRole("button", { name: /runway act-two/i }).click();
    for (const label of [
      /url https da personagem/i,
      /url https da performance/i,
      /evidencia de consentimento/i,
      /evidencia dos direitos da performance/i,
    ]) {
      await expect(page.getByLabel(label)).toBeVisible();
    }
    await expect(page.getByText(/permanece em homologacao/i)).toBeVisible();
    await assertNoHorizontalOverflow(page, `${name}: Estúdio`);
    assert.deepEqual(pageErrors, [], `${name}: erros não tratados na página`);
    await page.screenshot({
      path: `/tmp/video-reference-analysis-v1-${name.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Análise, importação e Act-Two aprovados em desktop, iPhone 15 Pro e Pixel 7.",
);

async function assertNoHorizontalOverflow(page, context) {
  const sizes = await page.evaluate(() => ({
    viewport: document.documentElement.clientWidth,
    content: document.documentElement.scrollWidth,
  }));
  assert.ok(
    sizes.content <= sizes.viewport + 1,
    `${context}: overflow horizontal ${sizes.content}px > ${sizes.viewport}px`,
  );
}
