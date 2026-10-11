import assert from "node:assert/strict";
import { createHash, randomUUID } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { createRequire } from "node:module";
import { pathToFileURL } from "node:url";
import { videoBrowserOptions } from "./learning-cycle-video-checks.mjs";
const { chromium, devices } = createRequire(import.meta.url)("playwright-core");

// Amostra abertura, progressão e encerramento, sem alegar audição ou comportamento humano real.
export function sampleTimes(duration) {
  assert.ok(Number.isFinite(duration) && duration >= 3 && duration <= 120);
  return [...new Set([0.1, 1, 2, duration * .25, duration * .5, duration * .75, duration - .2]
    .map(value => Math.round(value * 1000) / 1000))].sort((a, b) => a - b);
}

// Lê somente bytes limitados de HTTPS sem seguir redirecionamentos e confirma a identidade persistida.
export async function downloadVideo(media, fetcher = fetch) {
  const url = new URL(media.url);
  assert.equal(url.protocol, "https:");
  assert.ok(!url.username && !url.password);
  assert.match(media.sha256, /^[a-f0-9]{64}$/);
  const response = await fetcher(url, { redirect: "error", signal: AbortSignal.timeout(60000) });
  assert.equal(response.status, 200);
  const chunks = [];
  let size = 0;
  try {
    for await (const chunk of response.body) {
      size += chunk.length;
      assert.ok(size <= 64 * 1024 * 1024, "Vídeo excede o limite da revisão");
      chunks.push(chunk);
    }
  } finally { if (!response.body.locked) await response.body.cancel(); }
  const bytes = Buffer.concat(chunks);
  assert.equal(createHash("sha256").update(bytes).digest("hex"), media.sha256, "Hash do vídeo mudou");
  return bytes;
}

// Captura o mesmo arquivo no tamanho de celular, mantendo instante, identidade e bytes auditáveis.
export async function captureVideos(inputs, directory, options = videoBrowserOptions(), fetcher = fetch) {
  assert.ok(Array.isArray(inputs) && inputs.length > 0 && inputs.length <= 2);
  await mkdir(directory, { recursive: true });
  const browser = await chromium.launch(options);
  const artifacts = [];
  const captureSessionId = randomUUID();
  try {
    for (const media of inputs) {
      assert.ok(Number.isSafeInteger(media.assetId) && media.assetId > 0);
      const times = sampleTimes(media.durationSeconds);
      const bytes = await downloadVideo(media, fetcher);
      const context = await browser.newContext({ ...devices["iPhone 15 Pro"], viewport: { width: 393, height: 700 }, deviceScaleFactor: 1 });
      try {
        await context.route("**/*", route => route.request().url() === "https://review.invalid/media.mp4"
          ? route.fulfill({ status: 200, contentType: "video/mp4", body: bytes }) : route.abort());
        const page = await context.newPage();
        await page.setContent('<style>body{margin:0;background:#000}video{width:393px;height:700px;object-fit:contain}</style><video muted preload="auto" src="https://review.invalid/media.mp4"></video>');
        await page.waitForFunction(() => document.querySelector("video").readyState >= 2);
        const duration = await page.locator("video").evaluate(v => v.duration);
        assert.ok(Math.abs(duration - media.durationSeconds) <= 1, "Duração divergente do contrato");
        for (const second of times) {
          await page.locator("video").evaluate((video, time) => new Promise((resolve, reject) => {
            const timer = setTimeout(() => reject(new Error("Timeout de frame")), 10000);
            video.addEventListener("seeked", () => { clearTimeout(timer); resolve(); }, { once: true });
            video.currentTime = time;
          }), second);
          await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
          const evidenceKey = `video-${media.assetId}-${media.sha256}-${Math.round(second * 1000)}`;
          const localPath = path.resolve(directory, `${evidenceKey}.png`);
          await page.screenshot({ path: localPath });
          artifacts.push({ captureSessionId, evidenceKey, evidenceType: "VIDEO_FRAME", deviceProfile: "IPHONE_15_PRO",
            pageNumber: 1, foldNumber: null, viewportWidth: 393, viewportHeight: 700, pageHeightPx: 700,
            scrollY: 0, sourceUrl: media.url, finalUrl: media.url, capturedAt: new Date().toISOString(), localPath });
        }
      } finally { await context.close(); }
    }
  } finally { await browser.close(); }
  return artifacts;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const [input, output, directory] = process.argv.slice(2);
  const artifacts = await captureVideos(JSON.parse(await readFile(input, "utf8")), directory);
  await writeFile(output, JSON.stringify(artifacts));
}
