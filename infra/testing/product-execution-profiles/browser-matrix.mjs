import assert from "node:assert/strict";
import { mkdir, writeFile } from "node:fs/promises";
import { createRequire } from "node:module";
import { approve, base, backend, payload, request } from "./api-matrix.mjs";
const require = createRequire(
  new URL("../../../frontend/package.json", import.meta.url),
);
const { chromium, devices, expect } = require("@playwright/test");
const output =
  process.env.PROFILE_TEST_ARTIFACTS ||
  "artifacts/product-execution-profiles/browser";
await mkdir(output, { recursive: true });
const origin = "http://127.0.0.1:4174";
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
  headless: true,
});
const results = [];
try {
  for (const [index, [name, options]] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ].entries()) {
    const id = 94021 + index;
    const context = await browser.newContext({
      ...options,
      reducedMotion: "reduce",
    });
    const page = await context.newPage();
    let errorMode = false;
    const errors = [],
      mutations = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/*", async (route) => {
      const url = new URL(route.request().url());
      if (url.pathname.startsWith(base(id))) {
        if (errorMode)
          return route.fulfill({
            status: 503,
            json: { detail: "Indisponibilidade simulada" },
          });
        if (route.request().method() === "POST") {
          mutations.push(url.pathname);
          await new Promise((resolve) => setTimeout(resolve, 250));
        }
        return route.continue({ url: origin + url.pathname + url.search });
      }
      if (url.pathname.endsWith("/activity-executions"))
        return route.fulfill({
          status: 503,
          json: {
            detail:
              "Falha de consulta simulada para conferir preservação do contexto",
          },
        });
      if (url.pathname.endsWith("/process-context"))
        return route.fulfill({ json: null });
      if (url.pathname.startsWith("/api/"))
        return route.fulfill({
          json:
            url.pathname === `/api/products/${id}`
              ? {
                  id,
                  name: `Produto sintético ${id}`,
                  internalName: `Produto sintético ${id}`,
                }
              : [],
        });
      if (url.origin !== origin) return route.abort();
      return route.continue();
    });
    try {
      await page.goto(
        `${origin}/products/${id}/execution-profiles?chainId=94014&sourceReference=experiment:${id}&learningCycleId=${id}`,
      );
      await page
        .getByRole("button", { name: "Nova ficha", exact: true })
        .click();
      const form = page.locator("form").first();
      await form.getByRole("button", { name: "Salvar nova revisão" }).click();
      assert.equal(
        mutations.length,
        0,
        "Campos obrigatórios não devem gerar requisição incompleta",
      );
      const data = payload(id);
      await form.locator('[name="chainId"]').selectOption("94014");
      await form.locator('[name="commercialPlanId"]').selectOption(String(id));
      await form
        .locator('[name="capability"]')
        .selectOption("PERSONALIZED_IMAGES");
      await form.locator('[name="actor"]').fill("Operador sintético");
      for (const [key, value] of Object.entries(data.contract)) {
        if (
          [
            "capability",
            "audiovisualRequired",
            "scenarios",
            "productionBudget",
          ].includes(key)
        )
          continue;
        await form
          .locator(`[name="${key}"]`)
          .fill(Array.isArray(value) ? value.join("\n") : String(value));
      }
      for (const [key, value] of Object.entries(data.contract.productionBudget))
        await form.locator(`[name="production-${key}"]`).fill(String(value));
      for (const scenario of data.contract.scenarios)
        for (const [key, value] of Object.entries(scenario))
          if (key !== "code")
            await form
              .locator(`[name="${scenario.code}-${key}"]`)
              .fill(String(value));
      const save = form.getByRole("button", { name: "Salvar nova revisão" });
      await save.click();
      await expect(save).toBeDisabled();
      await expect(save.locator(".spinner-border")).toBeVisible();
      await expect(
        page.getByRole("heading", {
          name: "Imagens personalizadas · v1",
          exact: true,
        }),
      ).toBeVisible();
      const profile = (await request(base(id)))[0];
      await expect(
        page.locator('a[href*="/value-chain-history/processes/"]'),
      ).toHaveCount(0);
      const bindForm = page.locator("form").filter({
        has: page.getByRole("button", { name: "Vincular ficha à execução" }),
      });
      await bindForm.locator('[name="actor"]').fill("Operador sintético");
      await bindForm.getByRole("button").click();
      await expect(bindForm.getByRole("button")).toBeEnabled();
      const binding = (await request(`${base(id)}/${profile.id}`)).bindings[0];
      assert.equal(binding.sourceReference, `experiment:${id}`);
      assert.equal(binding.learningCycleId, id);
      await page
        .getByRole("button", {
          name: "Solicitar ou consultar parecer de Plutus",
        })
        .click();
      await expect(page.getByText(/ANALYSIS · PENDING/)).toBeVisible();
      const financialId = (await request(`${base(id)}/${profile.id}`))
        .reviews[0].financialExecutionId;
      const reviewForm = page.locator("form").filter({
        has: page.getByRole("button", {
          name: "Registrar decisão financeira",
        }),
      });
      await reviewForm.locator('[name="checkpoint"]').selectOption("OFFER");
      await reviewForm
        .locator('[name="financialExecutionId"]')
        .fill(String(financialId));
      await reviewForm
        .locator('[name="reviewedBy"]')
        .fill("Responsável de teste");
      await reviewForm
        .locator('[name="rationale"]')
        .fill("Parecer sintético conferido sem receita real.");
      await reviewForm.locator('[name="decision"]').selectOption("APPROVE");
      await reviewForm.getByRole("button").click();
      await expect(
        page.getByRole("alert").filter({ hasText: "não está concluído" }),
      ).toBeVisible();
      await request(`/fixture/finance/${financialId}/complete`, {});
      for (const checkpoint of [
        "OFFER",
        "DELIVERY_DESIGN",
        "HOMOLOGATION",
        "OPERATION",
      ]) {
        await reviewForm
          .locator('[name="checkpoint"]')
          .selectOption(checkpoint);
        await reviewForm.getByRole("button").click();
        await expect(
          page.getByText(new RegExp(`${checkpoint} · COMPLETED · Aprovado`)),
        ).toBeVisible();
      }
      await page
        .getByText("Subprocessos e versões fixadas", { exact: true })
        .click();
      for (const link of await page
        .locator('a[href*="/value-chain-history/processes/"]')
        .all()) {
        const href = new URL(await link.getAttribute("href"), origin);
        assert.equal(href.searchParams.get("learningCycleId"), String(id));
        assert.equal(
          href.searchParams.get("sourceReference"),
          `experiment:${id}`,
        );
        assert.equal(href.searchParams.get("chainId"), "94014");
      }
      const [activityRead] = await Promise.all([
        page.waitForRequest((r) =>
          new URL(r.url()).pathname.endsWith("/activity-executions"),
        ),
        page
          .locator('a[href*="/value-chain-history/processes/"]')
          .first()
          .click(),
      ]);
      const selectedContext = new URL(activityRead.url());
      assert.equal(
        selectedContext.searchParams.get("sourceReference"),
        `experiment:${id}`,
      );
      assert.equal(
        selectedContext.searchParams.get("learningCycleId"),
        String(id),
      );
      assert.equal(selectedContext.searchParams.get("chainId"), "94014");
      await page.goBack();
      await expect(
        page.getByRole("heading", {
          name: "Imagens personalizadas · v1",
          exact: true,
        }),
      ).toBeVisible();
      const reserved = await request(`/fixture/reserve/${id}`, {
        operationKey: "browser-reserve",
        units: 2,
      });
      await request(`/fixture/settle/${id}/${reserved.reservationId}`, {
        actualBrl: null,
        failed: true,
      });
      await page.reload();
      await expect(
        page.getByText(/COST_PENDING · Teste segregado/),
      ).toBeVisible();
      const settle = page.locator("form").filter({
        has: page.getByRole("button", {
          name: "Registrar conciliação do consumo",
        }),
      });
      for (const [key, value] of Object.entries({
        consumptionId: reserved.reservationId,
        actualBrl: 3,
        providerReceipt: "internal://fixture/provider-receipt",
        reviewedBy: "Financeiro sintético",
        rationale: "Comprovante local com câmbio de teste",
      }))
        await settle.locator(`[name="${key}"]`).fill(String(value));
      await settle.locator('[name="failed"]').check();
      await settle.getByRole("button").click();
      await expect(
        page.getByText(/FAILED_CHARGED · Teste segregado/),
      ).toBeVisible();
      await page.screenshot({
        path: `${output}/${name}-profile.png`,
        fullPage: true,
      });
      assert.equal(
        await page.evaluate(
          () => document.documentElement.scrollWidth > innerWidth + 1,
        ),
        false,
        `Overflow ${name}`,
      );
      await page.getByRole("button", { name: "Criar nova revisão" }).click();
      const copy = page.locator("form").first();
      await copy.locator('[name="capability"]').selectOption("DIGITAL_PACKAGE");
      await copy.locator('[name="noVariableAiCost"]').check();
      await expect(copy.locator('[name="costModel"]')).toBeDisabled();
      await expect(
        copy.locator('[name="maximumAttemptCostBrl"]'),
      ).toBeDisabled();
      await expect(copy.locator('[name="production-costModel"]')).toBeEnabled();
      await copy.locator('[name="actor"]').fill("Nova revisão sintética");
      await copy.getByRole("button", { name: "Salvar nova revisão" }).click();
      await expect(
        page.getByRole("heading", {
          name: "Pacote digital · v1",
          exact: true,
        }),
      ).toBeVisible();
      const staticProfile = (await request(base(id)))[0];
      assert.equal(staticProfile.contract.costModel, "NO_VARIABLE_AI_COST");
      assert.equal(staticProfile.contract.maximumDeliveryCostBrl, 0);
      assert.equal(
        staticProfile.contract.productionBudget.maximumTotalCostBrl,
        8,
      );
      await page.getByRole("button", { name: "Criar nova revisão" }).click();
      await expect(
        page.locator('input[name="noVariableAiCost"]'),
      ).toBeChecked();
      await page.getByRole("button", { name: "Fechar formulário" }).click();
      assert.equal(
        (await request(`/fixture/context/${id}`)).profileId,
        profile.id,
        "Nova revisão não muda execução anterior",
      );
      await page.goto(
        `${origin}/products/${id}/execution-profiles?profileId=${(await request(base(id)))[0].id}&chainId=94014`,
      );
      errorMode = true;
      await page.reload();
      await expect(
        page
          .getByRole("alert")
          .filter({ hasText: "Não foi possível consultar" }),
      ).toBeVisible({ timeout: 20000 });
      errorMode = false;
      await page.getByRole("button", { name: "Tentar novamente" }).click();
      await expect(
        page.getByRole("heading", {
          name: "Pacote digital · v1",
          exact: true,
        }),
      ).toBeVisible();
      assert.deepEqual(errors, []);
      results.push({
        device: name,
        status: "PASS",
        productId: id,
        profileId: profile.id,
        mutations: mutations.length,
      });
    } catch (error) {
      await page.screenshot({
        path: `${output}/${name}-failure.png`,
        fullPage: true,
      });
      await writeFile(
        `${output}/${name}-failure.txt`,
        JSON.stringify(
          {
            errors,
            body: await page.locator("body").innerText(),
            error: String(error),
          },
          null,
          2,
        ),
      );
      throw error;
    } finally {
      await context.close();
    }
  }
  // O host administrativo usa HTTP; localhost sozinho não reproduz a ausência de randomUUID.
  const id = 94060;
  const profile = await request(base(id), payload(id));
  await request(`${base(id)}/${profile.id}/bindings`, {
    sourceReference: `experiment:${id}`,
    learningCycleId: id,
    actor: "Homologação HTTP local",
  });
  await approve(id, profile);
  const context = await browser.newContext({
    viewport: { width: 1440, height: 1000 },
  });
  const page = await context.newPage();
  const httpOrigin = "http://profile-local.test";
  const calls = [],
    errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.route("**/*", async (route) => {
    const url = new URL(route.request().url());
    const headers = {
      "access-control-allow-origin": httpOrigin,
      "access-control-allow-credentials": "true",
      "access-control-allow-methods": "GET, POST, OPTIONS",
      "access-control-allow-headers": "content-type",
    };
    if (route.request().method() === "OPTIONS")
      return route.fulfill({ status: 204, headers });
    if (
      url.pathname === "/api/image-generator/generations" &&
      route.request().method() === "POST"
    ) {
      const body = route.request().postDataJSON();
      calls.push(body);
      assert.equal(body.productId, id);
      assert.equal(body.commercialPlanId, id);
      assert.equal(body.experimentId, id);
      const response = await fetch(backend + `/fixture/reserve/${id}`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ operationKey: body.operationKey, units: 2 }),
      });
      const reserved = await response.json();
      if (!response.ok)
        return route.fulfill({
          status: response.status,
          headers,
          json: { message: reserved.detail ?? reserved.message },
        });
      await request(`/fixture/settle/${id}/${reserved.reservationId}`, {
        actualBrl: null,
        failed: false,
      });
      return route.fulfill({
        headers,
        json: {
          jobId: "fixture-http-batch",
          failures: [],
          images: [1, 2].map((n) => ({
            jobId: `fixture-http-image-${n}`,
            model: "gpt-image-2.5-sunburst",
            serviceTier: "fixture",
            outputFormat: "png",
            imageBase64:
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j1ioAAAAASUVORK5CYII=",
            generatedAt: "2026-09-14T00:00:00Z",
          })),
        },
      });
    }
    if (url.pathname.startsWith("/api/")) {
      const json =
        url.pathname === "/api/products"
          ? [{ id, name: "Produto HTTP sintético" }]
          : url.pathname === "/api/planning/commercial-plans"
            ? [{ id, name: "Plano HTTP sintético", experimentId: id }]
            : url.pathname === "/api/experiments"
              ? [{ id, productId: id, name: "Experimento HTTP sintético" }]
              : [];
      return route.fulfill({ headers, json });
    }
    if (url.origin !== httpOrigin) return route.abort();
    const response = await fetch(origin + url.pathname + url.search);
    return route.fulfill({
      status: response.status,
      contentType:
        response.headers.get("content-type") ?? "application/octet-stream",
      body: Buffer.from(await response.arrayBuffer()),
    });
  });
  try {
    await page.goto(httpOrigin + "/ai/image-generator");
    assert.equal(await page.evaluate(() => isSecureContext), false);
    assert.equal(
      await page.evaluate(() => typeof crypto.randomUUID),
      "undefined",
    );
    await page.locator("#image-generator-product").selectOption(String(id));
    await page.locator("#image-generator-plan").selectOption(String(id));
    await page.locator("#image-generator-experiment").selectOption(String(id));
    await page
      .locator("#image-generator-prompt")
      .fill("Imagem sintética de teste local; nenhum provedor pago.");
    const generate = page.getByRole("button", {
      name: "Gerar imagem",
      exact: true,
    });
    await generate.click();
    await expect.poll(() => calls.length).toBe(1);
    await expect(generate).toBeEnabled();
    await generate.click();
    await expect(page.getByText(/Há custo desconhecido/)).toBeVisible();
    assert.equal(calls.length, 2);
    assert.equal(new Set(calls.map((c) => c.operationKey)).size, 2);
    assert.ok(
      calls.every((c) => /^[a-zA-Z0-9._:-]{1,100}$/.test(c.operationKey)),
    );
    const current = await request(`${base(id)}/${profile.id}`);
    assert.equal(current.consumption.length, 1);
    assert.equal(current.consumption[0].status, "COST_PENDING");
    assert.deepEqual(errors, []);
    await page.screenshot({
      path: `${output}/desktop-http-image-generator.png`,
      fullPage: true,
    });
    results.push({
      device: "desktop-http",
      status: "PASS",
      productId: id,
      secureContext: false,
      generatedBatches: 1,
      blockedBatches: 1,
    });
  } catch (error) {
    await page.screenshot({
      path: `${output}/desktop-http-failure.png`,
      fullPage: true,
    });
    await writeFile(
      `${output}/desktop-http-failure.txt`,
      JSON.stringify(
        {
          errors,
          error: String(error),
          body: await page.locator("body").innerText(),
        },
        null,
        2,
      ),
    );
    throw error;
  } finally {
    await context.close();
  }
} finally {
  await browser.close();
}
await writeFile(
  `${output}/browser-results.json`,
  JSON.stringify({ status: "PASS", results, paidCalls: 0 }, null, 2),
);
console.log(JSON.stringify(results));
