import { createHash } from "node:crypto";
import { readdir, readFile } from "node:fs/promises";
import { join, relative } from "node:path";

const ARTICLE_COLLECTIONS = [
  { code: "GARTNER", directory: "pesquisas/gartner" },
  { code: "IA_APLICADA", directory: "pesquisas/ia-aplicada" },
  {
    code: "MOMENTOS_COMPRA_B2C",
    directory: "pesquisas/momentos-de-compra-b2c",
    ignoredFiles: new Set(["ini.md"]),
  },
];

/** Consulta novamente todas as coleções vivas de inspiração antes de cada execução local. */
export async function loadLiveArticleInspirations(repositoryRoot, consultedAt = new Date()) {
  const articles = [];
  const collections = [];
  const consultedAtIso = consultedAt.toISOString();
  for (const collection of ARTICLE_COLLECTIONS) {
    const directory = join(repositoryRoot, collection.directory);
    let directoryEntries;
    try {
      directoryEntries = await readdir(directory, { withFileTypes: true });
    } catch (error) {
      if (error?.code !== "ENOENT") throw error;
      collections.push({
        code: collection.code,
        path: collection.directory,
        status: "UNAVAILABLE",
        articleCount: 0,
        consultedAt: consultedAtIso,
      });
      continue;
    }
    const entries = directoryEntries
      .filter(
        (entry) =>
          entry.isFile() &&
          entry.name.endsWith(".md") &&
          !collection.ignoredFiles?.has(entry.name),
      )
      .sort((left, right) => left.name.localeCompare(right.name));
    let articleCount = 0;
    for (const entry of entries) {
      const absolutePath = join(directory, entry.name);
      const content = await readFile(absolutePath, "utf8");
      if (!content.trim()) continue;
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
      articleCount += 1;
    }
    collections.push({
      code: collection.code,
      path: collection.directory,
      status: articleCount > 0 ? "CURRENT" : "EMPTY",
      articleCount,
      consultedAt: consultedAtIso,
    });
  }
  return { consultedAt: consultedAtIso, collections, articles };
}

/** Anexa o inventário atual aos demais fatos congelados do ciclo. */
export function attachLiveArticleInspirations(research, inventory) {
  return {
    ...research,
    inspirations: {
      ...(research.inspirations || {}),
      consultedAt: inventory.consultedAt,
      collections: inventory.collections,
      articles: inventory.articles,
    },
  };
}

/** Extrai a data do próprio nome versionado sem inventar data editorial ausente. */
function extractMaterialDate(fileName) {
  return fileName.match(/\d{4}-\d{2}-\d{2}/)?.[0] || null;
}
