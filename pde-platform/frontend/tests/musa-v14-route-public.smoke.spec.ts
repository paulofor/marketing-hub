import { expect, test } from "@playwright/test";

// Prova de publicação do software; a homologação comercial é uma passagem independente.
test("candidata isolada comprova domínio, imagem e saúde sem executar o produto", async ({
  request,
}) => {
  const diagnostics = await request.get("/version-diagnostics.json");
  expect(diagnostics.ok()).toBeTruthy();
  const identity = await diagnostics.json();
  expect(identity.version).toBe("v9");
  expect(identity.experienceVersion).toBe(
    "musa-pde-entry-v14-primeiro-ajuste-aplicavel",
  );
  expect(identity.productSlug).toBe("metodo-musa-7-dias");
  expect(identity.frontendSourceSha256).toBe(
    process.env.PDE_EXPECTED_FRONTEND_SOURCE_SHA256,
  );
  expect(identity.commitSha).toBe(process.env.PDE_EXPECTED_COMMIT);
  expect(identity.publicUrl).toBe("https://v9.clubemusa.com.br");
  const html = await request.get(
    "/experiences/musa-pde-entry-v14-primeiro-ajuste-aplicavel",
  );
  expect(html.ok()).toBeTruthy();
  expect(html.headers()["cache-control"]).toContain("no-store");
  expect(html.headers()["x-robots-tag"]).toContain("noindex");
  expect(await html.text()).toContain('id="root"');
  expect((await request.get("/healthz")).ok()).toBeTruthy();
});
