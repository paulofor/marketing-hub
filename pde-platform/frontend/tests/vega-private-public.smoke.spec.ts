import { expect, test } from "@playwright/test";

test("preparação privada publicada preserva identidade, API e acesso sem criar sessões", async ({
  page,
  request,
}) => {
  const diagnostics = await request.get("/version-diagnostics.json");
  expect(diagnostics.ok()).toBeTruthy();
  const identity = await diagnostics.json();
  expect(identity.version).toBe("v8");
  expect(identity.productSlug).toBe("metodo-musa-7-dias");
  expect(identity.experienceVersion).toBe(
    process.env.PDE_EXPECTED_EXPERIENCE_VERSION,
  );
  expect(identity.frontendSourceSha256).toBe(
    process.env.PDE_EXPECTED_FRONTEND_SOURCE_SHA256,
  );
  expect(identity.commitSha).toBe(process.env.PDE_EXPECTED_COMMIT);

  const response = await request.get("/api/pde/vega/private/v1/contract");
  expect(
    response.ok(),
    `Contrato privado indisponível: HTTP ${response.status()}`,
  ).toBeTruthy();
  const contract = await response.json();
  expect(contract.prototypeVersion).toBe(identity.experienceVersion);
  expect(contract.productSlug).toBe(identity.productSlug);
  expect(contract.agentValidationGenerationMode).toBe("DETERMINISTIC_FIXTURE");
  expect(contract.checkoutMode).toBe("SIMULATED_NO_CHARGE");
  expect(contract.paymentEnabled).toBe(false);
  expect(contract.published).toBe(false);
  expect(contract.mediaSpendBrl).toBe(0);
  expect(contract.syntheticLimits).toEqual({
    sessionsPerCycleVersion: 18,
    attemptsPerSession: 2,
    attemptsPerCycleVersion: 36,
  });
  const protectedQueue = await request.get(
    "/api/pde/vega/private/v1/internal/adjustment/stage-executions/pending?mode=FIXTURE",
  );
  expect(protectedQueue.status()).toBe(403);

  const errors: string[] = [];
  const writes: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("request", (request) => {
    if (request.method() !== "GET")
      writes.push(request.method() + " " + new URL(request.url()).pathname);
  });
  const html = await page.goto("/agent-validation", {
    waitUntil: "domcontentloaded",
  });
  expect(html?.ok()).toBeTruthy();
  expect(html?.headers()["cache-control"]).toContain("no-store");
  expect(html?.headers()["x-robots-tag"]).toContain("noindex");
  expect(html?.headers()["referrer-policy"]).toBe("no-referrer");
  await expect(
    page.getByRole("heading", { name: "Seu primeiro ajuste", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("Experiência privada", { exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    ),
  ).toBe(true);
  expect(errors).toEqual([]);
  expect(writes).toEqual([]);
});
