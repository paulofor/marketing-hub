import assert from "node:assert/strict";
import { mkdtemp, mkdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import {
  attachLiveArticleInspirations,
  loadLiveArticleInspirations,
} from "./live-inspirations.mjs";

test("consulta todos os Markdown atuais das três coleções vivas", async (context) => {
  const root = await mkdtemp(join(tmpdir(), "pde-live-inspirations-"));
  context.after(async () => await rm(root, { recursive: true, force: true }));
  await mkdir(join(root, "pesquisas/gartner"), { recursive: true });
  await mkdir(join(root, "pesquisas/ia-aplicada"), { recursive: true });
  await mkdir(join(root, "pesquisas/momentos-de-compra-b2c"), { recursive: true });
  await writeFile(join(root, "pesquisas/gartner/2026-08-25-a.md"), "Gartner A\n");
  await writeFile(join(root, "pesquisas/gartner/2026-08-26-b.md"), "Gartner B\n");
  await writeFile(join(root, "pesquisas/ia-aplicada/2026-08-26-c.md"), "IA C\n");
  await writeFile(
    join(root, "pesquisas/momentos-de-compra-b2c/2026-08-26-d.md"),
    "Momento D\n",
  );
  await writeFile(
    join(root, "pesquisas/momentos-de-compra-b2c/ini.md"),
    "Contrato da coleção\n",
  );
  await writeFile(join(root, "pesquisas/ia-aplicada/ignorar.txt"), "fora do contrato\n");

  const inventory = await loadLiveArticleInspirations(
    root,
    new Date("2026-08-26T12:00:00Z"),
  );

  assert.equal(inventory.articles.length, 4);
  assert.deepEqual(
    inventory.articles.map((article) => article.origin),
    ["GARTNER", "GARTNER", "IA_APLICADA", "MOMENTOS_COMPRA_B2C"],
  );
  assert.equal(inventory.articles[0].materialDate, "2026-08-25");
  assert.equal(inventory.articles[0].contentSha256.length, 64);
  assert.equal(inventory.articles[2].content, "IA C\n");
  assert.deepEqual(
    inventory.collections.map((collection) => [collection.code, collection.status]),
    [
      ["GARTNER", "CURRENT"],
      ["IA_APLICADA", "CURRENT"],
      ["MOMENTOS_COMPRA_B2C", "CURRENT"],
    ],
  );
});

test("registra coleção diária vazia sem transformar ini.md em evidência", async (context) => {
  const root = await mkdtemp(join(tmpdir(), "pde-live-inspirations-empty-"));
  context.after(async () => await rm(root, { recursive: true, force: true }));
  await mkdir(join(root, "pesquisas/gartner"), { recursive: true });
  await mkdir(join(root, "pesquisas/ia-aplicada"), { recursive: true });
  await mkdir(join(root, "pesquisas/momentos-de-compra-b2c"), { recursive: true });
  await writeFile(join(root, "pesquisas/gartner/2026-08-26-a.md"), "Gartner\n");
  await writeFile(join(root, "pesquisas/ia-aplicada/2026-08-26-b.md"), "IA\n");
  await writeFile(join(root, "pesquisas/momentos-de-compra-b2c/ini.md"), "Contrato\n");

  const inventory = await loadLiveArticleInspirations(root);
  const collection = inventory.collections.find(
    (item) => item.code === "MOMENTOS_COMPRA_B2C",
  );

  assert.equal(collection.status, "EMPTY");
  assert.equal(collection.articleCount, 0);
  assert.equal(
    inventory.articles.some((article) => article.path.endsWith("/ini.md")),
    false,
  );
});

test("preserva o snapshot Hotmart ao anexar artigos recém-consultados", () => {
  const research = {
    inspirations: { hotmartProducts: [{ id: "hotmart-1" }] },
  };
  const inventory = {
    consultedAt: "2026-08-26T12:00:00.000Z",
    collections: [{ code: "GARTNER", status: "CURRENT" }],
    articles: [{ id: "article:1" }],
  };

  const enriched = attachLiveArticleInspirations(research, inventory);

  assert.deepEqual(enriched.inspirations.hotmartProducts, [{ id: "hotmart-1" }]);
  assert.deepEqual(enriched.inspirations.collections, [
    { code: "GARTNER", status: "CURRENT" },
  ]);
  assert.deepEqual(enriched.inspirations.articles, [{ id: "article:1" }]);
});
