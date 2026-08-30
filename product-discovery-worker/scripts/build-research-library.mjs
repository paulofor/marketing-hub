import { createHash } from "node:crypto";
import {
  mkdirSync,
  readFileSync,
  readdirSync,
  statSync,
  writeFileSync,
} from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const workerRoot = resolve(fileURLToPath(new URL("..", import.meta.url)));
const repositoryRoot = resolve(workerRoot, "..");
const sourceRoot = resolve(repositoryRoot, "pesquisas");
const outputPath = resolve(workerRoot, "research-library/index.json");
const checkOnly = process.argv.includes("--check");

/** Materializa a coleção viva em um índice reproduzível que entra na imagem de Argos. */
function buildIndex() {
  const documents = markdownFiles(sourceRoot).map((filePath) => {
    const content = normalize(readFileSync(filePath, "utf8"));
    const repositoryPath = relative(repositoryRoot, filePath).replaceAll("\\", "/");
    const filename = repositoryPath.split("/").at(-1);
    const collection = repositoryPath.split("/")[1];
    return {
      path: repositoryPath,
      collection,
      title: firstHeading(content) || filename.replace(/\.md$/i, ""),
      date: filename.match(/^(\d{4}-\d{2}-\d{2})/)?.[1] || null,
      evidenceEligible: filename !== "ini.md",
      sha256: createHash("sha256").update(content).digest("hex"),
      content,
    };
  });
  return `${JSON.stringify(
    {
      schemaVersion: 1,
      generatedFrom: "pesquisas/**/*.md",
      documents,
    },
    null,
    2,
  )}\n`;
}

/** Percorre as coleções em ordem estável para evitar diferenças artificiais entre ambientes. */
function markdownFiles(directory) {
  return readdirSync(directory)
    .sort()
    .flatMap((name) => {
      const path = join(directory, name);
      return statSync(path).isDirectory()
        ? markdownFiles(path)
        : path.endsWith(".md")
          ? [path]
          : [];
    });
}

/** Extrai o primeiro título Markdown sem interpretar o restante do conteúdo. */
function firstHeading(content) {
  return content.match(/^#\s+(.+)$/m)?.[1]?.trim() || null;
}

/** Uniformiza finais de linha e remove espaços finais sem alterar o texto factual. */
function normalize(content) {
  return content.replaceAll("\r\n", "\n").replace(/[ \t]+$/gm, "").trim();
}

const generated = buildIndex();
if (checkOnly) {
  let current = "";
  try {
    current = readFileSync(outputPath, "utf8");
  } catch {
    // A mensagem abaixo diferencia índice ausente de conteúdo desatualizado.
  }
  if (current !== generated) {
    throw new Error(
      "Índice de pesquisas desatualizado. Execute npm run build:research-library.",
    );
  }
} else {
  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, generated, "utf8");
}
