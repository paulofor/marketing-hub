/* Homologa o relatório real do serviço local em três dispositivos; intercepta todas as APIs. */
const fs = require("node:fs");
const crypto = require("node:crypto");
const assert = require("node:assert/strict");
const { chromium, devices, expect } = require("@playwright/test");
const evidenceDir =
  process.env.PREFLIGHT_PROCESS_EVIDENCE_DIR ||
  "/tmp/preflight-current-evidence";
const frontendUrl =
  process.env.PREFLIGHT_PROCESS_FRONTEND_URL || "http://127.0.0.1:4173";
const scenarios = (
  process.env.PREFLIGHT_PROCESS_SCENARIOS || "current,stale,pending"
).split(",");
assert.ok(
  scenarios.length &&
    scenarios.every((s) => ["current", "stale", "pending"].includes(s)),
);
(async () => {
  const browser = await chromium.launch({
    executablePath: "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  const results = [];
  try {
    for (const scenario of scenarios) {
      const originalReport = JSON.parse(
        fs.readFileSync(`${evidenceDir}/${scenario}-report.json`, "utf8"),
      );
      for (const [name, profile] of [
        ["desktop", { viewport: { width: 1440, height: 1000 } }],
        ["iPhone-15-Pro", devices["iPhone 15 Pro"]],
        ["Pixel-7", devices["Pixel 7"]],
      ]) {
        let report = structuredClone(originalReport);
        const context = await browser.newContext(profile);
        const page = await context.newPage();
        const errors = [],
          writes = [],
          calls = [];
        let accepted = false,
          simulatedPosts = 0;
        const runId = 96025;
        const experimentId = Number(
          report.currentExecutionReference.split(":")[1],
        );
        const identity = `publication:96020;page-sha256:${"a".repeat(64)};quartzo-fingerprint:${"b".repeat(64)}`;
        const codes = [
          "LANDING_QUALITY_REVIEW_APPROVED",
          "CHECKOUT_AND_DELIVERY_CAN_BE_COMPLETED",
          "META_EFFECTIVE_STATUS_CONFIRMED",
          "DATA_FRESHNESS_VALID",
        ];
        const preflight = () => ({
          runId,
          runStatus: accepted ? "READY_TO_PUBLISH" : "PREFLIGHT_PENDING",
          hasBlockers: !accepted,
          requiredLandingEvidenceReference: identity,
          currentEvidenceBlockReason: accepted
            ? null
            : "Homologação pendente local.",
          canRenewTechnicalHomologation: false,
          gates: codes.map((gateCode) => ({
            gateCode,
            gateGroup: "FUNCTIONAL_E2E",
            status: accepted ? "PASS" : "PENDING",
            severity: accepted ? "INFO" : "WARNING",
            summary: "Contrato local simulado.",
            evaluatedAt: "2026-10-04T00:00:00Z",
            evaluatorType: "DETERMINISTIC",
            evaluatorVersion: "local.v1",
          })),
        });
        page.on("pageerror", (err) => errors.push(err.message));
        await page.route(
          (url) => url.pathname.startsWith("/api/"),
          async (route) => {
            const req = route.request(),
              path = new URL(req.url()).pathname;
            calls.push(path);
            if (
              scenario === "pending" &&
              req.method() === "POST" &&
              path === `/api/experiment-runs/${runId}/homologation-results`
            ) {
              const payload = req.postDataJSON();
              assert.deepEqual(
                payload.gates.map((g) => g.gateCode).sort(),
                [...codes].sort(),
              );
              assert.ok(
                payload.gates.every(
                  (g) =>
                    g.status === "PASS" &&
                    g.evidenceReference &&
                    g.summary &&
                    g.evidenceReference.length <= 512 &&
                    g.summary.length <= 512,
                ),
              );
              assert.ok(
                payload.gates
                  .find((g) => g.gateCode === codes[0])
                  .evidenceReference.startsWith(identity),
              );
              accepted = true;
              simulatedPosts++;
              report = JSON.parse(
                fs.readFileSync(`${evidenceDir}/current-report.json`, "utf8"),
              );
              return route.fulfill({ json: preflight() });
            }
            if (req.method() !== "GET") {
              writes.push({ path, method: req.method() });
              return route.fulfill({
                status: 409,
                json: { detail: "Escrita proibida na homologação local" },
              });
            }
            if (path.endsWith("/activity-executions"))
              return route.fulfill({ json: report });
            if (path.endsWith("/automation/v1"))
              return route.fulfill({
                json: {
                  id: 96310,
                  productId: report.productId,
                  processDefinitionId: report.selectedProcessDefinitionId,
                  chainId: 96007,
                  learningCycleId: null,
                  sourceReference: report.currentExecutionReference,
                  status: "WAITING_INPUT",
                  reason: report.currentActivityStateReason,
                  currentActivityId: report.currentActivityId,
                  currentActivityName: report.currentActivityName,
                  currentOwnerName: "Backend",
                  totalActivities: report.selectedActivityCount,
                  completedActivities: report.completedActivityCount,
                  remainingActivities: report.remainingActivityCount,
                  omittedActivities: 0,
                  completionPercentage: report.completedActivityCount * 25,
                  knownCostUsd: null,
                  costCoverage: "NO_EXECUTIONS",
                  canStart: false,
                  canPause: false,
                  canResume: false,
                  automaticExecution: true,
                  childRunId: null,
                  revision: 1,
                },
              });
            if (path.includes("/value-chain-positions/"))
              return route.fulfill({
                json: {
                  productId: report.productId,
                  resolutionStatus: "IDENTIFIED",
                  resolutionMessage: "Contexto de teste local",
                  chainDefinitionId: 96007,
                  chainVersion: 1,
                  processDefinitionId: report.selectedProcessDefinitionId,
                  processName: report.processName,
                  sequenceNumber: 1,
                  processCount: 1,
                  processMeasurements: [],
                },
              });
            if (path.endsWith("/runs"))
              return route.fulfill({
                json:
                  scenario === "pending"
                    ? [
                        {
                          id: runId,
                          experimentId,
                          runNumber: 2,
                          mode: "PRODUCTION",
                          status: accepted
                            ? "READY_TO_PUBLISH"
                            : "PREFLIGHT_PENDING",
                          evidenceValidity: "NOT_EVALUATED",
                          dataQualityStatus: accepted ? "VALID" : "UNKNOWN",
                          stopPolicy: "MANUAL_ONLY",
                          requestedAt: "2026-10-04T00:00:00Z",
                        },
                      ]
                    : [],
              });
            if (
              scenario === "pending" &&
              path === `/api/experiment-runs/${runId}/preflight`
            )
              return route.fulfill({ json: preflight() });
            return route.fulfill({
              status: 404,
              json: { detail: "Dependência não utilizada neste teste" },
            });
          },
        );
        await page.goto(
          `${frontendUrl}/products/${report.productId}/value-chain-history/processes/${report.selectedProcessDefinitionId}/activities?chainId=96007&sourceReference=${encodeURIComponent(report.currentExecutionReference)}`,
          { waitUntil: "domcontentloaded" },
        );
        await expect(
          page.getByRole("heading", { name: new RegExp(report.processName) }),
        ).toBeVisible();
        await expect(
          page.getByRole("link", { name: "Plano comercial", exact: true }),
        ).toHaveAttribute("href", `/planning/${report.commercialPlanId}`);
        if (scenario !== "pending")
          await expect(
            page.locator("#activity-financialGuardrails"),
          ).toContainText("Plano financeiro local vencido");
        const surface = page.locator("#activity-surfaces");
        if (scenario === "pending") {
          await expect(surface).toContainText("Não iniciada");
          const panel = page.locator("#activity-transaction");
          await expect(panel.getByLabel(`Evidência ${codes[0]}`)).toHaveValue(
            identity,
          );
          const button = panel.getByRole("button", {
            name: "Registrar homologação",
            exact: true,
          });
          await expect(button).toBeDisabled();
          for (const code of codes) {
            await panel.getByLabel(`Resultado ${code}`).selectOption("PASS");
            await panel
              .getByLabel(`Evidência ${code}`)
              .fill(
                (code === codes[0] ? identity + ";" : "") +
                  "fixture-local:sem-gasto",
              );
            await panel
              .getByLabel(`Conclusão ${code}`)
              .fill("Comportamento determinístico simulado e segregado.");
          }
          for (const field of ["Evidência", "Conclusão"]) {
            const input = panel.getByLabel(`${field} ${codes[3]}`);
            const previous = await input.inputValue();
            await input.fill("a".repeat(513));
            await expect(input).toHaveValue("a".repeat(513));
            await expect(input).toHaveAttribute("aria-invalid", "true");
            await expect(button).toBeDisabled();
            assert.equal(simulatedPosts, 0);
            await input.fill("a".repeat(512));
            await expect(input).toHaveAttribute("aria-invalid", "false");
            await expect(button).toBeEnabled();
            await input.fill(previous);
          }
          await expect(button).toBeEnabled();
          await button.click();
          await expect(surface).toContainText("Concluída");
          await expect(
            page.locator("#activity-financialGuardrails"),
          ).toContainText("Plano financeiro local vencido");
          assert.equal(simulatedPosts, 1);
        } else if (scenario === "stale") {
          await expect(surface).toContainText("mudaram após a homologação");
          await expect(surface).toContainText("Não iniciada");
          await expect(
            surface.getByText("Objetivo comprovado", { exact: true }),
          ).toHaveCount(0);
        } else await expect(surface).toContainText("Concluída");
        const body = await page.locator("body").innerText();
        fs.writeFileSync(`${evidenceDir}/local-${scenario}-${name}.txt`, body);
        const screenshot = `${evidenceDir}/local-${scenario}-${name}.png`;
        await page.screenshot({ path: screenshot, fullPage: true });
        const dimensions = await page.evaluate(() => ({
          scroll: document.documentElement.scrollWidth,
          width: window.innerWidth,
        }));
        assert.ok(
          dimensions.scroll <= dimensions.width + 2,
          JSON.stringify(dimensions),
        );
        assert.deepEqual(errors, []);
        assert.deepEqual(writes, []);
        results.push({
          scenario,
          profile: name,
          completed: report.completedActivityCount,
          planId: report.commercialPlanId,
          sourceReference: report.currentExecutionReference,
          writes: 0,
          simulatedPosts,
          pageErrors: 0,
          width: dimensions.width,
          screenshotSha256: crypto
            .createHash("sha256")
            .update(fs.readFileSync(screenshot))
            .digest("hex"),
        });
        await context.close();
      }
    }
    fs.writeFileSync(
      `${evidenceDir}/local-ui-results.json`,
      JSON.stringify(results, null, 2),
    );
    console.log(JSON.stringify(results));
  } finally {
    await browser.close();
  }
})().catch((err) => {
  console.error(err);
  process.exitCode = 1;
});
