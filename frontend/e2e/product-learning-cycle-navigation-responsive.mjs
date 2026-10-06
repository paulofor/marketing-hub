import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";
import { chromium, devices, expect as baseExpect } from "@playwright/test";

const base = process.env.FRONTEND_BASE_URL || "http://127.0.0.1:4173";
const expect = baseExpect.configure({ timeout: 30000 });
const output = process.env.EVIDENCE_DIR || "/tmp/product-cycle-navigation";
await mkdir(output, { recursive: true });
const profiles = [
  ["desktop", { viewport: { width: 1440, height: 1000 } }],
  ["iphone", devices["iPhone 15 Pro"]],
  ["pixel", devices["Pixel 7"]],
];
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
try {
  for (const [name, profile] of profiles) {
    for (const [productId, chainId, cycleId, experimentId, stage, status] of [
      [7, 26, 4, 88, "DECISION", "OPEN"],
      [7, 26, 5, 98, "ADJUSTMENT", "OPEN"],
      [10, 26, 3, 93, "DECISION", "ADJUSTED"],
      [92001, 92026, 92005, 92098, "PLANNING", "OPEN"],
    ]) {
      const product = {
        id: productId,
        name: `Produto ${productId} · QA local`,
        internalName: productId === 7 ? "Capella" : `QA ${productId}`,
        slug: `qa-produto-${productId}`,
        commercialStatus: "VALIDACAO_COMERCIAL",
        automaticExecutionEnabled: true,
        automaticExecutionStatus: "PLAY",
      };
      const url = `/business-process-chains/learning-cycles?productId=${productId}&chainId=${chainId}`;
      const position = {
        productId,
        resolutionStatus: "IDENTIFIED",
        resolutionMessage: "Posição comercial histórica.",
        chainDefinitionId: chainId,
        chainName: "Cadeia QA local",
        chainVersion: chainId,
        processDefinitionId: 92045,
        processCode: "pde-commercial-homologation-activation",
        processName: "Homologação e ativação comercial do PDE",
        sequenceNumber: 5,
        processCount: 6,
        learningCycleNavigation: {
          productId,
          chainDefinitionId: chainId,
          cycleId,
          experimentId,
          stage,
          status,
          reason: "Acompanhe a próxima ação do ciclo persistido.",
          url,
        },
        subprocessPosition: {
          trackingStatus: "RECORDED",
          subprocessCount: 1,
          currentSubprocessDefinitionId: 92054,
          currentSubprocessName: "Homologação histórica",
        },
      };
      const cycle = {
        id: cycleId,
        productId,
        experimentId,
        chainDefinitionId: chainId,
        processDefinitionId: 92064,
        revision: 1,
        stage,
        stageLabel: "Passagem QA local",
        status,
        baseline: false,
        productVersion: "qa-v1",
        budgetLimitBrl: 0,
        windowStart: null,
        windowEnd: null,
        workUrl: null,
        brief: {
          hypothesis: "Hipótese sintética",
          mainChange: "Navegação",
          successCriterion: "Contexto correto",
        },
        inheritedLearning: {},
        events: [],
        commands: [],
        canCreateSuccessor: false,
        nextAction: "Acompanhar a passagem local",
        responsible: "Backend QA",
        createdAt: "2026-10-06T00:00:00Z",
      };
      const context = await browser.newContext(profile);
      const page = await context.newPage();
      const errors = [],
        mutations = [],
        historicalQueries = [];
      page.on("pageerror", (error) => errors.push(error.message));
      await page.route("**/api/**", async (route) => {
        const request = route.request();
        const path = new URL(request.url()).pathname;
        if (!path.startsWith("/api/")) {
          await route.continue();
          return;
        }
        if (request.method() !== "GET") {
          mutations.push(request.url());
          await route.abort();
          return;
        }
        if (path.includes("activity-executions")) historicalQueries.push(path);
        let data = [];
        if (path === "/api/products") data = [product];
        if (path === "/api/products/value-chain-positions") data = [position];
        if (path.endsWith(`/learning-cycles/v1/products/${productId}`))
          data = [cycle];
        if (path === "/api/business-process-chains")
          data = [
            {
              id: chainId,
              name: "Cadeia QA local",
              versionNumber: chainId,
              status: "PUBLISHED",
            },
          ];
        if (path.endsWith("/learning-cycles/v1/catalog"))
          data = {
            processDefinitionId: 92064,
            version: 1,
            diagram: { nodes: [], flows: [] },
            returnTargets: [],
            experiments: [],
          };
        await route.fulfill({ json: data });
      });
      await page.goto(base + "/products");
      const link = page.getByRole("link", { name: "Abrir ciclo e decisões" });
      await expect(link).toBeVisible();
      await expect(link).toHaveAttribute("href", url);
      assert.equal(
        await page
          .getByRole("link", { name: "Abrir próximo processo" })
          .count(),
        0,
      );
      await link.scrollIntoViewIfNeeded();
      const box = await link.boundingBox();
      assert.ok(
        box && box.width > 100 && box.height >= 40,
        "Botão deve ser tocável",
      );
      const size = await page.evaluate(() => ({
        viewport: document.documentElement.clientWidth,
        content: document.documentElement.scrollWidth,
      }));
      assert.ok(
        size.content <= size.viewport + 1,
        `${name}: sem overflow horizontal`,
      );
      await page.screenshot({
        path: `${output}/${name}-${productId}-${cycleId}.png`,
        fullPage: true,
      });
      await link.click();
      await expect(page).toHaveURL(base + url);
      await expect(
        page.getByRole("heading", { name: "Ciclos de aprendizado e vendas" }),
      ).toBeVisible();
      await expect(
        page.getByRole("heading", {
          name: `Ciclo #${cycleId} · experimento #${experimentId}`,
        }),
      ).toBeVisible();
      await expect(page.getByRole("combobox", { name: /Produto/ })).toHaveValue(
        String(productId),
      );
      await expect(
        page.getByRole("combobox", { name: /Cadeia de Valor/ }),
      ).toHaveValue(String(chainId));
      assert.deepEqual(
        historicalQueries,
        [],
        "Não consultar homologação antiga para escolher o botão",
      );
      assert.deepEqual(mutations, [], "Navegação sem tarefas ou gasto");
      assert.deepEqual(errors, [], "Sem erro JavaScript");
      console.log(
        `${name}: produto ${productId}, ciclo ${cycleId}, destino correto e sem mutações`,
      );
      await context.close();
    }
  }
} finally {
  await browser.close();
}
