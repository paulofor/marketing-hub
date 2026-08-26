import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { mkdirSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";

const outputDir = resolve(".codex/attachments/rigel-approved-package");
mkdirSync(outputDir, { recursive: true });
const response = await fetch(
  "http://191.252.181.168/api/planning/commercial-plans/4/visual-assets",
  { signal: AbortSignal.timeout(30_000) },
);
if (!response.ok) throw new Error(`Pacote indisponível: HTTP ${response.status}`);
const assets = (await response.json()).filter(
  (asset) =>
    asset.status === "APPROVED" &&
    asset.agentReviewStatus === "APPROVED" &&
    asset.customerReviewStatus === "APPROVED",
);
if (assets.length !== 11) {
  throw new Error(`Pacote aprovado inesperado: ${assets.length} ativos`);
}

for (const asset of assets) {
  const mediaResponse = await fetch(asset.assetUrl, {
    signal: AbortSignal.timeout(60_000),
  });
  if (!mediaResponse.ok) {
    throw new Error(`${asset.label} indisponível: HTTP ${mediaResponse.status}`);
  }
  const bytes = Buffer.from(await mediaResponse.arrayBuffer());
  const digest = createHash("sha256").update(bytes).digest("hex");
  if (digest !== asset.contentSha256) {
    throw new Error(`${asset.label} divergiu do SHA-256 aprovado`);
  }
  asset.localPath = resolve(outputDir, asset.label);
  writeFileSync(asset.localPath, bytes);
}

const video = assets.find((asset) => asset.mediaType === "VIDEO");
if (!video) throw new Error("Vídeo aprovado ausente");
const probe = spawnSync(
  "ffprobe",
  [
    "-v",
    "error",
    "-show_entries",
    "format=duration:stream=codec_name,width,height,r_frame_rate",
    "-of",
    "json",
    video.localPath,
  ],
  { encoding: "utf8" },
);
if (probe.status !== 0) throw new Error(probe.stderr);
const videoProbe = JSON.parse(probe.stdout);
const contactSheet = resolve(outputDir, "rigel-video-contact-sheet.jpg");
const thumbnail = spawnSync(
  "ffmpeg",
  [
    "-y",
    "-i",
    video.localPath,
    "-vf",
    "fps=1/5,scale=270:-1,tile=3x2",
    "-frames:v",
    "1",
    contactSheet,
  ],
  { encoding: "utf8" },
);
if (thumbnail.status !== 0) throw new Error(thumbnail.stderr);

const manifest = {
  packageId: assets[0].creativePackageId,
  count: assets.length,
  images: assets.filter((asset) => asset.mediaType === "IMAGE").length,
  videos: assets.filter((asset) => asset.mediaType === "VIDEO").length,
  assets,
  videoProbe,
  videoContactSheet: contactSheet,
};
writeFileSync(
  resolve(outputDir, "verified-manifest.json"),
  `${JSON.stringify(manifest, null, 2)}\n`,
);
console.log(
  JSON.stringify({
    packageId: manifest.packageId,
    count: manifest.count,
    images: manifest.images,
    videos: manifest.videos,
    videoProbe,
  }),
);
