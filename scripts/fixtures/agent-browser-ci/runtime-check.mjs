import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import { chromium, devices } from "playwright-core";

// Verifica a imagem final sem rede externa, credenciais ou modelo de IA.
assert.notEqual(process.getuid(), 0, "O runtime deve ser não-root");
assert.match(await readFile("/etc/ssl/certs/ca-certificates.crt", "utf8"), /BEGIN CERTIFICATE/);
assert.match(process.version, /^v2[0-9]\./);
const browser = await chromium.launch({ headless: true });
try {
  for (const [name, options] of [
    ["desktop", { viewport: { width: 1440, height: 900 } }],
    ["iPhone 15 Pro", devices["iPhone 15 Pro"]],
    ["Pixel 7", devices["Pixel 7"]],
  ]) {
    const context = await browser.newContext(options);
    const page = await context.newPage();
    await page.setContent('<html lang="pt-BR"><meta name="viewport" content="width=device-width,initial-scale=1"><button onclick="this.textContent=\'Concluído\'">Continuar</button></html>');
    await page.getByRole("button", { name: "Continuar" }).click();
    assert.equal(await page.getByRole("button").textContent(), "Concluído");
    const pixels = await page.screenshot();
    assert.equal(pixels.subarray(1, 4).toString(), "PNG");
    console.log(JSON.stringify({
      device: name, browser: await browser.version(), node: process.version,
      uid: process.getuid(), pixels: pixels.length,
      sha256: createHash("sha256").update(pixels).digest("hex"),
    }));
    await context.close();
  }
} finally {
  await browser.close();
}
