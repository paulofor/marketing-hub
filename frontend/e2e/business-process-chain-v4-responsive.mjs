import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium, devices } = require("@playwright/test");

const baseUrl = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
const debug = process.env.DEBUG_E2E === "1";

const chain = {
  id: 4,
  chainCode: "pde-value-creation-delivery",
  name: "Criação e entrega de valor de Produtos Digitais Experienciais",
  purpose:
    "Transformar uma oportunidade real em produto verificável, fácil de decidir e usar, compreensível por pessoas e agentes e vendido com controle.",
  outcomeDescription:
    "PDE escolhido com confiança, comprado com menor esforço, entregue com satisfação e convertido em aprendizado auditável para escala.",
  primaryMetric: "Tempo até venda entregue com satisfação",
  versionNumber: 4,
  status: "PUBLISHED",
  processCount: 6,
  createdAt: "2026-08-21T00:00:00Z",
  publishedAt: "2026-08-21T00:00:00Z",
};

const processNames = [
  "Descoberta e priorização da oportunidade PDE",
  "Plano Comercial e desenho da oferta PDE",
  "Construção e aprovação do PDE",
  "Comunicação e jornada de venda do PDE",
  "Homologação e ativação comercial do PDE",
  "Venda, entrega e aprendizado do PDE",
];

const detail = {
  ...chain,
  processes: processNames.map((name, index) => ({
    sequenceNumber: index + 1,
    valueContribution:
      index === 3
        ? "Torna a oferta compreensível para pessoas e agentes, com personalização explicável e jornada atribuível."
        : "Reduz esforço de decisão e preserva evidência, controle e valor entregue.",
    processDefinitionId: 401 + index,
    processCode: `pde-process-${index + 1}`,
    name,
    purpose:
      "Mantém Cartão de Decisão, prova verificável, fatos consistentes e controle explícito da personalização.",
    ownerName: "Marketing Hub",
    triggerDescription: "Resultado verificável do processo anterior.",
    outcomeDescription: "Gate aprovado com evidências persistidas.",
    versionNumber: index < 3 ? 4 : 3,
    status: "PUBLISHED",
  })),
};

const profiles = [
  ["desktop", { viewport: { width: 1440, height: 1000 } }],
  ["iPhone 15 Pro", devices["iPhone 15 Pro"]],
  ["Pixel 7", devices["Pixel 7"]],
];

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN,
});
try {
  for (const [name, options] of profiles) {
    const context = await browser.newContext(options);
    const page = await context.newPage();
    page.on("console", (message) => {
      if (debug) console.log(`${name} console: ${message.text()}`);
    });
    page.on("pageerror", (error) => {
      if (debug) console.log(`${name} pageerror: ${error.message}`);
    });
    await page.route("**/api/**", async (route) => {
      const pathname = new URL(route.request().url()).pathname;
      if (debug) console.log(`${name} request: ${pathname}`);
      if (!pathname.startsWith("/api/")) {
        await route.continue();
      } else if (pathname === "/api/business-process-chains") {
        await route.fulfill({ json: [chain] });
      } else if (pathname === "/api/business-process-chains/4") {
        await route.fulfill({ json: detail });
      } else {
        await route.fulfill({ json: [] });
      }
    });

    await page.goto(`${baseUrl}/business-process-chains?chainId=3`, {
      waitUntil: "domcontentloaded",
    });
    await page.getByRole("heading", { name: `${chain.name} · v4` }).waitFor();
    await page.getByText("6 em sequência").waitFor();
    for (const processName of processNames) {
      await page.getByRole("heading", { name: processName }).waitFor();
    }

    const sizes = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
    }));
    assert.ok(
      sizes.content <= sizes.viewport + 1,
      `${name}: overflow horizontal ${sizes.content}px > ${sizes.viewport}px`,
    );
    await context.close();
  }
} finally {
  await browser.close();
}

console.log("Cadeia PDE v4 aprovada em desktop, iPhone 15 Pro e Pixel 7.");
