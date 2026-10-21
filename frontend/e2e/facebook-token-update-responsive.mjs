import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { mkdir } from "node:fs/promises";
const require = createRequire(import.meta.url);
const { chromium, devices, expect } = require("@playwright/test");
const baseUrl = process.env.FRONTEND_BASE_URL ?? "http://127.0.0.1:4173";
assert.equal(new URL(baseUrl).hostname, "127.0.0.1");
const artifacts = process.env.ARTIFACTS_DIR ?? "/tmp/meta-token-browser";
await mkdir(artifacts, { recursive: true });
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
try {
  for (const [profile, options] of [
    ["desktop", { viewport: { width: 1440, height: 1000 } }],
    ["iphone", devices["iPhone 15 Pro"]],
    ["pixel", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(options);
    const page = await context.newPage();
    const errors = [];
    const writes = [];
    let rejected = true;
    let saved = false;
    page.on("pageerror", (error) => errors.push(error.message));
    await page.route("**/*", async (route) => {
      const request = route.request();
      const url = new URL(request.url());
      if (!url.pathname.startsWith("/api/")) {
        if (url.hostname === "127.0.0.1") await route.continue();
        else await route.abort();
        return;
      }
      if (request.method() === "POST") {
        writes.push(url.pathname);
        if (url.pathname === "/api/accounts/facebook/27/token") {
          assert.deepEqual(request.postDataJSON(), {
            accessToken: "synthetic-browser-token",
          });
          if (rejected)
            await route.fulfill({
              status: 422,
              json: { message: "synthetic-browser-token" },
            });
          else {
            saved = true;
            await route.fulfill({
              json: {
                accountId: 27,
                validatedAt: "2026-10-21T02:00:00",
                tokenRenewalEnabled: true,
              },
            });
          }
          return;
        }
        if (url.pathname.endsWith("/token/diagnostics")) {
          await route.fulfill({
            json: {
              accountId: 27,
              tokenSource: "ACCESS_TOKEN",
              hasToken: true,
              tokenDebug: {
                operation: "debug_token",
                status: "SUCCESS",
                httpStatus: 200,
                message: "Token inválido na Meta",
                tokenDebug: { valid: false },
              },
              permissions: [],
              requiredPermissions: [],
              recommendedPermissions: [],
            },
          });
          return;
        }
        throw new Error("Mutação inesperada: " + url.pathname);
      }
      if (url.pathname === "/api/accounts/facebook") {
        await route.fulfill({
          json: [
            {
              id: 27,
              name: "Conta QA",
              currency: "BRL",
              appId: "12345",
              adAccountId: "7777",
              hasAccessToken: true,
              tokenExpired: !saved,
              requiresTokenRenewal: !saved,
              tokenExpiresAt: saved
                ? "2030-01-01T00:00:00"
                : "2026-10-19T22:45:04",
              tokenRenewalEnabled: true,
              tokenRenewalStatus: saved ? "NEVER_ATTEMPTED" : "FAILED",
            },
          ],
        });
        return;
      }
      await route.fulfill({ json: [] });
    });
    await page.goto(`${baseUrl}/accounts/facebook`);
    await page
      .getByRole("button", { name: "Validar token", exact: true })
      .click();
    const diagnostic = page
      .getByRole("status")
      .filter({ hasText: "Diagnóstico seguro Meta" });
    await expect(diagnostic.getByText("Falhou", { exact: true })).toBeVisible();
    await expect(diagnostic.getByText("OK", { exact: true })).toHaveCount(0);
    await page
      .getByRole("button", { name: "Atualizar token", exact: true })
      .click();
    const dialog = page.getByRole("dialog", {
      name: "Atualizar token da Meta",
    });
    const input = dialog.getByLabel("Novo token de usuário");
    await expect(input).toBeFocused();
    await expect(input).toHaveAttribute("type", "password");
    await input.fill("synthetic-browser-token");
    await dialog.getByRole("button", { name: "Cancelar" }).click();
    await expect(dialog).toHaveCount(0);
    assert.equal(writes.filter((url) => url.endsWith("/token")).length, 0);
    await page
      .getByRole("button", { name: "Atualizar token", exact: true })
      .click();
    await expect(input).toHaveValue("");
    const box = await dialog.boundingBox();
    assert.ok(
      box && box.x >= 0 && box.x + box.width <= page.viewportSize().width + 1,
    );
    await page.screenshot({ path: `${artifacts}/${profile}-form.png` });
    await input.fill("synthetic-browser-token");
    await dialog
      .getByRole("button", { name: "Validar e salvar token" })
      .click();
    await expect(dialog.getByRole("alert")).toContainText(
      "A Meta não confirmou",
    );
    await expect(input).toHaveValue("");
    assert.ok(
      !(await page.locator("body").innerText()).includes(
        "synthetic-browser-token",
      ),
    );
    rejected = false;
    await input.fill("synthetic-browser-token");
    await dialog
      .getByRole("button", { name: "Validar e salvar token" })
      .click();
    await expect(dialog).toHaveCount(0);
    await expect(
      page.getByText(/Token validado e atualizado para Conta QA/),
    ).toBeVisible();
    await expect(page.getByText("Token válido", { exact: true })).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Diagnóstico seguro Meta" }),
    ).toHaveCount(0);
    assert.deepEqual(errors, []);
    assert.equal(writes.filter((url) => url.endsWith("/token")).length, 2);
    assert.ok(
      !(
        await page.evaluate(() =>
          JSON.stringify({
            local: { ...localStorage },
            session: { ...sessionStorage },
          }),
        )
      ).includes("synthetic-browser-token"),
    );
    console.log(
      JSON.stringify({
        profile,
        cancel: "passed",
        rejection: "passed",
        update: "passed",
        secrets: "absent",
        diagnostic: "passed",
      }),
    );
    await context.close();
  }
} finally {
  await browser.close();
}
