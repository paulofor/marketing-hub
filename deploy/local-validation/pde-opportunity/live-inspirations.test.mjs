import assert from "node:assert/strict";
import { mkdtemp, mkdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import {
  attachLiveArticleInspirations,
  loadLiveArticleInspirations,
} from "./live-inspirations.mjs";

test("consulta todos os Markdown atuais das duas coleções vivas", async (context) => {
  const root = await mkdtemp(join(tmpdir(), "pde-live-inspirations-"));
  context.after(async () => await rm(root, { recursive: true, force: true }));
  await mkdir(join(root, "pesquisas/gartner"), { recursive: true });
  await mkdir(join(root, "pesquisas/ia-aplicada"), { recursive: true });
  await writeFile(join(root, "pesquisas/gartner/2026-08-25-a.md"), "Gartner A\n");
  await writeFile(join(root, "pesquisas/gartner/2026-08-26-b.md"), "Gartner B\n");
  await writeFile(join(root, "pesquisas/ia-aplicada/2026-08-26-c.md"), "IA C\n");
  await writeFile(join(root, "pesquisas/ia-aplicada/ignorar.txt"), "fora do contrato\n");

  const inventory = await loadLiveArticleInspirations(
    root,
    new Date("2026-08-26T12:00:00Z"),
  );

  assert.equal(inventory.articles.length, 3);
  assert.deepEqual(
    inventory.articles.map((article) => article.origin),
    ["GARTNER", "GARTNER", "IA_APLICADA"],
  );
  assert.equal(inventory.articles[0].materialDate, "2026-08-25");
  assert.equal(inventory.articles[0].contentSha256.length, 64);
  assert.equal(inventory.articles[2].content, "IA C\n");
});

test("preserva o snapshot Hotmart ao anexar artigos recém-consultados", () => {
  const research = {
    inspirations: { hotmartProducts: [{ id: "hotmart-1" }] },
  };
  const inventory = {
    consultedAt: "2026-08-26T12:00:00.000Z",
    articles: [{ id: "article:1" }],
  };

  const enriched = attachLiveArticleInspirations(research, inventory);

  assert.deepEqual(enriched.inspirations.hotmartProducts, [{ id: "hotmart-1" }]);
  assert.deepEqual(enriched.inspirations.articles, [{ id: "article:1" }]);
});
