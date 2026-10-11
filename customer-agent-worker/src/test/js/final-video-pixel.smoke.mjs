import { test } from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import { createRequire } from "node:module";
import { pathToFileURL } from "node:url";
import os from "node:os";
import path from "node:path";
const script = process.env.CUSTOMER_AGENT_FINAL_VIDEO_SCRIPT
  || path.resolve("src/main/resources/browser/final-video-review.mjs");
const { captureVideos } = await import(pathToFileURL(script));
const { videoBrowserOptions } = await import(pathToFileURL(path.join(path.dirname(script), "learning-cycle-video-checks.mjs")));
const { chromium } = createRequire(import.meta.url)("playwright-core");

// Exige o navegador real com codecs; fixture sintética fixa dispensa FFmpeg e rede no runner.
test("captura pixels do instante correto com mídia local íntegra", { timeout: 45000 }, async () => {
  const directory = await mkdtemp(path.join(os.tmpdir(), "video-review-fixture-"));
  const options = videoBrowserOptions();
  options.args.push("--disable-dev-shm-usage", "--renderer-process-limit=2");
  let browser;
  try {
    const fixture = process.env.CUSTOMER_AGENT_VIDEO_FIXTURE
      || path.resolve("src/test/fixtures/final-video-red-blue.mp4.b64");
    const bytes = Buffer.from(await readFile(fixture, "utf8"), "base64");
    const media = { assetId: 93001, durationSeconds: 6, url: "https://fixtures.invalid/video.mp4",
      sha256: createHash("sha256").update(bytes).digest("hex"), reviewMoments: [4.5] };
    const artifacts = await captureVideos([media], directory, options,
      async () => new Response(bytes, { status: 200 }));
    browser = await chromium.launch(options);
    const page = await browser.newPage();
    const pixels = [];
    for (const artifact of [artifacts[0], artifacts.at(-1)]) {
      const encoded = (await readFile(artifact.localPath)).toString("base64");
      pixels.push(await page.evaluate(async data => {
        const image = new Image(); image.src = `data:image/png;base64,${data}`; await image.decode();
        const canvas = document.createElement("canvas"); canvas.width = image.width; canvas.height = image.height;
        const context = canvas.getContext("2d"); context.drawImage(image, 0, 0);
        return [...context.getImageData(195, 350, 1, 1).data];
      }, encoded));
    }
    assert.ok(pixels[0][0] > 200 && pixels[0][2] < 30, `A abertura precisa ser vermelha: ${pixels[0]}`);
    assert.ok(pixels[1][2] > 200 && pixels[1][0] < 30, `O encerramento precisa ser azul: ${pixels[1]}`);
  } finally {
    if (browser) await browser.close();
    await rm(directory, { recursive: true, force: true });
  }
});
