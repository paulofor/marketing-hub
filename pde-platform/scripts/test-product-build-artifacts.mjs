import assert from "node:assert/strict";
import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const repositoryRoot = fileURLToPath(new URL("../../", import.meta.url));

/** Lista recursivamente somente os arquivos regulares do diretório compilado. */
async function filesInside(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const nested = await Promise.all(
    entries.map(async (entry) => {
      const entryPath = path.join(directory, entry.name);
      return entry.isDirectory() ? filesInside(entryPath) : [entryPath];
    }),
  );
  return nested.flat();
}

/** Concatena os artefatos textuais para detectar vazamento de identidade entre produtos. */
async function compiledSurface(relativeDirectory) {
  const directory = path.join(repositoryRoot, relativeDirectory);
  const files = await filesInside(directory);
  const textFiles = files.filter((file) =>
    /\.(?:css|html|js|json|svg|txt)$/i.test(file),
  );
  const contents = await Promise.all(
    textFiles.map((file) => readFile(file, "utf8")),
  );
  return {
    names: files.map((file) => path.relative(directory, file)).join("\n"),
    text: contents.join("\n"),
  };
}

test("o bundle de Vega não contém a superfície de Mira", async () => {
  const vega = await compiledSurface("pde-platform/frontend/dist");
  assert.doesNotMatch(
    `${vega.names}\n${vega.text}`,
    /mira-private|pde-planejado-36|Sua rotina, organizada com calma/i,
    "[ARQUITETURA] A imagem compilada de Vega contém artefato de Mira.",
  );
});

test("o bundle de Mira não contém a superfície de Vega", async () => {
  const mira = await compiledSurface("pde-platform/frontend/dist-mira");
  assert.match(mira.names, /mira\.html/);
  assert.match(mira.text, /Sua rotina, organizada com calma/);
  assert.doesNotMatch(
    `${mira.names}\n${mira.text}`,
    /Clube MUSA|metodo-musa-7-dias|musa-pde-entry|logo-musa|musa-product/i,
    "[ARQUITETURA] A imagem compilada de Mira contém artefato de Vega.",
  );
});

test("o bundle comercial de Mira não contém a pesquisa privada nem Vega", async () => {
  const commercialHtml = await readFile(
    path.join(
      repositoryRoot,
      "pde-platform/frontend/dist-mira-commercial/mira-commercial.html",
    ),
    "utf8",
  );
  const commercial = await compiledSurface(
    "pde-platform/frontend/dist-mira-commercial",
  );
  assert.match(commercial.names, /mira-commercial\.html/);
  assert.match(commercial.text, /Cuide de você com mais clareza/);
  assert.match(commercialHtml, /(?:src|href)="\/assets\//);
  assert.doesNotMatch(
    commercialHtml,
    /(?:src|href)="\/assets\/assets\//,
    "[ARQUITETURA] O HTML comercial de Mira duplicou o prefixo público /assets/.",
  );
  assert.doesNotMatch(
    `${commercial.names}\n${commercial.text}`,
    /mira-private|acesso privado|Clube MUSA|metodo-musa-7-dias|musa-pde-entry/i,
    "[ARQUITETURA] A imagem comercial de Mira contém artefato privado ou de Vega.",
  );
});

test("o bundle de Alcyone contém somente sua superfície e três fixtures geradas", async () => {
  const alcyone = await compiledSurface("pde-platform/frontend/dist-alcyone");
  assert.match(alcyone.names, /alcyone\.html/);
  assert.match(alcyone.names, /assets\/alcyone\/manifest\.json/);
  assert.match(alcyone.names, /assets\/alcyone\/look-1\.png/);
  assert.match(alcyone.text, /Três caminhos claros para a sua ocasião/);
  assert.match(alcyone.text, /PDE_STATIC_RESULT_FIXTURES_V1/);
  assert.doesNotMatch(
    `${alcyone.names}\n${alcyone.text}`,
    /mira-private|Clube MUSA|metodo-musa-7-dias|musa-pde-entry|logo-musa/i,
    "[ARQUITETURA] A imagem de Alcyone contém artefato de outro produto.",
  );
});
