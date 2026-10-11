import { test } from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import { existsSync } from "node:fs";
import { execFileSync } from "node:child_process";
import os from "node:os";
import path from "node:path";
import { sampleTimes, downloadVideo, captureVideos } from "../../main/resources/browser/final-video-review.mjs";

test("cobre os primeiros dois segundos, progressão e CTA de dois produtos", () => {
  for (const duration of [20, 35, 45]) {
    const times = sampleTimes(duration);
    assert.deepEqual(times.slice(0, 3), [.1, 1, 2]);
    assert.ok(times.includes(duration / 2));
    assert.equal(times.at(-1), duration - .2);
  }
  assert.throws(() => sampleTimes(0));
});
test("hash, origem e tamanho protegem a revisão antes de pagar o modelo", async () => {
  const bytes = Buffer.from("fixture audiovisual segregada");
  const media = { url: "https://fixtures.invalid/final.mp4", sha256: createHash("sha256").update(bytes).digest("hex") };
  const fetcher = async (_, options) => {
    assert.equal(options.redirect, "error");
    return new Response(bytes, { status: 200 });
  };
  assert.deepEqual(await downloadVideo(media, fetcher), bytes);
  await assert.rejects(downloadVideo({ ...media, sha256: "b".repeat(64) }, fetcher), /Hash/);
  await assert.rejects(downloadVideo({ ...media, url: "http://fixtures.invalid/final.mp4" }, fetcher));
});
test("não omite prova curta nem fala situada entre os quartis", () => {
  for (const duration of [30, 45]) {
    const moments = [8.2, 25.5, duration - 1];
    const times = sampleTimes(duration, moments);
    moments.forEach(second => assert.ok(times.includes(second)));
    assert.ok(times.includes(.1) && times.includes(duration - .2));
  }
  assert.throws(() => sampleTimes(30, [NaN]));
  assert.throws(() => sampleTimes(30, [31]));
  assert.throws(() => sampleTimes(30, Array(25).fill(1)));
});
const chromiumPath = process.env.CHROMIUM_BIN || process.env.CHROME_BIN || "/usr/bin/chromium";
test("captura pixels do instante correto com mídia local íntegra", {
  skip: !existsSync(chromiumPath) || !existsSync("/usr/bin/ffmpeg"), timeout: 45000,
}, async () => {
  const directory = await mkdtemp(path.join(os.tmpdir(), "video-review-fixture-"));
  try {
    const file = path.join(directory, "red-blue.mp4");
    execFileSync("ffmpeg", ["-v", "error", "-y", "-f", "lavfi", "-i", "color=c=red:s=90x160:r=5:d=3",
      "-f", "lavfi", "-i", "color=c=blue:s=90x160:r=5:d=3", "-filter_complex_threads", "1",
      "-filter_complex", "[0:v][1:v]concat=n=2:v=1:a=0", "-c:v", "libx264", "-threads", "2",
      "-pix_fmt", "yuv420p", "-movflags", "+faststart", file]);
    const bytes = await readFile(file);
    const media = { assetId: 93001, durationSeconds: 6, url: "https://fixtures.invalid/video.mp4",
      sha256: createHash("sha256").update(bytes).digest("hex"), reviewMoments: [4.5] };
    const artifacts = await captureVideos([media], directory, { executablePath: chromiumPath,
      headless: true, args: ["--no-sandbox", "--disable-dev-shm-usage", "--renderer-process-limit=2"] },
      async () => new Response(bytes, { status: 200 }));
    const pixel = artifact => execFileSync("ffmpeg", ["-v", "error", "-i", artifact.localPath,
      "-vf", "scale=1:1", "-frames:v", "1", "-f", "rawvideo", "-pix_fmt", "rgb24", "-"]);
    const first = pixel(artifacts[0]), last = pixel(artifacts.at(-1));
    assert.ok(first[0] > 200 && first[2] < 30, `A abertura precisa ser vermelha: ${[...first]}`);
    assert.ok(last[2] > 200 && last[0] < 30, `O encerramento precisa ser azul: ${[...last]}`);
  } finally { await rm(directory, { recursive: true, force: true }); }
});
