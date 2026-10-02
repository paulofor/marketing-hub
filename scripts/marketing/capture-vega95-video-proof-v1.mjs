/** Captura a interface real em QA e preserva versão e hashes para a montagem de Vega. */
import { createRequire } from "node:module";
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { sha256 } from "./video-candidate-contract.mjs";

const require = createRequire(import.meta.url);
const { chromium, devices } = require("playwright");
const [sourceDir] = process.argv.slice(2);
if (!sourceDir)
  throw new Error("Uso: node capture-vega95-video-proof-v1.mjs fontes");
const brief = JSON.parse(
  await readFile(
    join(dirname(fileURLToPath(import.meta.url)), "vega95-video-v1/brief.json"),
  ),
);
await mkdir(sourceDir, { recursive: true });
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_BIN || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
const page = await browser.newPage({
  ...devices["Pixel 7"],
  deviceScaleFactor: 2,
});
const captures = [];
const writes = [];
let productSnapshot;
page.on("request", (request) => {
  if (request.method() !== "GET" && request.url().includes("/api/"))
    writes.push(new URL(request.url()).pathname);
});
const capture = async (file, selector, sourceUrl = page.url()) => {
  const target = page.locator(selector);
  await target.scrollIntoViewIfNeeded();
  await target.screenshot({
    path: join(sourceDir, file),
    animations: "disabled",
  });
  captures.push({
    file,
    sha256: sha256(await readFile(join(sourceDir, file))),
    text: await target.innerText(),
    sourceUrl,
  });
};
try {
  const responsePromise = page.waitForResponse((r) =>
    r.url().includes("/api/pde/products/metodo-musa-7-dias"),
  );
  await page.goto(
    "https://v8.clubemusa.com.br/?mh_preview=qa&pde_analytics=off",
    { waitUntil: "networkidle", timeout: 60000 },
  );
  const response = await responsePromise;
  productSnapshot = await response.json();
  if (
    !response.ok() ||
    !response.url().includes(`experienceVersion=${brief.experienceVersion}`)
  )
    throw new Error("Versão pública divergente.");
  const choices = [
    "Calça e camisa ou blusa",
    "Trabalho ou reunião",
    "Elegância discreta",
    "Ajustar manga, barra ou caimento",
  ];
  for (const [index, choice] of choices.entries()) {
    await page.getByRole("button", { name: choice, exact: true }).waitFor();
    await capture(`choice-${index + 1}.png`, ".public-question-card");
    await page.getByRole("button", { name: choice, exact: true }).click();
    await page.waitForTimeout(220);
  }
  await page
    .getByRole("button", { name: "Descobrir meu primeiro ajuste", exact: true })
    .click();
  await page.locator(".public-diagnostic-result").waitFor({ timeout: 45000 });
  await capture("result.png", ".public-result-hero");
  await capture("microaction.png", ".public-today-action");
  await capture("save.png", ".public-email-capture");
  const materialPath =
    "pde-platform/frontend/public/materials/musa-v12/checklist-antes-de-sair.html";
  const material = await readFile(
    join(dirname(fileURLToPath(import.meta.url)), "../../", materialPath),
  );
  await page.setContent(material.toString(), { waitUntil: "load" });
  await capture(
    "checklist.png",
    "main > fieldset:first-of-type",
    `repo:${materialPath}`,
  );
  captures.at(-1).sourceHtmlSha256 = sha256(material);
  captures.at(-1).captureKind =
    "VERSIONED_MATERIAL_PREVIEW_NOT_LIVE_PUBLICATION";
  if (writes.some((path) => path !== "/api/pde/public/presence-diagnostic"))
    throw new Error("A captura tentou gerar evento, pagamento ou cadastro.");
  const media = [
    {
      file: "source-2810.mp4",
      mediaAssetId: brief.sourceMediaAssetId,
      sourceVideoAssetId: brief.sourceVideoAssetId,
      sha256: brief.sourceSha256,
    },
  ];
  if (
    sha256(await readFile(join(sourceDir, media[0].file))) !== media[0].sha256
  )
    throw new Error("Filme de origem divergente.");
  const manifest = {
    capturedAt: new Date().toISOString(),
    experienceVersion: brief.experienceVersion,
    qaSegregated: true,
    writes,
    customerEmailsSubmitted: 0,
    paidProviderCalls: 0,
    productSnapshotSha256: sha256(Buffer.from(JSON.stringify(productSnapshot))),
    captures,
    media,
    limitations: [
      "Capturas demonstram a versão atual do produto; não representam uso ou satisfação de cliente.",
      "Microação categorial; a encenação não é uma prescrição específica do aplicativo.",
    ],
  };
  await writeFile(
    join(sourceDir, "capture-manifest.json"),
    JSON.stringify(manifest, null, 2),
  );
  console.log(
    JSON.stringify({
      captured: captures.length,
      experienceVersion: brief.experienceVersion,
      writes,
    }),
  );
} finally {
  await browser.close();
}
