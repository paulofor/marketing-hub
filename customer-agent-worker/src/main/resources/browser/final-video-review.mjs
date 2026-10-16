import assert from "node:assert/strict";
import { createHash, randomUUID } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { createRequire } from "node:module";
import { pathToFileURL } from "node:url";
import { videoBrowserOptions } from "./learning-cycle-video-checks.mjs";
const { chromium, devices } = createRequire(import.meta.url)("playwright-core");

// Amostra abertura, progressão e encerramento, sem alegar audição ou comportamento humano real.
export function sampleTimes(duration, reviewMoments = []) {
  assert.ok(Number.isFinite(duration) && duration >= 3 && duration <= 120);
  assert.ok(Array.isArray(reviewMoments) && reviewMoments.length <= 24);
  assert.ok(reviewMoments.every(value => Number.isFinite(value) && value > 0 && value < duration));
  return [...new Set([0.1, 1, 2, duration * .25, duration * .5, duration * .75, duration - .2, ...reviewMoments]
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

// Extrai o mix na linha do tempo do vídeo, preservando também a pausa após o fim da faixa AAC.
export async function extractAudio(page, bytes, media, directory) {
  assert.ok(Number.isFinite(media.durationSeconds) && media.durationSeconds >= 3 && media.durationSeconds <= 120);
  const decoded = await page.evaluate(async ({encoded, videoDuration}) => {
    const data = Uint8Array.from(atob(encoded), value => value.charCodeAt(0));
    const context = new AudioContext();
    try {
      const original = await context.decodeAudioData(data.buffer);
      if (original.duration < 3 || original.duration > 121) throw new Error("Duração de áudio inválida");
      if (original.duration > videoDuration + 1) throw new Error("Áudio ultrapassa a duração do vídeo");
      // A faixa pode acabar antes da imagem. Completa somente o silêncio real do encerramento,
      // sem esticar fala, cortar palavras ou tratar a pausa como erro de sincronização.
      const timelineDuration = Math.max(videoDuration, original.duration);
      const rate = 24000;
      const offline = new OfflineAudioContext(1, Math.ceil(timelineDuration * rate), rate);
      const source = offline.createBufferSource(); source.buffer = original;
      source.connect(offline.destination); source.start();
      const rendered = await offline.startRendering();
      const samples = rendered.getChannelData(0);
      const wav = new ArrayBuffer(44 + samples.length * 2), view = new DataView(wav);
      const label = (offset, value) => [...value].forEach((c,i) => view.setUint8(offset+i,c.charCodeAt(0)));
      label(0,"RIFF");view.setUint32(4,wav.byteLength-8,true);label(8,"WAVE");label(12,"fmt ");
      view.setUint32(16,16,true);view.setUint16(20,1,true);view.setUint16(22,1,true);
      view.setUint32(24,rate,true);view.setUint32(28,rate*2,true);view.setUint16(32,2,true);view.setUint16(34,16,true);
      label(36,"data");view.setUint32(40,samples.length*2,true);
      let peak=0, energy=0;
      for(let i=0;i<samples.length;i++) { const v=Math.max(-1,Math.min(1,samples[i])); peak=Math.max(peak,Math.abs(v));energy+=v*v; view.setInt16(44+i*2,Math.round(v*(v<0?32768:32767)),true); }
      const buffer=new Uint8Array(wav);let binary="";
      for(let i=0;i<buffer.length;i+=16384) binary+=String.fromCharCode(...buffer.subarray(i,i+16384));
      return { data:btoa(binary), durationSeconds:timelineDuration,
        decodedDurationSeconds:original.duration, trailingSilenceSeconds:Math.max(0,videoDuration-original.duration),
        sampleRate:rate,channels:1,peak,rms:Math.sqrt(energy/samples.length) };
    } finally { await context.close(); }
  }, {encoded:bytes.toString("base64"),videoDuration:media.durationSeconds});
  assert.ok(Math.abs(decoded.durationSeconds-media.durationSeconds)<=1,"Duração do mix divergente do vídeo");
  assert.ok(decoded.peak>0.00001 && decoded.rms>0.000001,"Áudio ausente ou silencioso; escuta bloqueada");
  const audio=Buffer.from(decoded.data,"base64");
  const audioSha256=createHash("sha256").update(audio).digest("hex");
  const localPath=path.resolve(directory,`audio-${media.assetId}-${media.sha256}.wav`);
  await writeFile(localPath,audio,{mode:0o600});
  const {data,...metrics}=decoded;
  return {assetId:media.assetId,videoSha256:media.sha256,audioSha256,localPath,...metrics};
}

// Captura o mesmo arquivo no tamanho de celular, mantendo instante, identidade e bytes auditáveis.
export async function captureVideos(inputs, directory, options = videoBrowserOptions(), fetcher = fetch) {
  assert.ok(Array.isArray(inputs) && inputs.length > 0 && inputs.length <= 2);
  await mkdir(directory, { recursive: true });
  const browser = await chromium.launch(options);
  const artifacts = [];
  const audioManifest = [];
  const captureSessionId = randomUUID();
  try {
    for (const media of inputs) {
      assert.ok(Number.isSafeInteger(media.assetId) && media.assetId > 0);
      const times = sampleTimes(media.durationSeconds, media.reviewMoments);
      const bytes = await downloadVideo(media, fetcher);
      const context = await browser.newContext({ ...devices["iPhone 15 Pro"], viewport: { width: 393, height: 700 }, deviceScaleFactor: 1 });
      try {
        await context.route("**/*", route => route.abort());
        const page = await context.newPage();
        await page.setContent('<meta name="viewport" content="width=device-width,initial-scale=1"><style>body{margin:0;background:#000}video{width:393px;height:700px;object-fit:contain}</style><video muted preload="auto"></video>');
        // O Blob contém os bytes já verificados e permite seek sem simular HTTP Range parcialmente.
        await page.locator("video").evaluate((video, encoded) => {
          const data = Uint8Array.from(atob(encoded), value => value.charCodeAt(0));
          video.src = URL.createObjectURL(new Blob([data], { type: "video/mp4" }));
          video.load();
        }, bytes.toString("base64"));
        await page.waitForFunction(() => document.querySelector("video").readyState >= 2);
        if (media.audioReviewRequired) audioManifest.push(await extractAudio(page,bytes,media,directory));
        const duration = await page.locator("video").evaluate(v => v.duration);
        assert.ok(Math.abs(duration - media.durationSeconds) <= 1, "Duração divergente do contrato");
        for (const second of times) {
          await page.locator("video").evaluate((video, time) => new Promise((resolve, reject) => {
            const timer = setTimeout(() => reject(new Error("Timeout de frame")), 10000);
            video.addEventListener("seeked", () => { clearTimeout(timer); resolve(); }, { once: true });
            video.currentTime = time;
          }), second);
          await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
          assert.ok(Math.abs(await page.locator("video").evaluate(video => video.currentTime) - second) <= .08,
            "O frame não corresponde ao instante solicitado");
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
  await writeFile(path.resolve(directory,"audio-manifest.json"),JSON.stringify(audioManifest),{mode:0o600});
  return artifacts;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const [input, output, directory] = process.argv.slice(2);
  const artifacts = await captureVideos(JSON.parse(await readFile(input, "utf8")), directory);
  await writeFile(output, JSON.stringify(artifacts));
}
