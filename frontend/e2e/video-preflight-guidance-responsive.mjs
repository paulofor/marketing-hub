import assert from "node:assert/strict";
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const data = JSON.parse(
  await readFile(process.env.VIDEO_PREFLIGHT_FIXTURE_RESULT, "utf8"),
);
const output = process.env.VIDEO_PREFLIGHT_EVIDENCE_DIR;
await mkdir(output, { recursive: true });
const base = "http://127.0.0.1:15173",
  processId = data.processUrl.split("/")[3];
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
const results = [];
try {
  for (const [name, device] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(device),
      page = await context.newPage(),
      errors = [],
      writes = [],
      external = [];
    let project = {
      id: 91004,
      productId: 91001,
      experimentId: 91001,
      campaignKey: "fixture-video-finance-v1",
      title: "Vídeo sintético segregado",
      objective: "Primeiro ajuste",
      scriptText: "Aplicar, avaliar e retomar.",
      hookText: "Primeiro ajuste",
      captionPlan: "Aplicar, avaliar e retomar.",
      ctaText: "Ver o ajuste",
      scenePlan: "Dor\nMecanismo\nResultado\nProva",
      referencePerformanceUri:
        "internal://agent-tasks/91005/visual-evidence/91006#crop=10,20,300,400",
      status: "DRAFT",
      targetDurationSeconds: 15,
      videoCategory: "COMMERCIAL_SHORT",
    };
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/*", async (route) => {
      const request = route.request(),
        url = new URL(request.url());
      if (url.origin !== base) {
        external.push(url.href);
        return route.abort();
      }
      if (request.method() !== "GET") writes.push(url.pathname);
      if (url.pathname === "/api/sales-videos/projects/91004") {
        if (request.method() === "PATCH") {
          const update = request.postDataJSON();
          for (const key of [
            "productId",
            "experimentId",
            "campaignKey",
            "scriptText",
            "hookText",
            "captionPlan",
            "ctaText",
            "referencePerformanceUri",
            "targetDurationSeconds",
          ])
            assert.equal(update[key], project[key], key);
          project = { ...project, ...update };
        }
        return route.fulfill({ json: project });
      }
      if (
        url.pathname === "/api/sales-videos/autonomy/v1/cycles" &&
        request.method() === "POST"
      ) {
        const requestBody = request.postDataJSON();
        assert.equal(requestBody.videoProjectId, project.id);
        assert.equal(requestBody.budgetLimitUsd, 8);
        assert.equal(requestBody.productionProfile, "FINAL_CAMPAIGN");
        assert.equal(project.scenePlan.split("\n").length, 5);
        return route.fulfill({
          status: 201,
          json: {
            id: 91009,
            ...requestBody,
            status: "WAITING_PROVIDER_PREFLIGHT",
          },
        });
      }
      if (
        url.pathname.startsWith("/api/") &&
        !url.pathname.startsWith(
          "/api/business-process-chains/learning-cycles/v1",
        ) &&
        !url.pathname.startsWith("/api/business-processes/") &&
        !["/api/products", "/api/business-process-chains"].includes(
          url.pathname,
        )
      )
        return route.fulfill({ json: [] });
      return route.continue();
    });
    const processUrl = `${base}/products/91001/value-chain-history/processes/${processId}/activities?learningCycleId=${data.cycle}&chainId=91002`;
    await page.goto(processUrl);
    const panel = page.getByRole("region", {
      name: "Execução automática do processo",
    });
    await expect(
      panel.getByText(data.action.title, { exact: true }),
    ).toBeVisible();
    await expect(panel.getByRole("progressbar")).toHaveAttribute(
      "aria-valuenow",
      "0",
    );
    await expect(
      panel.locator(".product-process-situation__running-icon"),
    ).toHaveCount(0);
    await panel.getByText("Ver contexto completo", { exact: true }).click();
    const copied = panel.getByLabel("Contexto completo do processo", {
      exact: true,
    });
    await expect(copied).toContainText(data.action.code);
    await expect(copied).toContainText(data.action.evidenceReference);
    await panel.getByText("Ver contexto completo", { exact: true }).click();
    const action = panel.getByRole("link", {
      name: data.action.actionLabel,
      exact: true,
    });
    await expect(action).toHaveAttribute(
      "href",
      "/audio-video-studio/projects/91004",
    );
    await action.scrollIntoViewIfNeeded();
    await page.screenshot({ path: `${output}/${name}-blocked.png` });
    await action.click();
    await expect(page.getByLabel("Roteiro completo")).toHaveValue(
      "Aplicar, avaliar e retomar.",
    );
    assert.equal(
      new URL(page.url()).pathname,
      "/audio-video-studio/projects/91004",
    );
    await expect(
      page.getByLabel("Referência da captura homologada"),
    ).toHaveValue(
      "internal://agent-tasks/91005/visual-evidence/91006#crop=10,20,300,400",
    );
    await page
      .getByLabel("Referência da captura homologada")
      .scrollIntoViewIfNeeded();
    await page.screenshot({ path: `${output}/${name}-proof-reference.png` });
    await expect(page.getByLabel(/^Cena \d+ ·/)).toHaveCount(4);
    await page
      .getByRole("button", { name: "Adicionar cena", exact: true })
      .click();
    await page.getByLabel(/^Cena 5 ·/).fill("CTA: ver o ajuste privado");
    const saved = page.waitForResponse(
      (response) =>
        response.url().endsWith("/api/sales-videos/projects/91004") &&
        response.request().method() === "PATCH",
    );
    await page
      .getByRole("button", { name: "Salvar continuidade", exact: true })
      .click();
    assert.equal((await saved).status(), 200);
    assert.deepEqual(project.scenePlan.split("\n"), [
      "Dor",
      "Mecanismo",
      "Resultado",
      "Prova",
      "CTA: ver o ajuste privado",
    ]);
    await page.reload();
    await expect(page.getByLabel(/^Cena 5 ·/)).toHaveValue(
      "CTA: ver o ajuste privado",
    );
    await expect(page.getByLabel("Roteiro completo")).toHaveValue(
      "Aplicar, avaliar e retomar.",
    );
    await page.getByLabel(/^Cena 5 ·/).scrollIntoViewIfNeeded();
    await page.screenshot({ path: `${output}/${name}-fifth-scene.png` });
    await page.getByLabel("Teto do ciclo em USD", { exact: true }).fill("8");
    await page
      .getByLabel("Perfil de produção do ciclo", { exact: true })
      .selectOption("FINAL_CAMPAIGN");
    await page
      .getByLabel("Objetivo de aprendizado", { exact: true })
      .fill("Validar a solicitação do projeto local segregado.");
    await page
      .getByLabel("Critério de sucesso", { exact: true })
      .fill("Persistir cinco cenas e solicitar somente o ciclo governado.");
    const requested = page.waitForResponse(
      (response) =>
        response.url().endsWith("/api/sales-videos/autonomy/v1/cycles") &&
        response.request().method() === "POST",
    );
    await page
      .getByRole("button", {
        name: "Solicitar produção a Apolo sob controle de Plutus",
        exact: true,
      })
      .click();
    assert.equal((await requested).status(), 201);
    await page.goBack();
    await expect(
      panel.getByText(data.action.title, { exact: true }),
    ).toBeVisible();
    assert.deepEqual(errors, []);
    assert.deepEqual(writes, [
      "/api/sales-videos/projects/91004",
      "/api/sales-videos/autonomy/v1/cycles",
    ]);
    assert.deepEqual(external, []);
    results.push({
      device: name,
      actionCode: data.action.code,
      persistedEvidence: true,
      exactProject: true,
      return: true,
      externalCalls: 0,
      legacyFourToFiveScenesPersisted: true,
      governedProductionRequested: true,
    });
    await context.close();
  }
} finally {
  await browser.close();
}
await writeFile(`${output}/results.json`, JSON.stringify(results, null, 2));
console.log(JSON.stringify(results));
