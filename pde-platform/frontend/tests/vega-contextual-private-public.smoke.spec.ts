import { test, expect } from "@playwright/test";

test("variante contextual publicada mantém acesso privado e não cria trabalho", async ({
  page,
  request,
}) => {
  const version = "musa-pde-entry-v16-modelagem-internacional";
  const capability = await request.get("/api/pde/vega/style/v1/contract");
  expect(capability.ok()).toBeTruthy();
  expect(await capability.json()).toMatchObject({
    prototypeVersion: version,
    generationMode: "DETERMINISTIC_CONTEXTUAL_RULES",
    providerCalls: 0,
    trafficClass: "AGENT_VALIDATION",
    sessionLimit: 18,
  });
  expect(
    (await request.get("/api/pde/vega/style/v1/access/workspace")).status(),
  ).toBe(401);
  const writes: string[] = [],
    errors: string[] = [];
  page.on("request", (req) => {
    if (req.method() !== "GET") writes.push(req.method());
  });
  page.on("pageerror", (error) => errors.push(error.message));
  const response = await page.goto(`/experiences/${version}/agent-validation`);
  expect(response?.headers()["x-robots-tag"]).toContain("noindex");
  await expect(
    page.getByRole("heading", {
      name: "Seu guarda-roupa pode ter mais a sua cara.",
    }),
  ).toBeVisible();
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(
    page.getByRole("article", { name: "Sua orientação pronta" }),
  ).toHaveCount(0);
  expect(errors).toEqual([]);
  expect(writes).toEqual([]);
});
