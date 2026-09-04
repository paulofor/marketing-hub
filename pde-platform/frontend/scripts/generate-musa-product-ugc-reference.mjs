import { createHash } from "node:crypto";
import { spawnSync } from "node:child_process";
import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rootDir = path.resolve(__dirname, "..");
const sourcePath = path.join(
  rootDir,
  "src",
  "video-reference-assets",
  "musa-product-ugc-reference.svg",
);
const outputPath = path.join(
  rootDir,
  "public",
  "assets",
  "musa-product-ugc-reference.png",
);
const manifestPath = path.join(
  rootDir,
  "public",
  "assets",
  "musa-product-ugc-reference-manifest.json",
);

const sourceMarkup = await fs.readFile(sourcePath, "utf8");
const textElements = sourceMarkup.match(/<text\b[^>]*>/g) ?? [];
if (
  textElements.length === 0 ||
  textElements.some(
    (element) => !element.includes('font-family="DejaVu Sans, sans-serif"'),
  )
) {
  throw new Error(
    "A referência Product UGC deve declarar DejaVu Sans em todos os textos.",
  );
}

const fontMatch = spawnSync(
  process.env.FC_MATCH_BIN || "fc-match",
  ["--format", "%{family}", "DejaVu Sans"],
  { encoding: "utf8" },
);
if (
  fontMatch.status !== 0 ||
  !fontMatch.stdout.split(",").some((family) => family.trim() === "DejaVu Sans")
) {
  throw new Error(
    `A fonte DejaVu Sans não está disponível para renderizar a referência Product UGC: ${fontMatch.stderr || "fc-match indisponível"}`,
  );
}

const rsvgRender = spawnSync(
  process.env.RSVG_CONVERT_BIN || "rsvg-convert",
  [
    "--width",
    "1080",
    "--height",
    "1920",
    "--format",
    "png",
    "--output",
    outputPath,
    sourcePath,
  ],
  { encoding: "utf8" },
);

const render =
  rsvgRender.error?.code === "ENOENT"
    ? spawnSync(
        process.env.FFMPEG_BIN || "ffmpeg",
        [
          "-hide_banner",
          "-loglevel",
          "error",
          "-y",
          "-i",
          sourcePath,
          "-frames:v",
          "1",
          "-pix_fmt",
          "rgb24",
          outputPath,
        ],
        { encoding: "utf8" },
      )
    : rsvgRender;

const renderer =
  rsvgRender.error?.code === "ENOENT" ? "ffmpeg" : "rsvg-convert";

if (render.status !== 0) {
  throw new Error(
    `Falha ao gerar a referência Product UGC do MUSA: ${render.stderr || `${renderer} indisponível`}`,
  );
}

const content = await fs.readFile(outputPath);
const manifest = {
  contractVersion: "MUSA_PRODUCT_UGC_REFERENCE_V1",
  source: "src/video-reference-assets/musa-product-ugc-reference.svg",
  generatedBy: "scripts/generate-musa-product-ugc-reference.mjs",
  renderer,
  fontFamily: "DejaVu Sans",
  output: "public/assets/musa-product-ugc-reference.png",
  width: 1080,
  height: 1920,
  purpose:
    "Referência limpa da experiência digital MUSA para a receita Product UGC da Runway",
  sha256: createHash("sha256").update(content).digest("hex"),
};

await fs.writeFile(
  manifestPath,
  `${JSON.stringify(manifest, null, 2)}\n`,
  "utf8",
);
