import { createHash } from "node:crypto";
import { readdir, readFile } from "node:fs/promises";
import { join, relative } from "node:path";

const ARTICLE_COLLECTIONS = [
  { code: "GARTNER", directory: "pesquisas/gartner" },
  { code: "IA_APLICADA", directory: "pesquisas/ia-aplicada" },
];

/** Consulta novamente todas as coleções vivas de inspiração antes de cada execução local. */
export async function loadLiveArticleInspirations(repositoryRoot, consultedAt = new Date()) {
  const articles = [];
  const consultedAtIso = consultedAt.toISOString();
  for (const collection of ARTICLE_COLLECTIONS) {
    const directory = join(repositoryRoot, collection.directory);
    const entries = (await readdir(directory, { withFileTypes: true }))
      .filter((entry) => entry.isFile() && entry.name.endsWith(".md"))
      .sort((left, right) => left.name.localeCompare(right.name));
    for (const entry of entries) {
      const absolutePath = join(directory, entry.name);
      const content = await readFile(absolutePath, "utf8");
      const repositoryPath = relative(repositoryRoot, absolutePath).replaceAll("\\", "/");
      articles.push({
        id: `article:${repositoryPath}`,
        origin: collection.code,
        path: repositoryPath,
        materialDate: extractMaterialDate(entry.name),
        consultedAt: consultedAtIso,
        contentSha256: createHash("sha256").update(content).digest("hex"),
        content,
      });
    }
  }
  return { consultedAt: consultedAtIso, articles };
}

/** Anexa o inventário atual aos demais fatos congelados do ciclo. */
export function attachLiveArticleInspirations(research, inventory) {
  return {
    ...research,
    inspirations: {
      ...(research.inspirations || {}),
      consultedAt: inventory.consultedAt,
      articles: inventory.articles,
    },
  };
}

/** Extrai a data do próprio nome versionado sem inventar data editorial ausente. */
function extractMaterialDate(fileName) {
  return fileName.match(/\d{4}-\d{2}-\d{2}/)?.[0] || null;
}
