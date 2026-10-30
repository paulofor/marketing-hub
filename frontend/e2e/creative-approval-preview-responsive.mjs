import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";
import { chromium, devices, expect } from "@playwright/test";

const base = process.env.FRONTEND_BASE_URL || "http://127.0.0.1:15173";
const output = process.env.EVIDENCE_DIR || "/tmp/creative-approval-preview";
await mkdir(output, { recursive: true });
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
const image =
  '<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="1350"><rect width="1080" height="1350" fill="#eee8fc"/><rect x="60" y="60" width="960" height="1050" rx="50" fill="#7755aa"/><text x="100" y="350" font-size="85" fill="white">AMOSTRA DE TESTE</text><text x="100" y="1240" font-size="65">Imagem inteira · QA</text></svg>';

try {
  for (const [device, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    for (const productId of [95004, 95007]) {
      const context = await browser.newContext(options);
      const page = await context.newPage();
      const failures = [],
        mutations = [];
      let broken = false;
      page.on("pageerror", (error) => failures.push(error.message));
      const taskId = productId + 1000;
      const assets = [1, 2].map((index) => ({
        taskId,
        artifactId: taskId + index,
        title: `Criativo ${index}`,
        contentUrl: `/api/agent-tasks/${taskId}/visual-evidence/${taskId + index}/content`,
        sha256: `${index}`.repeat(64),
      }));
      const control = {
        executorType: "HUMAN",
        interactionType: "APPROVAL",
        actionAvailable: true,
        actionLabel: "Aprovar uso privado da peça",
        description: "Seleção privada de teste.",
        availabilityReason: "Peças revisadas por Psique e Têmis.",
        confirmationRequired: true,
        confirmationTitle: "Confirmar uso privado",
        confirmationMessage: "Revise as imagens e confirme seu uso privado.",
        confirmationToken: `CONFIRM:QA:${productId}`,
        decisionMode: "REVIEW_AND_ACCEPT",
        requirements: [],
        reviewAssets: assets,
      };
      await context.route("**/api/**", async (route) => {
        const request = route.request();
        const path = new URL(request.url()).pathname;
        if (!path.startsWith("/api/")) return route.continue();
        if (path.includes("/visual-evidence/")) {
          assert.equal(request.method(), "GET");
          assert.ok(assets.some((asset) => asset.contentUrl === path));
          if (broken && path === assets[1].contentUrl)
            return route.fulfill({ status: 503, body: "" });
          return route.fulfill({ contentType: "image/svg+xml", body: image });
        }
        if (request.method() !== "GET") {
          mutations.push({ path, body: request.postDataJSON() });
          return route.fulfill({
            json: {
              sourceReference: `experiment:${productId}`,
              tasks: [],
              operationalState: "COMPLETED",
            },
          });
        }
        let data = [];
        if (path.endsWith("process-context") || path.endsWith("automation/v1"))
          data = null;
        if (path.endsWith("activity-executions"))
          data = {
            productId,
            productName: `Produto QA ${productId}`,
            productInternalName: `QA ${productId}`,
            selectedProcessDefinitionId: 95121,
            processCode: "creative-production-approval",
            processName: "Produção e revisão de criativos",
            selectedProcessVersionNumber: 1,
            selectedProcessStatus: "PUBLISHED",
            currentExecutionReference: `experiment:${productId}`,
            operationalState: "IN_PROGRESS",
            activityCount: 1,
            selectedActivityCount: 1,
            completedActivityCount: 0,
            remainingActivityCount: 1,
            blockedActivityCount: 0,
            activitiesWithTasksCount: 0,
            uniqueTaskCount: 0,
            knownEstimatedCostUsd: 0,
            costCoverage: "NO_EXECUTIONS",
            currentActivityId: "human",
            activities: [
              {
                activityDefinitionId: 95126,
                activityId: "human",
                activityName: "Registrar decisão humana de uso",
                activityOwnerName: "Operador humano",
                activityObjective: "Confira os criativos antes da decisão.",
                sequenceNumber: 6,
                selectedVersionActivity: true,
                operationalState: "NOT_STARTED",
                taskCount: 0,
                tasks: [],
                executionControl: control,
              },
            ],
          };
        return route.fulfill({ json: data });
      });
      const url = `${base}/products/${productId}/value-chain-history/processes/95121/activities?chainId=95027&sourceReference=experiment%3A${productId}#activity-human`;
      await page.goto(url);
      const gallery = page.getByRole("region", {
        name: "Criativos para sua decisão",
      });
      try {
        await expect(gallery).toBeVisible({ timeout: 15000 });
      } catch (error) {
        console.error({
          failures,
          body: (await page.locator("body").innerText()).slice(0, 3000),
        });
        throw error;
      }
      for (const image of await gallery.getByRole("img").all()) {
        await expect(image).toHaveJSProperty("naturalWidth", 1080);
        assert.equal(
          await image.evaluate(
            (element) => getComputedStyle(element).objectFit,
          ),
          "contain",
        );
      }
      const approve = page.getByRole("button", {
        name: "Aprovar uso privado da peça",
      });
      await expect(approve).toBeEnabled();
      const popupPromise = context.waitForEvent("page");
      await gallery
        .getByRole("link", { name: "Abrir criativo 1 em tamanho original" })
        .click();
      const popup = await popupPromise;
      await popup.waitForLoadState();
      assert.equal(new URL(popup.url()).pathname, assets[0].contentUrl);
      await expect(popup.locator("svg")).toHaveAttribute("width", "1080");
      assert.equal(mutations.length, 0);
      await popup.close();
      assert.ok(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth + 1,
        ),
      );
      await gallery.screenshot({
        path: `${output}/${device}-${productId}.png`,
      });
      await gallery.getByRole("img").first().scrollIntoViewIfNeeded();
      await page.screenshot({
        path: `${output}/${device}-${productId}-viewport.png`,
      });
      broken = true;
      await page.reload();
      await expect(gallery.getByRole("alert")).toContainText(
        "Não foi possível carregar a imagem",
      );
      await expect(approve).toBeDisabled();
      broken = false;
      await page
        .getByRole("button", { name: "Tentar carregar novamente" })
        .click();
      await expect(approve).toBeEnabled();
      assert.equal(mutations.length, 0);
      await approve.click();
      await expect.poll(() => mutations.length).toBe(1);
      assert.deepEqual(mutations[0].body, {
        decision: "APPROVE",
        confirmationToken: control.confirmationToken,
      });
      assert.equal(failures.length, 0, failures.join("\n"));
      console.log(
        `${device} produto ${productId}: prévia, original, recuperação e aceite local aprovados`,
      );
      await context.close();
    }
  }
} finally {
  await browser.close();
}
console.log("Navegação e limpeza concluídas.");
