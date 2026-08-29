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

const commonBlueprint = {
  customerJob: "Receber orientação pessoal com pouco esforço.",
  valueMechanism:
    "Transformar contexto e memória em uma recomendação aplicável.",
  experienceFlow:
    "Identificar; consentir; conversar; orientar; refinar; avaliar; retornar.",
  requiredInputs: "Cliente, contexto, consentimento e foto opcional.",
  expectedOutputs: "Orientação, motivo e próximo passo.",
  memoryStrategy: "Memória segregada por tenant, produto e cliente.",
  integrationRequirements:
    "Backend PDE, worker Java e Codex App Server por stdio.",
  safetyGuardrails: "Bloquear mistura de clientes e mídia sem consentimento.",
  successMetrics: "Orientação, utilidade, retorno, venda, custo e margem.",
  backendSdkModule: "pde-platform/pde-harness-sdk",
};

const productTypes = [
  {
    id: 4,
    code: "AI_SANDBOX_CONVERSATIONAL_PRODUCT",
    name: "Consultor WhatsApp com IA",
    internalName: "Fluorita",
    description: "Consultoria contextual no canal que o cliente já usa.",
    aliases: ["Consultor WhatsApp", "Especialista por WhatsApp"],
    status: "ACTIVE",
    blueprint: {
      ...commonBlueprint,
      version: "consultant-whatsapp-v1",
      primaryChannel: "WHATSAPP",
      frontendSdkModule: null,
    },
    constructionReady: true,
    missingBlueprintFields: [],
    productCount: 1,
  },
  {
    id: 7,
    code: "AI_PWA_CONSULTANT_PRODUCT",
    name: "Consultor PWA com IA",
    internalName: "Turmalina",
    description:
      "Consultoria mobile-first instalável com experiência visual própria.",
    aliases: ["Consultor PWA", "Consultor web instalável"],
    status: "ACTIVE",
    blueprint: {
      ...commonBlueprint,
      version: "consultant-pwa-v1",
      primaryChannel: "PWA",
      frontendSdkModule: "pde-platform/frontend/src/consultant-sdk/v1",
    },
    constructionReady: true,
    missingBlueprintFields: [],
    productCount: 0,
  },
];

const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN ?? "/usr/bin/chromium",
});

try {
  for (const [profileName, profile] of profiles) {
    const context = await browser.newContext(profile);
    const page = await context.newPage();
    const pageErrors = [];
    const savedPayloads = [];
    page.on("pageerror", (error) => pageErrors.push(error.message));

    await page.route("**/api/**", async (route) => {
      const request = route.request();
      const pathname = new URL(request.url()).pathname;
      if (pathname === "/api/product-types" && request.method() === "GET") {
        await route.fulfill({ status: 200, json: productTypes });
        return;
      }
      if (pathname === "/api/product-types/7" && request.method() === "PUT") {
        savedPayloads.push(request.postDataJSON());
        await new Promise((resolve) => setTimeout(resolve, 220));
        await route.fulfill({ status: 200, json: productTypes[1] });
        return;
      }
      await route.fulfill({ status: 200, json: [] });
    });

    await page.goto(`${baseUrl}/product-types`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByText("Consultor WhatsApp com IA", { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByText("Consultor PWA com IA", { exact: true }),
    ).toBeVisible();
    await expect(page.getByText("Mineral: Fluorita")).toBeVisible();
    await expect(page.getByText("Mineral: Turmalina")).toBeVisible();
    await expect(page.getByText("Base pronta")).toHaveCount(2);

    const pwaCard = page.locator("article", {
      hasText: "Consultor PWA com IA",
    });
    await pwaCard.getByText("Ver base de construção").click();
    await expect(pwaCard.getByText("Canal: PWA")).toBeVisible();
    await expect(
      pwaCard.getByText("pde-platform/frontend/src/consultant-sdk/v1"),
    ).toBeVisible();
    await pwaCard.getByRole("button", { name: "Editar tipo" }).click();
    await expect(page.getByLabel("Versão da base *")).toHaveValue(
      "consultant-pwa-v1",
    );
    await page
      .getByLabel("Métricas de sucesso *")
      .fill("Orientação, utilidade, retorno, receita e margem.");
    await page.getByRole("button", { name: "Salvar alterações" }).click();
    await expect(
      page.getByRole("button", { name: "Salvando..." }),
    ).toBeDisabled();
    await expect(
      page.getByRole("button", { name: "Cadastrar tipo" }),
    ).toBeVisible();

    assert.equal(
      savedPayloads.length,
      1,
      `${profileName}: salvamento duplicado`,
    );
    assert.equal(
      savedPayloads[0].blueprint.primaryChannel,
      "PWA",
      `${profileName}: canal alterado pelo formulário`,
    );
    assert.equal(
      savedPayloads[0].blueprint.successMetrics,
      "Orientação, utilidade, retorno, receita e margem.",
      `${profileName}: métricas editadas não foram enviadas`,
    );
    const dimensions = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      content: document.documentElement.scrollWidth,
    }));
    assert.ok(
      dimensions.content <= dimensions.viewport + 1,
      `${profileName}: overflow horizontal ${dimensions.content}px > ${dimensions.viewport}px`,
    );
    assert.deepEqual(
      pageErrors,
      [],
      `${profileName}: erros JavaScript não tratados`,
    );
    await page.screenshot({
      path: `/tmp/product-type-catalog-${profileName.replaceAll(" ", "-")}.png`,
      fullPage: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}

console.log(
  "Catálogo de tipos aprovado em desktop, iPhone 15 Pro e Pixel 7, com edição sem duplicidade.",
);
