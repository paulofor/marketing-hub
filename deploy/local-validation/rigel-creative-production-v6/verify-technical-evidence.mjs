import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { readFile, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";

const localDirectory = resolve(process.argv[2] ?? ".");
const evidenceDirectory = join(localDirectory, "evidence");
const artifactDirectory = join(evidenceDirectory, "artifacts");
const proofDirectory = join(evidenceDirectory, "proof");
const reportFile = join(evidenceDirectory, "technical-verification.json");
const readJson = async (file) => JSON.parse(await readFile(file, "utf8"));
const sha256 = async (file) =>
  createHash("sha256")
    .update(await readFile(file))
    .digest("hex");
const assert = (condition, message) => {
  if (!condition) throw new Error(message);
};

const contract = await readJson(
  join(localDirectory, "rigel-creative-contract.v1.json"),
);
const storyboard = await readJson(
  join(evidenceDirectory, "apollo-storyboard.json"),
);
const manifest = await readJson(
  join(artifactDirectory, "rigel-creative-manifest.json"),
);
const verifiedFiles = [];

async function verifyFile(category, file, expectedSha256, baseDirectory) {
  const actualSha256 = await sha256(join(baseDirectory, file));
  if (expectedSha256) {
    assert(
      actualSha256 === expectedSha256,
      `Hash divergente em ${category}/${file}`,
    );
  }
  verifiedFiles.push({ category, file, sha256: actualSha256 });
}

for (const proof of manifest.sourceProofs) {
  await verifyFile("SOURCE_PROOF", proof.file, proof.sha256, proofDirectory);
}
for (const screenshot of manifest.destinationEvidence.screenshots) {
  await verifyFile(
    "DESTINATION_SCREENSHOT",
    screenshot.file,
    screenshot.sha256,
    proofDirectory,
  );
}
await verifyFile(
  "SUPPLEMENTAL_PROOF",
  "rigel-tasting-proof.png",
  null,
  proofDirectory,
);
for (const asset of manifest.assets) {
  await verifyFile("FINAL_ASSET", asset.file, asset.sha256, artifactDirectory);
}
for (const frame of manifest.reviewFrames) {
  await verifyFile(
    "REVIEW_FRAME",
    frame.file,
    frame.sha256,
    join(artifactDirectory, "review-frames"),
  );
}
for (const preview of manifest.channelPreviews) {
  await verifyFile(
    "CHANNEL_PREVIEW",
    preview.file,
    preview.sha256,
    join(artifactDirectory, "channel-previews"),
  );
}

const videoAsset = manifest.assets.find(
  (asset) => asset.mediaType === "VIDEO",
);
assert(videoAsset, "Manifesto sem vídeo final para verificação técnica");
const videoFile = join(artifactDirectory, videoAsset.file);
const probe = JSON.parse(
  execFileSync(
    "ffprobe",
    [
      "-v",
      "error",
      "-select_streams",
      "v:0",
      "-show_entries",
      "stream=codec_name,width,height,pix_fmt:format=duration,size,format_name",
      "-of",
      "json",
      videoFile,
    ],
    { encoding: "utf8" },
  ),
);
const video = {
  file: videoAsset.file,
  width: probe.streams[0].width,
  height: probe.streams[0].height,
  codec: probe.streams[0].codec_name,
  pixelFormat: probe.streams[0].pix_fmt,
  durationSeconds: Number(probe.format.duration),
  bytes: Number(probe.format.size),
  formatName: probe.format.format_name,
  cutDurationsSeconds: storyboard.cuts.map((cut) => cut.durationSeconds),
};
assert(video.width === 1080 && video.height === 1920, "Resolução do MP4 divergente");
assert(video.codec === "h264", "Codec do MP4 divergente");
assert(video.pixelFormat === "yuv420p", "Pixel format do MP4 divergente");
assert(
  Math.abs(video.durationSeconds - 30) < 0.15,
  "Duração do MP4 divergente",
);
assert(
  JSON.stringify(video.cutDurationsSeconds) === JSON.stringify([4, 3, 12, 5, 6]),
  "Durações dos cortes divergem do contrato",
);
assert(
  contract.formats.find((format) => format.id === "vertical-demo")
    ?.durationSeconds === 30,
  "Contrato não congela a duração de 30 segundos",
);

const counts = Object.fromEntries(
  [
    "SOURCE_PROOF",
    "DESTINATION_SCREENSHOT",
    "SUPPLEMENTAL_PROOF",
    "FINAL_ASSET",
    "REVIEW_FRAME",
    "CHANNEL_PREVIEW",
  ].map((category) => [
    category,
    verifiedFiles.filter((item) => item.category === category).length,
  ]),
);
assert(counts.SOURCE_PROOF === 4, "Quantidade de provas-fonte divergente");
assert(counts.DESTINATION_SCREENSHOT === 2, "Capturas do destino incompletas");
assert(counts.SUPPLEMENTAL_PROOF === 1, "Prova consolidada ausente");
assert(counts.FINAL_ASSET === 7, "Ativos finais incompletos");
assert(counts.REVIEW_FRAME === 5, "Frames de revisão incompletos");
assert(counts.CHANNEL_PREVIEW === 11, "Prévias de canal incompletas");

const report = {
  status: "APPROVED",
  verifier: "RIGEL_DETERMINISTIC_TECHNICAL_VERIFIER_V1",
  contractVersion: contract.contractVersion,
  manifestSha256: await sha256(
    join(artifactDirectory, "rigel-creative-manifest.json"),
  ),
  allHashesMatch: true,
  verifiedFileCount: verifiedFiles.length,
  counts,
  video,
  files: verifiedFiles,
};
await writeFile(reportFile, `${JSON.stringify(report, null, 2)}\n`);
process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
